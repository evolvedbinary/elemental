/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package org.exist.indexing.lucene;

import java.io.Reader;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

import org.exist.collections.CollectionConfiguration;
import org.exist.util.DatabaseConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static java.lang.invoke.MethodType.methodType;
import static org.exist.util.StringUtil.isNullOrEmptyOrWs;

public class AnalyzerConfig {

    /*

     Supported configurations

     <analyzer class="org.apache.lucene.analysis.standard.StandardAnalyzer"/>

     <analyzer id="ws" class="org.apache.lucene.analysis.core.WhitespaceAnalyzer"/>

     <analyzer id="stdstops" class="org.apache.lucene.analysis.standard.StandardAnalyzer">
     ..<param name="stopwords" type="java.io.File" value="/tmp/stop.txt"/>
     </analyzer>

     <analyzer id="stdstops" class="org.apache.lucene.analysis.standard.StandardAnalyzer">
     ..<param name="stopwords" type="java.util.Set">
     ....<value>the</value>
     ....<value>this</value>
     ....<value>and</value>
     ....<value>that</value>
     ..</param>
     </analyzer>

     <analyzer id="sbstops" class="org.apache.lucene.analysis.snowball.SnowballAnalyzer">
     ..<param name="name" value="English"/>
     ..<param name="stopwords" type="java.util.Set">
     ....<value>the</value>
     ....<value>this</value>
     ....<value>and</value>
     ....<value>that</value>
     ..</param>
     </analyzer>

     */
    private static final Logger LOG = LogManager.getLogger(AnalyzerConfig.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final String ID_ATTRIBUTE = "id";

    private static final String NAME_ATTRIBUTE = "name";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final String PARAM_VALUE_ENTRY = "value";
    private static final String PARAM_ELEMENT_NAME = "param";

    private Map<String, Analyzer> analyzers = new TreeMap<>();
    private Analyzer defaultAnalyzer = new StandardAnalyzer();

    public Analyzer getAnalyzerById(String id) {
        return analyzers.get(id);
    }

    public Analyzer getDefaultAnalyzer() {
        return defaultAnalyzer;
    }

    /**
     * Parse {@code <analyzer/>} element and register configured analyzer.
     *
     * @param config The analyzer element from .xconf file.
     *
     * @throws DatabaseConfigurationException Something unexpected happened.
     */
    public void addAnalyzer(Element config) throws DatabaseConfigurationException {

        // Configure lucene analuzer with configuration
        final Analyzer analyzer = configureAnalyzer(config);
        if (analyzer == null) {
            return;
        }

        // Get (optional) id-attribute of analyzer
        final String id = config.getAttribute(ID_ATTRIBUTE).trim();

        // If no ID is provided, register as default analyzer
        // else register analyzer
        if (id.isEmpty()) {
            setDefaultAnalyzer(analyzer);
        } else {
            analyzers.put(id, analyzer);
        }
    }

    /**
     * Set default the analyzer.
     *
     * @param analyzer Lucene analyzer
     */
    public void setDefaultAnalyzer(Analyzer analyzer) {
        defaultAnalyzer = analyzer;
    }

    /**
     * Parse {@code <analyzer/>} element from xconf and initialize an analyzer with the
     * parameters.
     *
     * @param config The analyzer element
     * @return Initialized Analyzer object
     *
     * @throws DatabaseConfigurationException Something unexpected happened.
     */
    protected static Analyzer configureAnalyzer(Element config) throws DatabaseConfigurationException {

        // Get classname from attribute
        final String className = config.getAttribute(CLASS_ATTRIBUTE).trim();

        Analyzer newAnalyzer = null;

        if (className.isEmpty()) {
            // No classname is defined.
            LOG.error("Missing class attribute or attribute is empty.");
            // DW: throw exception?

        } else {
            // Classname is defined.

            // Probe class
            final Class<?> untypedClazz;
            try {
                untypedClazz = Class.forName(className);

            } catch (ClassNotFoundException e) {
                LOG.error(String.format("Lucene index: analyzer class %s not found. (%s)", className, e.getMessage()));
                return null;
            }

            // CHeck if class is an Analyzer
            if (!Analyzer.class.isAssignableFrom(untypedClazz)) {
                LOG.error(String.format("Lucene index: analyzer class has to be a subclass of %s", Analyzer.class.getName()));
                return null;
            }

            final Class<? extends Analyzer> clazz = (Class<? extends Analyzer>) untypedClazz;

            // Get list of parameters
            List<KeyTypedValue<?>> cParams;
            try {
                cParams = getAllConstructorParameters(config);

            } catch (ParameterException pe) {
                // Unable to parse parameters.
                LOG.error(String.format("Unable to get parameters for %s: %s", className, pe.getMessage()), pe);
                cParams = new ArrayList<>();
            }

            // Iterate over all parameters, convert data to two arrays
            // that can be used in the reflection code
            final Class<?> cParamClasses[] = new Class<?>[cParams.size()];
            final Object cParamValues[] = new Object[cParams.size()];
            for (int i = 0; i < cParams.size(); i++) {
                KeyTypedValue<?> ktv = cParams.get(i);
                cParamClasses[i] = ktv.valueClass();
                cParamValues[i] = ktv.value();
            }

            // Create new analyzer
            if (cParamClasses.length > 0 && cParamClasses[0] == Version.class) {

                if (LOG.isDebugEnabled()) {
                    Version version = (Version) cParamValues[0];
                    LOG.debug(String.format("An explicit Version %s of lucene has been specified.", version.toString()));
                }

                // A lucene Version object has been provided, so it shall be used
                newAnalyzer = createInstance(clazz, cParamClasses, cParamValues, false);

            } else {
                // Either no parameters have been provided, or more than one parameter

                // Extend arrays with (default) Version object info, add to front.
                Class<?>[] vcParamClasses = addVersionToClasses(cParamClasses);
                Object[] vcParamValues = addVersionValueToValues(cParamValues);

                // Finally create Analyzer
                newAnalyzer = createInstance(clazz, vcParamClasses, vcParamValues, true);

                // Fallback scenario: a special (not standard type of) Analyzer has been specified without 
                // a 'Version' argument on purpose. For this (try) to create the Analyzer with 
                // the original parameters.
                if (newAnalyzer == null) {
                    newAnalyzer = createInstance(clazz, cParamClasses, cParamValues, false);
                }

            }

        }

        if (newAnalyzer == null) {
            LOG.error(String.format("Unable to create analyzer '%s'", className));
        }

        return newAnalyzer;
    }

    /**
     * Create instance of the lucene analyzer with provided arguments
     *
     * @param clazz The analyzer class
     * @param vcParamClasses The parameter classes
     * @param vcParamValues The parameter values
     * @param warnOnError true if an error should be treated as a warning
     * @return The lucene analyzer
     */
    static <T extends Analyzer> T createInstance(final Class<T> clazz, final Class<?>[] vcParamClasses,
        final Object[] vcParamValues, final boolean warnOnError) {

        final String className = clazz.getName();

        MethodType constructorType = methodType(void.class);
        for (final Class<?> vcParamClazz : vcParamClasses) {
            constructorType = constructorType.appendParameterTypes(vcParamClazz);
        }

        try {
            final MethodHandle methodHandle = LOOKUP.findConstructor(clazz, constructorType);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Using analyzer %s", className));
            }
            return (T) methodHandle.invokeWithArguments(vcParamValues);
        } catch (final NoSuchMethodException e) {
            final String message = String.format("Could not find matching analyzer class constructor %s: %s", className, e.getMessage());
            if (warnOnError) {
                LOG.warn("{}. Will retry...", message);
            } else {
                LOG.error(message, e);
            }
        } catch (final Throwable e) {
            if (e instanceof InterruptedException) {
                // NOTE: must set interrupted flag
                Thread.currentThread().interrupt();
            }

            final String message = String.format("Exception while instantiating analyzer class %s: %s", className, e.getMessage());
            if (warnOnError) {
                LOG.warn("{}. Will retry...", message);
            } else {
                LOG.error(message, e);
            }
        }
        return null;
    }

