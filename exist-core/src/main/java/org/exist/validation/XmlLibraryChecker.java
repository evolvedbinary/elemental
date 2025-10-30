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
package org.exist.validation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ServiceLoader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.util.ExistSAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import static org.exist.util.StringUtil.LINE_SEPARATOR;

/**
 *  Class for checking dependencies with XML libraries.
 *
 * @author <a href="mailto:adam.retter@devon.gov.uk">Adam Retter</a>
 */
public class XmlLibraryChecker {

    /**
     * Possible XML Parsers, at least one must be valid
     */
    private static final ClassVersion[] VALID_PARSERS = {
        new ClassVersion("Xerces", "Xerces-J 2.11.0", "org.apache.xerces.impl.Version.getVersion()")
    };
    
    /**
     * Possible XML Transformers, at least one must be valid
     */
    private static final ClassVersion[] VALID_TRANSFORMERS = {
        new ClassVersion("Saxon", "9.9.1", "net.sf.saxon.Version.getProductVersion()")
    };
    
    /**
     * Possible XML resolvers, at least one must be valid
     */
    private static final ClassVersion[] VALID_RESOLVERS = {
        new ClassVersion("Resolver", "XmlResolver 1.2", "org.apache.xml.resolver.Version.getVersion()"),
    };

	private static final Logger LOGGER = LogManager.getLogger(XmlLibraryChecker.class);


    /**
     *  Remove "@" from string.
     */
    private static String getClassName(final String classid) {
        final String className;
        final int lastChar = classid.lastIndexOf('@');
        if (lastChar == -1) {
            className = classid;
        } else {
            className = classid.substring(0, lastChar);
        }
        return className;
    }

