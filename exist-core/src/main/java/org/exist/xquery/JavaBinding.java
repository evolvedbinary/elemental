/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.xquery;

import com.evolvedbinary.j8fu.function.Function4E;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.dom.QName;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;

/**
 * XQuery function call binding to Java methods and fields.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class JavaBinding extends BasicFunction {

    public static JavaBinding createFunction(final int lineNumber, final int columnNumber, final XQueryContext context, final QName qname, List<Expression> xdmParameters) throws XPathException {
        final List<JavaReflectiveCall> candidateJavaReflectiveCalls = createReflectiveCall(lineNumber, columnNumber, qname, xdmParameters);
        final FunctionSignature javaBindingFunctionSignature = functionSignature(qname, candidateJavaReflectiveCalls);
        final JavaBinding javaBinding = new JavaBinding(context, javaBindingFunctionSignature, candidateJavaReflectiveCalls);

        // set the arguments
        final JavaReflectiveCall headCandidateJavaReflectiveCall = candidateJavaReflectiveCalls.get(0); // TODO(AR) is it okay to just check the head, or should we check each item in `candidateJavaReflectiveCalls`?
        xdmParameters = mergeVarArgsXdmParameters(context, headCandidateJavaReflectiveCall, xdmParameters);
        javaBinding.setArguments(xdmParameters);

        // set the location
        javaBinding.setLocation(lineNumber, columnNumber);

        return javaBinding;
    }

    private static FunctionSignature functionSignature(final QName qname, final List<JavaReflectiveCall> candidateJavaReflectiveCalls) {
        // NOTE(AR) for the function signature we need to find the common super type and cardinality of each parameter and the return type
        final JavaReflectiveCall headCandidateJavaReflectiveCall = candidateJavaReflectiveCalls.get(0);
        final int javaParametersEquivalentXdmTypesLen;
        SequenceType javaReturnTypeEquivalentXdmType = null;
        if (headCandidateJavaReflectiveCall.javaParametersEquivalentXdmTypes != null) {
            javaParametersEquivalentXdmTypesLen = headCandidateJavaReflectiveCall.javaParametersEquivalentXdmTypes.length;
        } else {
            javaParametersEquivalentXdmTypesLen = 0;
        }
        final SequenceType[] javaParametersEquivalentXdmTypes = javaParametersEquivalentXdmTypesLen > 0 ? new SequenceType[javaParametersEquivalentXdmTypesLen] : null;
        for (int i = 0; i < candidateJavaReflectiveCalls.size(); i++) {
            final JavaReflectiveCall candidateJavaReflectiveCall = candidateJavaReflectiveCalls.get(i);
            if (i == 0) {
                if (javaParametersEquivalentXdmTypes != null && candidateJavaReflectiveCall.javaParametersEquivalentXdmTypes != null) {
                    System.arraycopy(candidateJavaReflectiveCall.javaParametersEquivalentXdmTypes, 0, javaParametersEquivalentXdmTypes, 0, javaParametersEquivalentXdmTypes.length);
                }
                javaReturnTypeEquivalentXdmType = candidateJavaReflectiveCall.javaReturnTypeEquivalentXdmType;
            } else {
                if (javaParametersEquivalentXdmTypes != null && candidateJavaReflectiveCall.javaParametersEquivalentXdmTypes != null) {
                    for (int j = 0; j < javaParametersEquivalentXdmTypes.length; j++) {
                        final int commonParamSuperType = Type.getCommonSuperType(candidateJavaReflectiveCall.javaParametersEquivalentXdmTypes[j].getPrimaryType(), javaParametersEquivalentXdmTypes[j].getPrimaryType());
                        final Cardinality commonParamSuperCardinality = Cardinality.superCardinalityOf(candidateJavaReflectiveCall.javaParametersEquivalentXdmTypes[j].getCardinality(), javaParametersEquivalentXdmTypes[j].getCardinality());
                        javaParametersEquivalentXdmTypes[j] = new SequenceType(commonParamSuperType, commonParamSuperCardinality);
                    }
                }
                final int commonReturnSuperType = Type.getCommonSuperType(candidateJavaReflectiveCall.javaReturnTypeEquivalentXdmType.getPrimaryType(), javaReturnTypeEquivalentXdmType.getPrimaryType());
                final Cardinality commonReturnSuperCardinality = Cardinality.superCardinalityOf(candidateJavaReflectiveCall.javaReturnTypeEquivalentXdmType.getCardinality(), javaReturnTypeEquivalentXdmType.getCardinality());
                javaReturnTypeEquivalentXdmType = new SequenceType(commonReturnSuperType, commonReturnSuperCardinality);
            }
        }

        return new FunctionSignature(qname, javaParametersEquivalentXdmTypes, javaReturnTypeEquivalentXdmType);
    }

    private static List<Expression> mergeVarArgsXdmParameters(final XQueryContext context, final JavaReflectiveCall javaReflectiveCall, final List<Expression> xdmParameters) {
        List<Expression> result = xdmParameters;
        final int javaParametersLen = javaReflectiveCall.javaParameters != null ? javaReflectiveCall.javaParameters.length : -1;

        if (javaParametersLen > 0 && javaParametersLen < xdmParameters.toArray().length) {
            final int lastJavaParameterIdx = javaReflectiveCall.javaParameters.length - 1;
            final Parameter lastJavaParameter = javaReflectiveCall.javaParameters[lastJavaParameterIdx];
            if (lastJavaParameter.getDeclaringExecutable().isVarArgs() && lastJavaParameter.getType().isArray()) {

                final SequenceConstructor sequenceConstructor = new SequenceConstructor(context);
                for (int i = lastJavaParameterIdx; i < xdmParameters.size(); i++) {
                    sequenceConstructor.add(xdmParameters.get(i));
                }

                result = xdmParameters.subList(0, lastJavaParameterIdx);
                result.add(sequenceConstructor);
            }
        }

        return result;
    }

    public static boolean isReflectiveCallAvailable(final QName qname, final int arity) {
        final Tuple2<String, JavaReflectiveCallParameters> javaClassNameAndBindingParams = getJavaClassName(qname.getNamespaceURI());

        final Class<?> clazz;
        try {
            clazz = Class.forName(javaClassNameAndBindingParams._1);
        } catch (final ClassNotFoundException e) {
            return false;
        }

        if ("new".equals(qname.getLocalPart())) {
            // 1. try and find a constructor that matches the arity
            for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                final Parameter[] javaParameters = constructor.getParameters();
                final int javaParametersLen = javaParameters.length;
                final boolean lastParameterIsVarArgs = (javaParametersLen > 0 && javaParameters[javaParametersLen - 1].getDeclaringExecutable().isVarArgs() && javaParameters[javaParametersLen - 1].getType().isArray());
                if (javaParametersLen == arity) {
                    return true;
                }
                if (lastParameterIsVarArgs && arity > javaParametersLen) {
                    return true;
                }
            }

        } else {
            final String javaSubjectName = xqueryKebabCaseToJavaCamelCase(qname.getLocalPart());

            // 2. try and find a field that matches the arity
            for (final Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(javaSubjectName)) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        // static field
                        if (arity == 0) {
                            return true;
                        }
                    } else {
                        // instance field
                        if (arity == 1) {
                            return true;
                        }
                    }
                }
            }

            // 3. try and find a method that matches the arity
            for (final Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(javaSubjectName)) {
                    final Parameter[] javaParameters = method.getParameters();
                    final int javaParametersLen = javaParameters.length;
                    final boolean lastParameterIsVarArgs = (javaParametersLen > 0 && javaParameters[javaParametersLen - 1].getDeclaringExecutable().isVarArgs() && javaParameters[javaParametersLen - 1].getType().isArray());

                    if (Modifier.isStatic(method.getModifiers())) {
                        // static method
                        if (javaParametersLen == arity) {
                            return true;
                        }
                        if (lastParameterIsVarArgs && arity > javaParametersLen) {
                            return true;
                        }

                    } else {
                        // instance method
                        if (javaParametersLen + 1 == arity) {
                            return true;
                        }
                        if (lastParameterIsVarArgs && arity > javaParametersLen + 1) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static List<JavaReflectiveCall> createReflectiveCall(final int lineNumber, final int columnNumber, final QName qname, final List<Expression> xdmParameters) throws XPathException {
        final Tuple2<String, JavaReflectiveCallParameters> javaClassNameAndBindingParams = getJavaClassName(qname.getNamespaceURI());

        final Class<?> clazz;
        try {
            clazz = Class.forName(javaClassNameAndBindingParams._1);
        } catch (final ClassNotFoundException e) {
            throw new XPathException(lineNumber, columnNumber, ErrorCodes.EXXQST0002, e.getMessage());
        }

        final String javaSubjectName;
        if ("new".equals(qname.getLocalPart())) {
            javaSubjectName = qname.getLocalPart();

            // 1. try and find a constructor that matches the arity
            @Nullable List<JavaReflectiveCall> candidateJavaReflectiveCalls = null;
            for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                final Parameter[] javaParameters = constructor.getParameters();
                @Nullable final Tuple2<SequenceType[], XPathUtil.TypeConversionPrecision> javaParametersEquivalentXdmTypesAndPrecision = javaParametersCompatibleWithXdmParameters(javaParameters, xdmParameters);
                if (javaParametersEquivalentXdmTypesAndPrecision != null) {
                    final SequenceType[] javaParametersEquivalentXdmTypes = javaParametersEquivalentXdmTypesAndPrecision._1;
                    final XPathUtil.TypeConversionPrecision javaParametersEquivalentXdmTypesPrecision = javaParametersEquivalentXdmTypesAndPrecision._2;
                    final JavaReflectiveCallInvoker javaReflectiveCallInvoker = constructor::newInstance;
                    final JavaReflectiveCall candidateJavaReflectiveCall = new JavaReflectiveCall(javaReflectiveCallInvoker, false, javaParameters, javaParametersEquivalentXdmTypes, javaParametersEquivalentXdmTypesPrecision, clazz, relaxedToXQueryType(clazz), javaClassNameAndBindingParams._2);
                    if (candidateJavaReflectiveCalls == null) {
                        candidateJavaReflectiveCalls = new ArrayList<>();
                    }
                    candidateJavaReflectiveCalls.add(candidateJavaReflectiveCall);
                }
            }

            if (candidateJavaReflectiveCalls != null) {
                return filterMostPrecise(candidateJavaReflectiveCalls);
            }

        } else {
            javaSubjectName = xqueryKebabCaseToJavaCamelCase(qname.getLocalPart());

            // 2. try and find a field that matches the arity
            for (final Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(javaSubjectName)) {
                    if (javaFieldAcceptsParameters(field, xdmParameters)) {
                        final JavaReflectiveCallInvoker javaReflectiveCallInvoker;
                        final boolean requiresInstanceArg;
                        final SequenceType[] javaParametersEquivalentXdmTypes;
                        if (Modifier.isStatic(field.getModifiers())) {
                            // static field
                            javaReflectiveCallInvoker = javaArgs -> field.get(null);
                            requiresInstanceArg = false;
                            javaParametersEquivalentXdmTypes = null;
                        } else {
                            // instance field
                            javaReflectiveCallInvoker = javaArgs -> field.get(javaArgs[0]);
                            requiresInstanceArg = true;
                            javaParametersEquivalentXdmTypes = new SequenceType[] { relaxedToXQueryType(field.getDeclaringClass()) };
                        }
                        return Collections.singletonList(new JavaReflectiveCall(javaReflectiveCallInvoker, requiresInstanceArg, null, javaParametersEquivalentXdmTypes, XPathUtil.TypeConversionPrecision.ONE_TO_ONE_MAPPING, field.getType(), relaxedToXQueryType(field.getType()), javaClassNameAndBindingParams._2));
                    }
                }
            }

            // 3. try and find a method that matches the arity
            @Nullable List<JavaReflectiveCall> candidateJavaReflectiveCalls = null;
            for (final Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(javaSubjectName)) {
                    final boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
                    final Parameter[] javaParameters = method.getParameters();
                    // NOTE(AR) we `sublist` below for non-static methods as the first XDM param we have is a reference to the java object instance itself
                    @Nullable Tuple2<SequenceType[], XPathUtil.TypeConversionPrecision> javaParametersEquivalentXdmTypesAndPrecision = javaParametersCompatibleWithXdmParameters(javaParameters, isStaticMethod ? xdmParameters : xdmParameters.subList(1, xdmParameters.size()));
                    if (javaParametersEquivalentXdmTypesAndPrecision != null) {
                        SequenceType[] javaParametersEquivalentXdmTypes = javaParametersEquivalentXdmTypesAndPrecision._1;
                        final XPathUtil.TypeConversionPrecision javaParametersEquivalentXdmTypesPrecision = javaParametersEquivalentXdmTypesAndPrecision._2;
                        final JavaReflectiveCallInvoker javaReflectiveCallInvoker;
                        final boolean requiresInstanceArg;
                        final Class<?> javaReturnType = method.getReturnType();
                        final SequenceType javaReturnTypeEquivalentXdmType;

                        if (Modifier.isStatic(method.getModifiers())) {
                            // static method
                            javaReflectiveCallInvoker = javaArgs -> method.invoke(null, javaArgs);
                            requiresInstanceArg = false;
                            javaReturnTypeEquivalentXdmType = relaxedToXQueryType(method.getReturnType());

                        } else {
                            // instance method
                            final boolean voidReturnThis = javaClassNameAndBindingParams._2.voidReturnsThis && (void.class == javaReturnType || Void.class == javaReturnType);
                            javaReflectiveCallInvoker = javaArgs -> {
                                final Object javaResult = method.invoke(javaArgs[0], Arrays.copyOfRange(javaArgs, 1, javaArgs.length));
                                if (voidReturnThis) {
                                    // method returns void and `?void=this` is set, so return this
                                    return javaArgs[0];
                                } else {
                                    return javaResult;
                                }
                            };
                            requiresInstanceArg = true;
                            final SequenceType[] withInstanceJavaParametersEquivalentXdmTypes = new SequenceType[javaParametersEquivalentXdmTypes.length + 1];
                            withInstanceJavaParametersEquivalentXdmTypes[0] = relaxedToXQueryType(method.getDeclaringClass());
                            System.arraycopy(javaParametersEquivalentXdmTypes, 0, withInstanceJavaParametersEquivalentXdmTypes, 1, javaParametersEquivalentXdmTypes.length);
                            javaParametersEquivalentXdmTypes = withInstanceJavaParametersEquivalentXdmTypes;
                            javaReturnTypeEquivalentXdmType = voidReturnThis ? relaxedToXQueryType(method.getDeclaringClass()) : relaxedToXQueryType(method.getReturnType());
                        }

                        final JavaReflectiveCall candidateJavaReflectiveCall = new JavaReflectiveCall(javaReflectiveCallInvoker, requiresInstanceArg, javaParameters, javaParametersEquivalentXdmTypes, javaParametersEquivalentXdmTypesPrecision, javaReturnType, javaReturnTypeEquivalentXdmType, javaClassNameAndBindingParams._2);
                        if (candidateJavaReflectiveCalls == null) {
                            candidateJavaReflectiveCalls = new ArrayList<>();
                        }
                        candidateJavaReflectiveCalls.add(candidateJavaReflectiveCall);
                    }
                }
            }

            if (candidateJavaReflectiveCalls != null) {
                return filterMostPrecise(candidateJavaReflectiveCalls);
            }
        }

        throw new XPathException(lineNumber, columnNumber, ErrorCodes.EXXQST0003, "Could not find a Java field/method named: " + javaSubjectName);
    }

    private static List<JavaReflectiveCall> sort(final List<JavaReflectiveCall> javaReflectiveCalls) {
        javaReflectiveCalls.sort(JavaBinding::compareJavaReflectiveCall);
        return javaReflectiveCalls;
    }

    private static int compareJavaReflectiveCall(final JavaReflectiveCall jrc1, final JavaReflectiveCall jrc2) {
        final XPathUtil.TypeConversionPrecision jrc1Precision = jrc1.javaParametersEquivalentXdmTypesPrecision;
        final XPathUtil.TypeConversionPrecision jrc2Precision = jrc2.javaParametersEquivalentXdmTypesPrecision;

        final int precisionDiff = jrc1Precision.compare(jrc2Precision);
        if (precisionDiff != 0) {
            return precisionDiff;
        }

        @Nullable final SequenceType[] jrc1ParamTypes = jrc1.javaParametersEquivalentXdmTypes;
        final int jrc1ParamTypesLen = jrc1ParamTypes != null ? jrc1ParamTypes.length : 0;
        @Nullable final SequenceType[] jrc2ParamTypes = jrc2.javaParametersEquivalentXdmTypes;
        final int jrc2ParamTypesLen = jrc2ParamTypes != null ? jrc2ParamTypes.length : 0;
        final int paramTypesLengthDiff = jrc1ParamTypesLen - jrc2ParamTypesLen;
        if (paramTypesLengthDiff != 0) {
            return paramTypesLengthDiff;
        }

        for (int i = 0; i < jrc1ParamTypesLen; i++) {
            final SequenceType jrc1ParamType = jrc1ParamTypes[i];
            final SequenceType jrc2ParamType = jrc2ParamTypes[i];

            final int paramTypeDiff = jrc1ParamType.getPrimaryType() - jrc2ParamType.getPrimaryType();
            if (paramTypeDiff != 0) {
                return paramTypeDiff;
            }

            final int paramCardinalityDiff = jrc1ParamType.getCardinality().compare(jrc2ParamType.getCardinality());
            if (paramCardinalityDiff != 0) {
                return paramCardinalityDiff;
            }
        }

        return 0;
    }

    private static List<JavaReflectiveCall> filterMostPrecise(final List<JavaReflectiveCall> javaReflectiveCalls) {
        javaReflectiveCalls.sort(JavaBinding::compareJavaReflectiveCall);
        final XPathUtil.TypeConversionPrecision greatestPrecision = javaReflectiveCalls.get(0).javaParametersEquivalentXdmTypesPrecision;
        int i = 1;
        for (; i < javaReflectiveCalls.size(); i++) {
            if (greatestPrecision != javaReflectiveCalls.get(i).javaParametersEquivalentXdmTypesPrecision) {
                break;
            }
        }
        return javaReflectiveCalls.subList(0, i);
    }

    private static boolean javaFieldAcceptsParameters(final Field field, final List<Expression> params) {
        if (Modifier.isStatic(field.getModifiers())) {
            return params.isEmpty();
        } else {
            return params.size() == 1;
        }
    }

    /**
     * Checks if the required Java parameters are compatible with the provided XDM Parameters.
     *
     * @param javaParameters the required Java parameters.
     * @param xdmParameters the expected XDM parameters.
     *
     * @return if they are compatible, returns an array of equivalent XDM types, else null.
     */
    private static @Nullable Tuple2<SequenceType[], XPathUtil.TypeConversionPrecision> javaParametersCompatibleWithXdmParameters(final Parameter[] javaParameters, final List<Expression> xdmParameters) {
        // check that there are at least as many XDM parameters as Java parameters
        if ((javaParameters.length == 0 && xdmParameters.isEmpty()) || (javaParameters.length > 0 && javaParameters.length <= xdmParameters.size())) {

            final SequenceType[] javaParametersEquivalentXdmTypes = new SequenceType[javaParameters.length];
            XPathUtil.TypeConversionPrecision prevTypeConversionPrecision = XPathUtil.TypeConversionPrecision.ONE_TO_ONE_MAPPING;

            for (int i = 0; i < javaParameters.length; i++) {
                final Parameter javaParameter = javaParameters[i];
                @Nullable XPathUtil.TypeConversionPrecision typeConversionPrecision = javaParameterCompatibleWithXdmParameter(javaParameter, xdmParameters.get(i));
                if (typeConversionPrecision == null) {
                    return null;
                }

                if (javaParameter.getType().isArray() && xdmParameters.get(i).returnsType() == Type.ARRAY) {
                    javaParametersEquivalentXdmTypes[i] = new SequenceType(Type.ARRAY, Cardinality.EXACTLY_ONE);
                } else {
                    javaParametersEquivalentXdmTypes[i] = relaxedToXQueryType(javaParameter.getType());
                }
                prevTypeConversionPrecision = typeConversionPrecision.min(prevTypeConversionPrecision);

                // is this the last Java parameter, and are there more XDM parameters than Java parameters?
                if (i == javaParameters.length - 1 && xdmParameters.size() > javaParameters.length) {

                    // is the last Java parameter varargs and can we fit the remaining XDM parameters into it?
                    if (javaParameter.getDeclaringExecutable().isVarArgs() && javaParameter.getType().isArray()) {
                        for (int j = i + 1; j < xdmParameters.size(); j++) {
                            typeConversionPrecision = javaParameterCompatibleWithXdmParameter(javaParameter, xdmParameters.get(j));
                            if (typeConversionPrecision == null) {
                                return null;
                            }
                        }
                        if (!javaParametersEquivalentXdmTypes[i].equals(relaxedToXQueryType(javaParameter.getType()))) {
                            // type must match previous type
                            return null;
                        }
                        prevTypeConversionPrecision = typeConversionPrecision.min(prevTypeConversionPrecision);
                        prevTypeConversionPrecision = prevTypeConversionPrecision.min(XPathUtil.TypeConversionPrecision.ONE_TO_ARRAY_MAPPING);

                    } else {
                        // we have too many XDM parameters for the number of Java parameters
                        return null;
                    }
                }
            }

            return Tuple(javaParametersEquivalentXdmTypes, prevTypeConversionPrecision);
        }

        return null;
    }

    private static XPathUtil.TypeConversionPrecision javaParameterCompatibleWithXdmParameter(final Parameter methodParameter, final Expression xqueryParameter) {
        final Class<?> expectedJavaClassType = methodParameter.getType();
        return XPathUtil.xdmTypeConvertibleToJavaClass(xqueryParameter.returnsType(), xqueryParameter.getCardinality(), expectedJavaClassType);
    }

    private static SequenceType relaxedToXQueryType(final Class<?> javaClass) {
        final SequenceType type = toXQueryType(javaClass);

        // broaden xs:int to xs:integer - makes JavaBinding easier to work with in XQuery code
        if (type.getPrimaryType() == Type.INT) {
            type.setPrimaryType(Type.INTEGER);
        }

        return type;
    }

    private static SequenceType toXQueryType(final Class<?> clazz) {
        final int type;
        final Cardinality cardinality;
        if (clazz.isArray()) {
            type = XPathUtil.javaClassToXdmType(clazz.getComponentType());
            cardinality = Cardinality.ZERO_OR_MORE;
        } else {
            type = XPathUtil.javaClassToXdmType(clazz);
            cardinality = Cardinality.EXACTLY_ONE;
        }
        return new SequenceType(type, cardinality);
    }

    /**
     * Convert a name in XQuery (Kebab Case) to Java (Camel Case).
     *
     * @param name the name to convert.
     *
     * @return the converted name.
     */
    static String xqueryKebabCaseToJavaCamelCase(final String name) {
        final char[] buf = new char[name.length()];
        int bi = 0;
        boolean prevWasHyphen = false;
        for (int ni = 0; ni < name.length(); ni++) {
            char c = name.charAt(ni);

            if (prevWasHyphen && (c >= 97 && c <= 122)) {
                // upper case the 'c' char
                c -= 32;
            }

            prevWasHyphen = c == '-';

            if (!prevWasHyphen) {
                buf[bi++] = c;
            }
        }

        return new String(buf, 0, bi);
    }

    private static Tuple2<String, JavaReflectiveCallParameters> getJavaClassName(String uri) {
        uri = uri.replaceFirst("^java:", "");
        final int idxParameters = uri.indexOf('?');
        final JavaReflectiveCallParameters javaBindingParameters;
        if (idxParameters > -1 && uri.endsWith("void=this")) {
            uri = uri.substring(0, idxParameters);
            javaBindingParameters = new JavaReflectiveCallParameters(true);
        } else {
            javaBindingParameters = new JavaReflectiveCallParameters(false);
        }
        return Tuple(uri, javaBindingParameters);
    }

    static class JavaReflectiveCallParameters {
        final boolean voidReturnsThis;

        public JavaReflectiveCallParameters(final boolean voidReturnsThis) {
            this.voidReturnsThis = voidReturnsThis;
        }
    }

    @FunctionalInterface
    private interface JavaReflectiveCallInvoker extends Function4E<Object[], Object, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException> { }

    /**
     * Simple data class to hold details of a possible Java reflective call for binding from XQuery.
     */
    public static class JavaReflectiveCall {
        final JavaReflectiveCallInvoker invoker;
        final boolean requiresInstanceArg;
        @Nullable final Parameter[] javaParameters;
        @Nullable final SequenceType[] javaParametersEquivalentXdmTypes;
        final XPathUtil.TypeConversionPrecision javaParametersEquivalentXdmTypesPrecision;
        final Class<?> javaReturnType;
        final SequenceType javaReturnTypeEquivalentXdmType;
        final JavaReflectiveCallParameters javaReflectiveCallParameters;

        private JavaReflectiveCall(final JavaReflectiveCallInvoker invoker, final boolean requiresInstanceArg, @Nullable final Parameter[] javaParameters, final SequenceType[] javaParametersEquivalentXdmTypes, final XPathUtil.TypeConversionPrecision javaParametersEquivalentXdmTypesPrecision, final Class<?> javaReturnType, final SequenceType javaReturnTypeEquivalentXdmType, final JavaReflectiveCallParameters javaReflectiveCallParameters) {
            this.invoker = invoker;
            this.requiresInstanceArg = requiresInstanceArg;
            this.javaParameters = javaParameters;
            this.javaParametersEquivalentXdmTypes = javaParametersEquivalentXdmTypes;
            this.javaParametersEquivalentXdmTypesPrecision = javaParametersEquivalentXdmTypesPrecision;
            this.javaReturnType = javaReturnType;
            this.javaReturnTypeEquivalentXdmType = javaReturnTypeEquivalentXdmType;
            this.javaReflectiveCallParameters = javaReflectiveCallParameters;
        }
    }

    private final List<JavaReflectiveCall> candidateJavaReflectiveCalls;

    private JavaBinding(final XQueryContext context, final FunctionSignature signature, final List<JavaReflectiveCall> candidateJavaReflectiveCalls) {
        super(context, signature);
        this.candidateJavaReflectiveCalls = candidateJavaReflectiveCalls;
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {

        // select the best JavaReflectiveCall from the available candidates by matching against the available args
        final Tuple2<JavaReflectiveCall, Object[]> bestJavaReflectiveCallAndJavaArgs = selectBestJavaReflectiveCall(candidateJavaReflectiveCalls, args);
        final JavaReflectiveCall javaReflectiveCall = bestJavaReflectiveCallAndJavaArgs._1;
        final Object[] javaArgs = bestJavaReflectiveCallAndJavaArgs._2;

        try {
            @Nullable final Object javaResult = javaReflectiveCall.invoker.apply(javaArgs);
            return XPathUtil.javaObjectToXPath(javaResult, context, true, false, false, this);

        } catch (final IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e) {
            throw new XPathException(this, new ErrorCodes.JavaErrorCode(e), e.getMessage());
        }
    }

    private static Tuple2<JavaReflectiveCall, Object[]> selectBestJavaReflectiveCall(final List<JavaReflectiveCall> candidateJavaReflectiveCalls, final Sequence[] args) throws XPathException {
        for (final JavaReflectiveCall candidateJavaReflectiveCall : candidateJavaReflectiveCalls) {
            final Object[] javaArgs = toJavaValues(args, candidateJavaReflectiveCall);

            boolean matchedAll = false;
            final int argsOffset = candidateJavaReflectiveCall.requiresInstanceArg ? 1 : 0;
            for (int i = argsOffset; i < javaArgs.length; i++) {
                @Nullable Parameter[] javaParameters = candidateJavaReflectiveCall.javaParameters;
                matchedAll = javaParameters != null && XPathUtil.isAssignableFrom(javaParameters[i - argsOffset].getType(), javaArgs[i].getClass());
                if (!matchedAll) {
                    break;
                }
            }

            if (matchedAll) {
                return Tuple(candidateJavaReflectiveCall, javaArgs);
            }
        }

        // could not find a best candidate so just return the first
        final JavaReflectiveCall bestJavaReflectiveCall = candidateJavaReflectiveCalls.get(0);
        final Object[] javaArgs = toJavaValues(args, bestJavaReflectiveCall);
        return Tuple(bestJavaReflectiveCall, javaArgs);
    }

    private static Object[] toJavaValues(final Sequence[] args, final JavaReflectiveCall javaReflectiveCall) throws XPathException {
        final Object[] javaValues = new Object[args.length];
        final int argsOffset = javaReflectiveCall.requiresInstanceArg ? 1 : 0;
        for (int i = 0; i < args.length; i++) {

            @Nullable Object javaValue = XPathUtil.xpathToJavaObject(args[i]);
            if (javaValue instanceof BigInteger) {
                final BigInteger bigInteger = (BigInteger) javaValue;
                if (javaReflectiveCall.javaParameters[i - argsOffset].getType() == int.class) {
                    javaValue = bigInteger.intValue();
                } else if (javaReflectiveCall.javaParameters[i - argsOffset].getType() == Integer.class) {
                    javaValue = Integer.valueOf(bigInteger.intValue());
                }
            }


            if ((!(javaValue != null && javaValue.getClass().isArray())) && javaReflectiveCall.javaParameters != null && javaReflectiveCall.javaParameters.length > i && javaReflectiveCall.javaParameters[i].getType().isArray()) {
                Object arrayWrappedJavaValue = java.lang.reflect.Array.newInstance(javaReflectiveCall.javaParameters[i].getType().getComponentType(), 1);
                java.lang.reflect.Array.set(arrayWrappedJavaValue, 0, javaValue);
                javaValue = arrayWrappedJavaValue;
            }

            javaValues[i] = javaValue;
        }
        return javaValues;
    }
}