    /**
     * Extend list of values, add version-value to front
     */
    private static Object[] addVersionValueToValues(final Object[] cParamValues) {
        final Object vcParamValues[] = new Object[cParamValues.length + 1];
        vcParamValues[0] = LuceneIndex.LUCENE_VERSION_IN_USE;
        System.arraycopy(cParamValues, 0, vcParamValues, 1, cParamValues.length);
        return vcParamValues;
    }

    /**
     * Extend list of classes, add version-class to front
     */
    private static Class<?>[] addVersionToClasses(final Class<?>[] cParamClasses) {
        final Class<?> vcParamClasses[] = new Class<?>[cParamClasses.length + 1];
        vcParamClasses[0] = Version.class;
        System.arraycopy(cParamClasses, 0, vcParamClasses, 1, cParamClasses.length);
        return vcParamClasses;
    }

    /**
     * Retrieve parameter info from all <param/> elements.
     *
     * @param config The <analyzer/> element from the provided configuration
     * @return List of triples key-value-valueType
     * @throws org.exist.indexing.lucene.AnalyzerConfig.ParameterException
     */
    static List<KeyTypedValue<?>> getAllConstructorParameters(Element config) throws ParameterException {
        final List<KeyTypedValue<?>> parameters = new ArrayList<>();
        final NodeList params = config.getElementsByTagNameNS(CollectionConfiguration.NAMESPACE, PARAM_ELEMENT_NAME);

        // iterate over all <param/> elements
        for (int i = 0; i < params.getLength(); i++) {
            parameters.add(getConstructorParameter((Element) params.item(i)));
        }

        return parameters;
    }

