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
 */
package org.exist.resolver;

import org.apache.xerces.impl.dtd.XMLDTDDescription;
import org.apache.xerces.impl.xs.XSDDescription;
import org.apache.xerces.util.SAXInputSource;
import org.apache.xerces.util.XMLEntityDescriptionImpl;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.XMLSchemaDescription;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.exist.util.XMLReaderObjectFactory;
import org.xml.sax.*;
import org.xmlresolver.Resolver;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Adapts an {@link org.xmlresolver.Resolver} for use
 * with Xerces SAX Parser by implementing {@link org.apache.xerces.xni.parser.XMLEntityResolver}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XercesXmlResolverAdapter implements XMLEntityResolver {
    private final Resolver resolver;

    public XercesXmlResolverAdapter(final Resolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public XMLInputSource resolveEntity(final XMLResourceIdentifier xmlResourceIdentifier) throws XNIException, IOException {

        try {
            // get the name
            final String name;
            if (xmlResourceIdentifier instanceof XSDDescription) {
                final QName triggeringComponent = ((XSDDescription) xmlResourceIdentifier).getTriggeringComponent();
                name = triggeringComponent != null ? triggeringComponent.localpart : null;
            } else if (xmlResourceIdentifier instanceof XMLSchemaDescription) {
                final QName triggeringComponent = ((XMLSchemaDescription) xmlResourceIdentifier).getTriggeringComponent();
                name = triggeringComponent != null ? triggeringComponent.localpart : null;
            } else if (xmlResourceIdentifier instanceof XMLEntityDescriptionImpl) {
                name = ((XMLEntityDescriptionImpl)xmlResourceIdentifier).getEntityName();
            } else if (xmlResourceIdentifier instanceof XMLDTDDescription) {
                name = ((XMLDTDDescription)xmlResourceIdentifier).getRootName();
            } else {
                name = null;
            }

            // get the systemId
            final String systemId;
            if (xmlResourceIdentifier.getExpandedSystemId() !=  null) {
                systemId = xmlResourceIdentifier.getExpandedSystemId();
            } else {
                systemId = xmlResourceIdentifier.getNamespace();
            }

//            System.out.println(String.format("xri=(name=%s publicId=%s baseSystemId=%s systemId=%s)", name, xmlResourceIdentifier.getPublicId(), xmlResourceIdentifier.getBaseSystemId(), systemId));

            // resolve the entity via an org.xmlresolver.Resolver
            final InputSource src = resolver.resolveEntity(name, xmlResourceIdentifier.getPublicId(), xmlResourceIdentifier.getBaseSystemId(), systemId);
            if (src == null) {
                return null;
            }

            return new SAXInputSource(src);

        } catch (final SAXException e) {
            throw new XNIException(e);
        }
    }

    /**
     * Wraps the {@code resolver} in a XercesXMLResolverAdapter
     * and then sets it as the property {@code http://apache.org/xml/properties/internal/entity-resolver}
     * on the {@code xmlReader}.
     *
     * @param xmlReader the Xerces XML Reader
     * @param resolver the resolver, or null to unset the property
     *
     * @throws SAXNotSupportedException if the property is not supported by the XMLReader
     * @throws SAXNotRecognizedException if the property is not recognised by the XMLReader
     */
    public static void setXmlReaderEntityResolver(final XMLReader xmlReader, @Nullable final Resolver resolver) throws SAXNotSupportedException, SAXNotRecognizedException {
        final XMLEntityResolver xmlEntityResolver = resolver != null ? new XercesXmlResolverAdapter(resolver) : null;
        setXmlReaderEntityResolver(xmlReader, xmlEntityResolver);
    }

    /**
     * Sets the {@code xmlEntityResolver} as the property {@code http://apache.org/xml/properties/internal/entity-resolver}
     * on the {@code xmlReader}.
     *
     * @param xmlReader the Xerces XML Reader
     * @param xmlEntityResolver the resolver, or null to unset the resolver
     *
     * @throws SAXNotSupportedException if the property is not supported by the XMLReader
     * @throws SAXNotRecognizedException if the property is not recognised by the XMLReader
     */
    public static void setXmlReaderEntityResolver(final XMLReader xmlReader, @Nullable final XMLEntityResolver xmlEntityResolver) throws SAXNotSupportedException, SAXNotRecognizedException {
        xmlReader.setProperty(XMLReaderObjectFactory.APACHE_PROPERTIES_INTERNAL_ENTITYRESOLVER, xmlEntityResolver);
    }

    /**
     * Get the underlying resolver.
     *
     * @return the underlying resolver.
     */
    public Resolver getResolver() {
        return resolver;
    }
}
