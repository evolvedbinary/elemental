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
package org.exist.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility class for extracting parameters from 
 * DOM representation into a Map.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ParametersExtractor {

    public final static String PARAMETERS_ELEMENT_NAME = "parameters";
    public final static String PARAMETER_ELEMENT_NAME = "parameter";
    private final static String PARAMETER_NAME_ATTRIBUTE = "name";
    private final static String PARAMETER_VALUE_ATTRIBUTE = "value";

    /**
     * Extract the parameters.
     *
     * @param parameters A "parameters" element, which may contain "parameter" child elements
     *
     * @return the parameters
     */
    public static Map<String, List<? extends Object>> extract(final Element parameters) {

        final Map<String, List<? extends Object>> result;

        if(parameters == null || !parameters.getLocalName().equals(PARAMETERS_ELEMENT_NAME)) {
            result = new HashMap<>(0);
        } else {

            final String namespace = parameters.getNamespaceURI();
            final NodeList nlParameter = parameters.getElementsByTagNameNS(namespace, PARAMETER_ELEMENT_NAME);

            result = extract(nlParameter);
        }

        return result;
    }

    /**
     * Extract the parameters.
     *
     * @param nlParameter A NodeList of "parameter" elements
     *
     * @return the parameters
     */
    public static Map<String, List<? extends Object>> extract(final NodeList nlParameter) {

        final Map<String, List<? extends Object>> result;

        if(nlParameter == null || nlParameter.getLength() == 0) {
            result = new HashMap<>(0);
        } else {
            result = extractParameters(nlParameter);
        }


        return result;
    }

    private static Map<String, List<? extends Object>> extractParameters(final NodeList nlParameter) {

        final Map<String, List<?>> parameters = new HashMap<>(nlParameter.getLength());

        for (int i = 0 ; i < nlParameter.getLength();  i++) {
            final Element param = (Element)nlParameter.item(i);
            //TODO : rely on schema-driven validation -pb
            final String name = param.getAttribute(PARAMETER_NAME_ATTRIBUTE);
            /*if(name == null) {
                throwOrLog("Expected attribute '" + PARAMETER_NAME_ATTRIBUTE + "' for element '" + PARAMETER_ELEMENT_NAME + "' in trigger's configuration.", testOnly);
            }*/

            List values = parameters.get(name);

            final String value = param.getAttribute(PARAMETER_VALUE_ATTRIBUTE);
            if (!value.isEmpty()) {
                if(values == null) {
                    values = new ArrayList<String>();
                }
                values.add(value);
            } else {
                //are there child nodes?
                if(param.getChildNodes().getLength() > 0) {

                    if(values == null) {
                        values = new ArrayList<Map<String, List>>();
                    }

                    values.add(getParameterChildParameters(param));
                }
            }

            parameters.put(name, values);
        }

        return parameters;
    }

    private static Map<String, List> getParameterChildParameters(final Element parameter) {

        final Map<String, List> results = new HashMap<>();

        final NodeList childParameters = parameter.getChildNodes();
        for(int i = 0; i < childParameters.getLength(); i++) {
            final Node nChildParameter = childParameters.item(i);
            if(nChildParameter instanceof Element childParameter) {
                final String name = childParameter.getLocalName();

                if(childParameter.getAttributes().getLength() > 0){
                    List<Properties> childParameterProperties = (List<Properties>)results.get(name);
                    if(childParameterProperties == null) {
                        childParameterProperties = new ArrayList<>();
                    }

                    final NamedNodeMap attrs = childParameter.getAttributes();
                    final Properties props = new Properties();
                    for(int a = 0; a < attrs.getLength(); a++) {
                        final Node attr = attrs.item(a);
                        props.put(attr.getLocalName(), attr.getNodeValue());
                    }
                    childParameterProperties.add(props);

                    results.put(name, childParameterProperties);
                } else {
                    List<String> strings = (List<String>)results.get(name);
                    if(strings == null) {
                        strings = new ArrayList<>();
                    }
                    strings.add(childParameter.getTextContent());
                    results.put(name, strings);
                }
            }
        }

        return results;
    }

    /**
     * Parses a structure like:
     * <pre>
     * {@code
     *  <parameters>
     *    <param name="a" value="1"/><param name="b" value="2"/>
     *  </parameters>
     * }
     * </pre>
     * into a set of Properties.
     *
     * @param nParameters
     *            The parameters Node
     * @return a set of name value properties for representing the XML
     *         parameters
     */
    public static Properties parseParameters(final Node nParameters){
        return parseProperties(nParameters, "param");
    }

    /**
     * Parses a structure like:
     * <pre>
     * {@code
     *   <properties>
     *     <property name="a" value="1"/>
     *     <property name="b" value="2"/>
     *   </properties>
     * }
     * </pre>
     * into a set of Properties
     *
     * @param nProperties
     *            The properties Node
     * @return a set of name value properties for representing the XML
     *         properties
     */
    public static Properties parseProperties(final Node nProperties) {
        return parseProperties(nProperties, "property");
    }

    /**
     * Parses a structure like:
     * <pre>
     * {@code
     *   <features>
     *     <feature name="a" value="1"/>
     *     <feature name="b" value="2"/>
     *   </features>
     * }
     * </pre>
     * into a set of Properties
     *
     * @param nFeatures
     *            The features Node
     * @return a set of name value properties for representing the XML
     *         features
     */
    public static Properties parseFeatures(final Node nFeatures) {
        return parseProperties(nFeatures, "feature");
    }

    /**
     * Parses a structure like:
     * <pre>
     * {@code
     *   <properties>
     *     <property name="a" value="1"/>
     *     <property name="b" value="2"/>
     *   </properties>
     * }
     * </pre>
     * into a set of Properties
     *
     * @param container
     *            The container of the properties
     * @param elementName
     *            The name of the property element
     * @return a set of name value properties for representing the XML
     *         properties
     */
    private static Properties parseProperties(final Node container, final String elementName) throws IllegalArgumentException {
        final Properties properties = new Properties();

        if(container != null && container.getNodeType() == Node.ELEMENT_NODE) {
            final NodeList params = ((Element) container).getElementsByTagName(elementName);
            for(int i = 0; i < params.getLength(); i++) {
                final Element param = ((Element) params.item(i));

                final String name = param.getAttribute("name");
                final String value = param.getAttribute("value");

                if ((!name.isEmpty()) && (!value.isEmpty())) {
                    properties.setProperty(name, value);
                } else {
                    if (name.isEmpty()) {
                        throw new IllegalArgumentException("'name' attribute missing for " + elementName);
                    } else {
                        throw new IllegalArgumentException("'value' attribute missing for " + elementName);
                    }
                }
            }
        }

        return properties;
    }
}