    /**
     * Retrieve configuration information from one <param/> element. Type
     * information is used to construct actual data containing objects.
     *
     * @param param Element that represents <param/>
     * @return Triple key-value-value-type
     * @throws org.exist.indexing.lucene.AnalyzerConfig.ParameterException
     */
    static KeyTypedValue<?> getConstructorParameter(final Element param) throws ParameterException {

        // Get attributes
        final NamedNodeMap attrs = param.getAttributes();

        // Get name of parameter, NULL when no value is present
        Node namedItem = attrs.getNamedItem(NAME_ATTRIBUTE);
        final String name = (namedItem == null) ? null : namedItem.getNodeValue();

        // Get value type information of parameter, NULL when not available
        namedItem = attrs.getNamedItem(TYPE_ATTRIBUTE);
        final String type = (namedItem == null) ? null : namedItem.getNodeValue();

        // Get actual value from attribute, or NULL when not available.
        namedItem = attrs.getNamedItem(PARAM_VALUE_ENTRY);
        final String value = (namedItem == null) ? null : namedItem.getNodeValue();

        // Place holder return value
        KeyTypedValue<?> parameter = null;

        if (isNullOrEmptyOrWs(type) || "java.lang.String".equals(type)) {
            // String or no type is provided, assume string.

            if (value == null) {
                throw new ParameterException("The 'value' attribute must exist and must contain String value.");
            }

            parameter = new KeyTypedValue<>(name, value, String.class);

        } else {
            switch (type) {
                case "java.lang.reflect.Field":

                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain a full classname.");
                    }

                    // Use reflection
                    // - retrieve classname from the value field
                    // - retrieve fieldname from the value field
                    final String clazzName = value.substring(0, value.lastIndexOf('.'));
                    final String fieldName = value.substring(value.lastIndexOf('.') + 1);
                    try {
                        // Retrieve value from Field
                        final Class<?> fieldClazz = Class.forName(clazzName);
                        final Field field = fieldClazz.getField(fieldName);
                        field.setAccessible(true);
                        final Object fValue = field.get(fieldClazz.newInstance());
                        parameter = new KeyTypedValue<>(name, fValue, Object.class);

                    } catch (final NoSuchFieldException | ClassNotFoundException | InstantiationException | IllegalAccessException nsfe) {
                        throw new ParameterException(nsfe.getMessage(), nsfe);
                    }
                    break;

                case "java.io.File": {

                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain a file name.");
                    }

                    LOG.info(String.format("Type '%s' has been deprecated in recent Lucene versions, "
                            + "please use 'java.io.FileReader' (short 'file') instead.", type));

                    parameter = new KeyTypedValue<>(name, new java.io.File(value), java.io.File.class);
                    break;
                }

                case "java.io.FileReader":
                case "file": {

                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain a file name.");
                    }

                    try {
                        // ToDo: check where to close reade to prevent resource leakage
                        Reader fileReader = new java.io.FileReader(value);
                        parameter = new KeyTypedValue<>(name, fileReader, Reader.class);

                    } catch (java.io.FileNotFoundException ex) {
                        LOG.error(String.format("File '%s' could not be found.", value), ex);
                    }
                    break;
                }

                case "java.util.Set": {

                    LOG.info(String.format("Type '%s' has been deprecated in recent Lucene versions, "
                            + "please use 'org.apache.lucene.analysis.util.CharArraySet' (short 'set') instead.", type));

                    final Set s = getConstructorParameterSetValues(param);
                    parameter = new KeyTypedValue<>(name, s, Set.class);
                    break;
                }

                case "java.lang.String[]": {
                    final String[] ary = getConstructorParameterStringArrayValues(param);
                    parameter = new KeyTypedValue<>(name, ary, String[].class);
                    break;
                }

                case "char[]": {
                    final char[] ary = getConstructorParameterCharArrayValues(param);
                    parameter = new KeyTypedValue<>(name, ary, char[].class);
                    break;
                }

                case "org.apache.lucene.analysis.util.CharArraySet":
                case "set": {
                    // This is mandatory to use iso a normal Set since Lucene 4
                    final CharArraySet s = getConstructorParameterCharArraySetValues(param);
                    parameter = new KeyTypedValue<>(name, s, CharArraySet.class);
                    break;
                }

