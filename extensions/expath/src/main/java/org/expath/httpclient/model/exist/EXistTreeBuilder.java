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
package org.expath.httpclient.model.exist;

import org.apache.http.Header;
import org.exist.dom.QName;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.xquery.XQueryContext;
import org.expath.httpclient.HeaderSet;
import org.expath.httpclient.HttpClientException;
import org.expath.httpclient.HttpConstants;
import org.expath.httpclient.model.TreeBuilder;
import org.expath.tools.ToolsException;

import javax.xml.XMLConstants;

/**
 * @author <a href="mailto:adam@existsolutions.com">Adam Retter</a>
 */
public class EXistTreeBuilder implements TreeBuilder {

    final MemTreeBuilder builder;
    
    public EXistTreeBuilder(final XQueryContext context) {
        context.pushDocumentContext();
        builder = context.getDocumentBuilder();
        builder.startDocument();
    }

    //TODO EXPath Caller should send QName, otherwise we duplicate code and reduce reuse!
    @Override
    public void startElem(final String localname) throws ToolsException {
        final String prefix = HttpConstants.HTTP_CLIENT_NS_PREFIX;
        final String uri = HttpConstants.HTTP_CLIENT_NS_URI;
        
        builder.startElement(new QName(localname, uri, prefix), null);
    }

    @Override
    public void attribute(final String localname, final CharSequence value) throws ToolsException {
        builder.addAttribute(new QName(localname, XMLConstants.NULL_NS_URI), value.toString());
    }

    @Override
    public void startContent() throws ToolsException {
        //TODO this is not needed, it is very saxon specific
    }

    @Override
    public void endElem() throws ToolsException {
        builder.endElement();
    }
    
    public DocumentImpl close() {
        builder.endDocument();
        final DocumentImpl doc = builder.getDocument();
        builder.getContext().popDocumentContext();
        return doc;
    }

    @Override
    public void outputHeaders(HeaderSet headers)
            throws HttpClientException
    {
        for ( Header h : headers ) {
            assert h.getName() != null : "Header name cannot be null";
            String name = h.getName().toLowerCase();
            try {
                startElem("header");
                attribute("name", name);
                attribute("value", h.getValue());
                //startContent();
                endElem();
            }
            catch ( ToolsException ex ) {
                throw new HttpClientException("Error building the header " + name, ex);
            }
        }
    }

}