    /**
     *  Determine the class that is actually used as XML parser. 
     * 
     * @return Full classname of parser.
     */
    private static String determineActualParserClass() {
        String parserClass = "<UNKNOWN>";
        try {
            final SAXParserFactory factory = ExistSAXParserFactory.getSAXParserFactory();
            final XMLReader xmlReader = factory.newSAXParser().getXMLReader();
            final String classId = xmlReader.toString();
            parserClass = getClassName(classId);
            
        } catch (final ParserConfigurationException | SAXException ex) {
            LOGGER.error(ex.getMessage());
        }
        return parserClass;
    }

    
    /**
     *  Determine the class that is actually used as XML transformer. 
     * 
     * @return Full classname of transformer.
     */
    private static String determineActualTransformerClass(){
        String transformerClass = "<UNKNOWN>";
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            final Transformer transformer = factory.newTransformer();
            final String classId = transformer.toString();
            transformerClass = getClassName(classId);

        } catch (final TransformerConfigurationException ex) {
            LOGGER.error(ex.getMessage());
        }
        return transformerClass;    
    }

    /**
     *  Determine the class that is actually used as XML resolver.
     *
     * @return Full classname of resolver.
     */
    private static String determineActualResolverClass(){
        return "org.apache.xml.resolver.Resolver";
    }

    /**
     *  Perform checks on parsers, transformers and resolvers.
     */
    public static void check() {

        final StringBuilder message = new StringBuilder();

        /*
         * Parser
         */
        final ServiceLoader<SAXParserFactory> allSax = ServiceLoader.load(SAXParserFactory.class);
        for (final SAXParserFactory sax : allSax) {
            message.append(getClassName(sax.toString()));
            message.append(" ");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Detected SAXParserFactory classes: {}", message.toString());
        }

		boolean	validParserVersionFound = false;
        boolean	validTransformerVersionFound = false;
        boolean	validResolverVersionFound = false;

        message.setLength(0);
        if (hasValidClassVersion("Parser", VALID_PARSERS, message)) {
			LOGGER.info(message.toString());
            validParserVersionFound = true;
        } else {
			LOGGER.warn(message.toString());
        }

        /*
         * Transformer
         */
        message.setLength(0);
        final ServiceLoader<TransformerFactory> allXsl = ServiceLoader.load(TransformerFactory.class);
        for (final TransformerFactory xsl : allXsl) {
            message.append(getClassName(xsl.toString()));
            message.append(" ");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Detected TransformerFactory classes: {}", message.toString());
        }

        message.setLength(0);
        if (hasValidClassVersion("Transformer", VALID_TRANSFORMERS, message)) {
            LOGGER.info(message.toString());
            validTransformerVersionFound = true;
        } else {
            LOGGER.warn(message.toString());
        }

        /*
         * Resolver
         */
        message.setLength(0);
        if (hasValidClassVersion("Resolver", VALID_RESOLVERS, message)) {
            LOGGER.info(message.toString());
            validResolverVersionFound = true;
        } else {
            LOGGER.warn(message.toString());
        }
		
		if (validParserVersionFound) {
            LOGGER.info("Using Parser: {}.", determineActualParserClass());
        } else {
            LOGGER.warn("Using Parser: {}.", determineActualParserClass());
        }
        if (validTransformerVersionFound) {
            LOGGER.info("Using Transformer: {}.", determineActualTransformerClass());
        } else {
            LOGGER.warn("Using Transformer: {}.", determineActualTransformerClass());
        }
        if (validResolverVersionFound) {
            LOGGER.info("Using Resolver: {}.", determineActualResolverClass());
		} else {
            LOGGER.warn("Using Resolver: {}.", determineActualResolverClass());
        }
    }

    /**
     *  Check if for the specified service object one of the required
     * classes is available.
     * 
     * @param type  Parser, Transformer or Resolver, used for reporting only.
     * @param validClasses Array of valid classes. 
     * @param message  Output message of detecting classes.
     * @return TRUE if valid class has been found, otherwise FALSE.
     */
    public static boolean hasValidClassVersion(final String type, final ClassVersion[] validClasses, final StringBuilder message) {
        message.append(type).append(": ");

        for (int i = 0; i < validClasses.length; i++) {
            final ClassVersion validClass = validClasses[i];

            if (i > 0) {
                message.append(' ');
            }

            message.append(validClass.getSimpleName()).append(' ');

            final String actualVersion = validClass.getActualVersion();
            if (actualVersion != null) {
                message.append("version: ").append(actualVersion);

                if (actualVersion.compareToIgnoreCase(validClass.getRequiredVersion()) >= 0) {
                    message.append('.');
                    return true;
                } else {
                    message.append(", but required version: ").append(validClass.getRequiredVersion()).append('.');
                }
                
            } else {
                message.append("Not found!");
            }
        }

        message.append(LINE_SEPARATOR).append("Warning: Failed find a valid ").append(type).append("!");

        return false;
    }

    /**
     * Checks to see if a valid XML Parser exists
     * 
     * @return boolean true indicates a valid Parser was found, false otherwise
     */
    public static boolean hasValidParser() {
        return hasValidParser(new StringBuilder());
    }

    /**
     * Checks to see if a valid XML Parser exists
     * 
     * @param message	Messages about the status of available Parser's will 
     *                  be appended to this buffer
     * 
     * @return boolean true indicates a valid Parser was found, false otherwise
     */
    public static boolean hasValidParser(final StringBuilder message) {
        return hasValidClassVersion("Parser", VALID_PARSERS, message);
    }

    /**
     * Checks to see if a valid XML Transformer exists
     * 
     * @return boolean true indicates a valid Transformer was found, 
     *         false otherwise
     */
    public static boolean hasValidTransformer() {
        return hasValidTransformer(new StringBuilder());
    }

    /**
     * Checks to see if a valid XML Transformer exists
     * 
     * @param message	Messages about the status of available Transformer's 
     *                  will be appended to this buffer
     * 
     * @return boolean true indicates a valid Transformer was found, 
     *         false otherwise
     */
    public static boolean hasValidTransformer(final StringBuilder message) {
        return hasValidClassVersion("Transformer", VALID_TRANSFORMERS, message);
    }

    /**
     * Simple class to describe a class, its required version and how to 
     * obtain the actual version 
     */
    public static class ClassVersion {

        private final String simpleName;
        private final String requiredVersion;
        private final String versionFunction;

        /**
         * Default Constructor
         * 
         * @param simpleName		The simple name for the class (just a 
         *                          descriptor really)
         * @param requiredVersion	The required version of the class
         * @param versionFunction	The function to be invoked to obtain the 
         *                          actual version of the class, must be fully 
         *                          qualified (i.e. includes the package name)
         */
        ClassVersion(final String simpleName, final String requiredVersion, final String versionFunction) {
            this.simpleName = simpleName;
            this.requiredVersion = requiredVersion;
            this.versionFunction = versionFunction;
        }

        /**
         *  @return the simple name of the class
         */
        public String getSimpleName() {
            return simpleName;
        }

        /**
         *  @return the required version of the class
         */
        public String getRequiredVersion() {
            return requiredVersion;
        }

        /**
         * Invokes the specified versionFunction using reflection to get the 
         * actual version of the class
         * 
         *  @return the actual version of the class
         */
        public String getActualVersion() {
            String actualVersion = null;

            //get the class name from the specifiec version function string
            final String versionClassName = versionFunction
                    .substring(0, versionFunction.lastIndexOf('.'));

            //get the function name from the specifiec version function string
            final String versionFunctionName = versionFunction.substring(
                    versionFunction.lastIndexOf('.') + 1, versionFunction.lastIndexOf('('));

            try {
                //get the class
                final Class<?> versionClass = Class.forName(versionClassName);

                //get the method
                final Method getVersionMethod = versionClass.getMethod(versionFunctionName, (Class[]) null);

                //invoke the method on the class
                actualVersion = (String) getVersionMethod.invoke(versionClass, (Object[]) null);
                
            } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                LOGGER.warn(ex.getMessage());
            }

            //return the actual version
            return actualVersion;
        }
    }
}
