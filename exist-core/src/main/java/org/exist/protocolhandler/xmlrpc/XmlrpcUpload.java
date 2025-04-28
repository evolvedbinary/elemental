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
package org.exist.protocolhandler.xmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.exist.protocolhandler.xmldb.XmldbURL;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Write document using XML-RPC to remote database and read the data
 * from an input stream.
 * 
 * Sends a document to the server using XML-RPC. The document can be
 * either XML or non-XML (binary). Chunked means that the document is send 
 * as smaller parts to the server, the servlet glues the parts together. There
 * is no limitation on the size of the documents that can be transported.
 *
 * @author Dannes Wessels
 */
public class XmlrpcUpload {
    
    private final static Logger LOG = LogManager.getLogger(XmlrpcUpload.class);
    
    /**
     * Write data from a (input)stream to the specified XML-RPC url and leave
     * the input stream open.
     * 
     * @param xmldbURL URL pointing to location on the server.
     * @param is Document stream
     * @throws IOException When something is wrong.
     */
    public void stream(XmldbURL xmldbURL, InputStream is) throws IOException {
        LOG.debug("Begin document upload");
        try {
            // Setup xmlrpc client
            final XmlRpcClient client = new XmlRpcClient();
            final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setEncoding(UTF_8.name());
            config.setEnabledForExtensions(true);
            config.setServerURL(new URL(xmldbURL.getXmlRpcURL()));

            if(xmldbURL.hasUserInfo()) {
                config.setBasicUserName(xmldbURL.getUsername());
                config.setBasicPassword(xmldbURL.getPassword());
            }
            client.setConfig(config);

            String contentType=MimeType.BINARY_TYPE.getName();
            final MimeType mime
                    = MimeTable.getInstance().getContentTypeFor(xmldbURL.getDocumentName());
            if (mime != null){
                contentType = mime.getName();
            }
            
            // Initialize xmlrpc parameters
            final List<Object> params = new ArrayList<>(5);
            String handle=null;
            
            // Copy data from inputstream to database
            final byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                params.clear();
                if(handle!=null){
                    params.add(handle);
                }
                params.add(buf);
                params.add(len);
                handle = (String)client.execute("upload", params);
            }
            
            // All data transported, now parse data on server
            params.clear();
            params.add(handle);
            params.add(xmldbURL.getCollectionPath() );
            params.add(Boolean.TRUE);
            params.add(contentType);
            final Boolean result =(Boolean)client.execute("parseLocal", params);
            
            // Check XMLRPC result
            if(result){
                LOG.debug("Document stored.");
            } else {
                LOG.debug("Could not store document.");
                throw new IOException("Could not store document.");
            }
            
        } catch (final IOException ex) {
            LOG.debug(ex);
            throw ex;
            
        } catch (final Exception ex) {
            LOG.debug(ex);
            throw new IOException(ex.getMessage(), ex);
            
        } finally {
           LOG.debug("Finished document upload");
        }
    }

}
