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
package xyz.elemental.build.tools.spoon.processors;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of a Java Annotation Processor that
 * processes org.exist.security.PermissionRequired annotations
 * and generates secure subclasses that enforce the checks.
 *
 * Plugs into the Java Compiler.
 *
 * @author <a href="mailto:adam@evolvedbinary.com>Adam Retter </a>
 */
@SupportedAnnotationTypes("org.exist.security.PermissionRequired")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PermissionRequiredProcessor extends AbstractProcessor {

    private static final String PERMISSION_REQUIRED_ANNOTATION_NAME = "org.exist.security.PermissionRequired";
    private static final String PERMISSION_REQUIRED_UNDEFINED = PERMISSION_REQUIRED_ANNOTATION_NAME + ".UNDEFINED";
    private static final String PERMISSION_REQUIRED_CHECK_CLASS_NAME = "org.exist.security.PermissionRequiredCheck";
    private static final String PERMISSION_REQUIRED_CHECK_CLASS_METHOD_CHECK_NAME = "checkMethodPermissions";
    private static final String PERMISSION_REQUIRED_CHECK_CLASS_METHOD_PARAMETER_CHECK_NAME = "checkMethodParameterPermissions";
    private static final String BASE_CLASS_SUFFIX = "Internal";

    public PermissionRequiredProcessor() {
        super();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("%s processing round...", PermissionRequiredProcessor.class.getName()));

        // collect the details of the annotated methods and parameters
        final Map<TypeElement, Map<ExecutableElement, List<VariableElement>>> annotatedClassesMethods = collectAnnotatedMethodsAndParameters(annotations, roundEnv);

        // generate subclasses from them
        generateSubClasses(annotatedClassesMethods);

        return true;
    }

    private void generateSubClasses(final Map<TypeElement, Map<ExecutableElement, List<VariableElement>>> annotatedClassesMethods) {
        for (final Map.Entry<TypeElement, Map<ExecutableElement, List<VariableElement>>> annotatedClassMethods : annotatedClassesMethods.entrySet()) {
            final TypeElement clazz = annotatedClassMethods.getKey();
            final String clazzQualifiedName = clazz.getQualifiedName().toString();
            final String subClazzQualifiedName = clazzQualifiedName.replace(BASE_CLASS_SUFFIX, "");

            try {
                generateSubClass(clazz, clazzQualifiedName, subClazzQualifiedName, annotatedClassMethods);
            } catch (final IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Failed to generate subclass named: %s of class: %s. Error: %s", subClazzQualifiedName, clazzQualifiedName, e.getMessage()), clazz);
            }
        }
    }

    private void generateSubClass(final TypeElement clazz, final String clazzQualifiedName, final String subClazzQualifiedName, final Map.Entry<TypeElement, Map<ExecutableElement, List<VariableElement>>> annotatedClassMethods) throws IOException {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("\tGenerating subclass named: %s of class: %s", subClazzQualifiedName, clazzQualifiedName));

        final int clazzQualifiedNameLastSep = clazzQualifiedName.lastIndexOf('.');
        final String clazzPackageName = clazzQualifiedName.substring(0, clazzQualifiedNameLastSep);
        final String clazzSimpleName = clazzQualifiedName.substring(clazzQualifiedNameLastSep + 1);

        final int subClazzQualifiedNameLastSep = subClazzQualifiedName.lastIndexOf('.');
        final String subClazzPackageName = subClazzQualifiedName.substring(0, subClazzQualifiedNameLastSep);
        final String subClazzSimpleName = subClazzQualifiedName.substring(subClazzQualifiedNameLastSep + 1);

        final int checkClassQualifiedNameLastSep = PERMISSION_REQUIRED_CHECK_CLASS_NAME.lastIndexOf('.');
        final String checkClazzSimpleName = PERMISSION_REQUIRED_CHECK_CLASS_NAME.substring(checkClassQualifiedNameLastSep + 1);

        final TypeSpec.Builder subClazzSpecBuilder = TypeSpec.classBuilder(subClazzSimpleName)
            .addJavadoc("Wraps {@link $L} with {@link $L} to enforce security.\n\nTHIS CODE WAS GENERATED AT COMPILE TIME BY: $L", clazzSimpleName, checkClazzSimpleName, getClass().getName())
            .addModifiers(Modifier.PUBLIC)
            .superclass(ClassName.get(clazzPackageName, clazzSimpleName));

        // copy constructors from class to subclass
        for (final Element element : clazz.getEnclosedElements()) {
            if (element instanceof ExecutableElement) {
                final ExecutableElement executableElement = (ExecutableElement) element;
                if ("<init>".equals(executableElement.getSimpleName().toString())) {

                    final MethodSpec constructorSpec = MethodSpec.constructorBuilder()
                        .addParameters(executableElement.getParameters().stream().map(ParameterSpec::get).collect(Collectors.toList()))
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("super($L)", executableElement.getParameters())
                        .build();

                    subClazzSpecBuilder.addMethod(constructorSpec);

                } else if ("newInstance".equals(executableElement.getSimpleName().toString())) {
                    // implement abstract newInstance method in subclass
                    final MethodSpec newInstanceSpec = MethodSpec.overriding(executableElement)
                        .addStatement("return new $L($L)", subClazzSimpleName, executableElement.getParameters())
                        .build();
                    subClazzSpecBuilder.addMethod(newInstanceSpec);
                }
            }
        }

        // override annotated methods in subclass
        for (final Map.Entry<ExecutableElement, List<VariableElement>> annotatedClassMethod : annotatedClassMethods.getValue().entrySet()) {
            final ExecutableElement method = annotatedClassMethod.getKey();
            final String methodName = method.getSimpleName().toString();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("\t\tOverriding method: %s", methodName));

            final MethodSpec.Builder methodSpecBuilder = MethodSpec.overriding(annotatedClassMethod.getKey());
            @Nullable final CodeBlock permissionRequiredCheckStatements = generatePermissionRequiredCheckStatements(method, annotatedClassMethod.getValue());
            if (permissionRequiredCheckStatements != null) {
                methodSpecBuilder.addCode(permissionRequiredCheckStatements);
            }
            methodSpecBuilder.addStatement("super.$L($L)", methodName, method.getParameters());

            subClazzSpecBuilder.addMethod(methodSpecBuilder.build());
        }

        final JavaFile javaFile = JavaFile.builder(subClazzPackageName, subClazzSpecBuilder.build())
            .build();

        javaFile.writeTo(processingEnv.getFiler());
    }

    private @Nullable CodeBlock generatePermissionRequiredCheckStatements(final ExecutableElement method, final List<VariableElement> methodParameters) {
        @Nullable CodeBlock.Builder codeBlockBuilder = null;

        // Add PermissionRequiredCheck based on Method Annotations
        @Nullable Requires methodRequires = null;
        final List<? extends AnnotationMirror> methodAnnotations = method.getAnnotationMirrors();
        for (final AnnotationMirror methodAnnotation : methodAnnotations) {
            final String methodAnnotationName = ((TypeElement) methodAnnotation.getAnnotationType().asElement()).getQualifiedName().toString();

            if (methodAnnotationName.equals(PERMISSION_REQUIRED_ANNOTATION_NAME)) {
                methodRequires = getRequires(methodAnnotation);

                if (codeBlockBuilder == null) {
                    codeBlockBuilder = CodeBlock.builder();
                }
                codeBlockBuilder.add("// NOTE(AR) Enforce Method Permission Required Check\n");
                codeBlockBuilder.addStatement("$L.$L(this, $L, $L, $L)", PERMISSION_REQUIRED_CHECK_CLASS_NAME, PERMISSION_REQUIRED_CHECK_CLASS_METHOD_CHECK_NAME, methodRequires.user, methodRequires.group, methodRequires.mode);

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("\t\t\tAdded %s", PERMISSION_REQUIRED_CHECK_CLASS_METHOD_CHECK_NAME));
            }
        }

        if (methodRequires == null) {
            methodRequires = new Requires(PERMISSION_REQUIRED_UNDEFINED, PERMISSION_REQUIRED_UNDEFINED, PERMISSION_REQUIRED_UNDEFINED);
        }

        // Add PermissionRequiredCheck(s) based on Parameter Annotations
        for (final VariableElement methodParameter : methodParameters) {
            final String methodParameterName = methodParameter.getSimpleName().toString();
            final List<? extends AnnotationMirror> methodParameterAnnotations = methodParameter.getAnnotationMirrors();

            for (final AnnotationMirror methodParameterAnnotation : methodParameterAnnotations) {
                final String methodParameterAnnotationName = ((TypeElement) methodParameterAnnotation.getAnnotationType().asElement()).getQualifiedName().toString();

                if (methodParameterAnnotationName.equals(PERMISSION_REQUIRED_ANNOTATION_NAME)) {
                    final Requires methodParameterRequires = getRequires(methodParameterAnnotation);

                    if (codeBlockBuilder == null) {
                        codeBlockBuilder = CodeBlock.builder();
                    }
                    codeBlockBuilder.add("// NOTE(AR) Enforce Method Parameter Permission Required Check\n");
                    codeBlockBuilder.addStatement("$L.$L(this, $L, $L, $L, $L, $L, $L, $L)", PERMISSION_REQUIRED_CHECK_CLASS_NAME, PERMISSION_REQUIRED_CHECK_CLASS_METHOD_PARAMETER_CHECK_NAME, methodRequires.user, methodRequires.group, methodRequires.mode, methodParameterName, methodParameterRequires.user, methodParameterRequires.group, methodParameterRequires.mode);

                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("\t\t\tAdded %s", PERMISSION_REQUIRED_CHECK_CLASS_METHOD_PARAMETER_CHECK_NAME));
                }
            }
        }

        if (codeBlockBuilder != null) {
            return codeBlockBuilder.build();
        } else {
            return null;
        }
    }

    private Requires getRequires(final AnnotationMirror annotation) {
        String user = PERMISSION_REQUIRED_UNDEFINED;
        String group = PERMISSION_REQUIRED_UNDEFINED;
        String mode = PERMISSION_REQUIRED_UNDEFINED;

        final Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues = annotation.getElementValues();
        for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> annotationValue : annotationValues.entrySet()) {
            final String annotationName = annotationValue.getKey().getSimpleName().toString();
            final AnnotationValue value = annotationValue.getValue();

            switch (annotationName) {
                case "user":
                    user = value.toString();
                    break;
                case "group":
                    group = value.toString();
                    break;
                case "mode":
                    mode = value.toString();
                    break;
            }
        }

        return new Requires(user, group, mode);
    }

    /**
     * Collect all the Annotated Method and Parameters
     * and group them by Class.
     *
     * Returns a Map whose key is a Class (of annotated Methods/Parameters),
     * the value is a Map of methods and a list of any annotated method parameters.
     * @return Map<Class, Map<Method, List<Parameter>>
     */
    private Map<TypeElement, Map<ExecutableElement, List<VariableElement>>> collectAnnotatedMethodsAndParameters(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final Map<TypeElement, Map<ExecutableElement, List<VariableElement>>> annotatedClassesMethods = new LinkedHashMap<>();

        for (final TypeElement annotation : annotations) {

            // Process each element
            final Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (final Element element : annotatedElements) {

                if (element instanceof ExecutableElement) {
                    final ExecutableElement method = (ExecutableElement) element;
                    if (!validateMethod(method)) {
                        continue;
                    }

                    final TypeElement clazz = (TypeElement) method.getEnclosingElement();
                    if (!validateClass(clazz)) {
                        continue;
                    }

                    annotatedClassesMethods
                        .computeIfAbsent(clazz, c -> new LinkedHashMap<>())
                        .computeIfAbsent(method, m -> new ArrayList<>());

                } else if (element instanceof VariableElement) {
                    final VariableElement parameter = (VariableElement) element;
                    final ExecutableElement method = (ExecutableElement) parameter.getEnclosingElement();
                    if (!validateParameter(method, parameter)) {
                        continue;
                    }

                    final TypeElement clazz = (TypeElement) method.getEnclosingElement();
                    if (!validateClass(clazz)) {
                        continue;
                    }

                    annotatedClassesMethods
                        .computeIfAbsent(clazz, c -> new LinkedHashMap<>())
                        .computeIfAbsent(method, m -> new ArrayList<>())
                        .add(parameter);

                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("The %s annotation is only supported on methods and method parameters", PERMISSION_REQUIRED_ANNOTATION_NAME));
                    continue;
                }
            }
        }

        return annotatedClassesMethods;
    }

    private boolean validateClass(final TypeElement clazz) {
        if (!clazz.getModifiers().contains(Modifier.ABSTRACT)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("The %s annotation is only supported in abstract classes, but found in class: %s", PERMISSION_REQUIRED_ANNOTATION_NAME, clazz.getQualifiedName().toString()));
            return false;
        }

        if (!clazz.getSimpleName().toString().endsWith(BASE_CLASS_SUFFIX)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("The %s annotation is only supported classes whose name is suffixed with the string: '%s', but found in class: %s", PERMISSION_REQUIRED_ANNOTATION_NAME, BASE_CLASS_SUFFIX, clazz.getQualifiedName().toString()));
            return false;
        }

        return true;
    }

    private boolean validateMethod(final ExecutableElement method) {
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("The %s annotation is not supported on private methods, but found on method: %s.%s", PERMISSION_REQUIRED_ANNOTATION_NAME, ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString(), method.getSimpleName()));
            return false;
        }

        if (method.getModifiers().contains(Modifier.FINAL)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("The %s annotation is not supported on final methods, but found on method: %s.%s", PERMISSION_REQUIRED_ANNOTATION_NAME, ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString(), method.getSimpleName()));
            return false;
        }

        return true;
    }

    private boolean validateParameter(final ExecutableElement method, final VariableElement parameter) {
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("The %s annotation is not supported on parameters of private methods, but found on parameter: %s of method: %s.%s", PERMISSION_REQUIRED_ANNOTATION_NAME, parameter.getSimpleName(), ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString(), method.getSimpleName()));
            return false;
        }

        if (method.getModifiers().contains(Modifier.FINAL)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("The %s annotation is not supported on parameters of final methods, but found on parameter: %s of method: %s.%s", PERMISSION_REQUIRED_ANNOTATION_NAME, parameter.getSimpleName(), ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString(), method.getSimpleName()));
            return false;
        }

        return true;
    }

    private static class Requires {
        final String user;
        final String group;
        final String mode;

        public Requires(final String user, final String group, final String mode) {
            this.user = user;
            this.group = group;
            this.mode = mode;
        }
    }
}