                case "java.lang.Integer":
                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain an integer value.");
                    }
                    try {
                        final Integer n = Integer.parseInt(value);
                        parameter = new KeyTypedValue<>(name, n, Integer.class);
                    } catch (final NumberFormatException ex) {
                        LOG.error(String.format("Value %s could not be converted to an integer. %s", value, ex.getMessage()));
                    }
                    break;

                case "int":
                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain an int value.");
                    }
                    try {
                        final Integer n = Integer.parseInt(value);
                        parameter = new KeyTypedValue<>(name, n, int.class);
                    } catch (final NumberFormatException ex) {
                        LOG.error(String.format("Value %s could not be converted to an int. %s", value, ex.getMessage()));
                    }
                    break;

                case "java.lang.Boolean":
                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain a Boolean value.");
                    }
                    final Boolean b1 = Boolean.parseBoolean(value);
                    parameter = new KeyTypedValue<>(name, b1, Boolean.class);
                    break;

                case "boolean":
                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain a boolean value.");
                    }
                    final Boolean b2 = Boolean.parseBoolean(value);
                    parameter = new KeyTypedValue<>(name, b2, boolean.class);
                    break;

                default:
                    // FallBack there was no match

                    if (value == null) {
                        throw new ParameterException("The 'value' attribute must exist and must contain a value.");
                    }

                    try {
                        //if the type is an Enum then use valueOf()
                        final Class clazz = Class.forName(type);
                        if (clazz.isEnum()) {
                            parameter = new KeyTypedValue<>(name, Enum.valueOf(clazz, value), clazz);
                        } else {
                            //default, assume java.lang.String
                            parameter = new KeyTypedValue<>(name, value, String.class);
                        }

                    } catch (ClassNotFoundException cnfe) {
                        throw new ParameterException(String.format("Class for type: %s not found. %s", type, cnfe.getMessage()), cnfe);
                    }
                    break;
            }
        }

        return parameter;
    }

    /**
     * Get parameter configuration data as standard Java (Hash)Set.
     *
     * @param param The parameter-configuration element.
     * @return Set of parameter values
     */
    private static Set<String> getConstructorParameterSetValues(Element param) {
        final Set<String> set = new HashSet<>();
        final NodeList values = param.getElementsByTagNameNS(CollectionConfiguration.NAMESPACE, PARAM_VALUE_ENTRY);
        for (int i = 0; i < values.getLength(); i++) {
            final Element value = (Element) values.item(i);
            set.add(value.getTextContent());
        }

        return set;
    }

    /**
     * Get parameter configuration data as a String[].
     *
     * @param param The parameter-configuration element.
     * @return Parameter data as String[]
     */
    private static String[] getConstructorParameterStringArrayValues(final Element param) throws ParameterException {
        final NodeList values = param.getElementsByTagNameNS(CollectionConfiguration.NAMESPACE, PARAM_VALUE_ENTRY);
        final String[] ary = new String[values.getLength()];
        for (int i = 0; i < values.getLength(); i++) {
            final Element value = (Element) values.item(i);
            ary[i] = value.getTextContent();
        }
        return ary;
    }

    /**
     * Get parameter configuration data as a char[].
     *
     * @param param The parameter-configuration element.
     * @return Parameter data as char[]
     */
    private static char[] getConstructorParameterCharArrayValues(final Element param) throws ParameterException {
        final NodeList values = param.getElementsByTagNameNS(CollectionConfiguration.NAMESPACE, PARAM_VALUE_ENTRY);
        final char[] ary = new char[values.getLength()];
        for (int i = 0; i < values.getLength(); i++) {
            final Element value = (Element) values.item(i);
            final String s = value.getTextContent();
            if (s == null || s.isEmpty()) {
                throw new ParameterException("The 'value[" + (i + 1) + "]' must be a single character.");
            }
            ary[i] = s.charAt(0);
        }
        return ary;
    }

    /**
     * Get parameter configuration data as a Lucene CharArraySet.
     *
     * @param param The parameter-configuration element.
     * @return Parameter data as Lucene CharArraySet
     */
    private static CharArraySet getConstructorParameterCharArraySetValues(Element param) {
        final Set<String> set = getConstructorParameterSetValues(param);
        return CharArraySet.copy(LuceneIndex.LUCENE_VERSION_IN_USE, set);
    }

    /**
         * CLass for containing the Triple : key (name), corresponding value and
         * class type of value.
         */
        record KeyTypedValue<T>(String key, T value, Class<T> valueClass) {

    }

    /**
     * Exception class to for reporting problems with the parameters.
     */
    static class ParameterException extends Exception {

        private static final long serialVersionUID = -4823392401966008877L;

        public ParameterException(String message) {
            super(message);
        }

        public ParameterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
