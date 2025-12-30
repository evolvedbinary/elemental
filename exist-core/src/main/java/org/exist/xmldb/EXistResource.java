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
package org.exist.xmldb;

import java.util.Date;
import java.util.Properties;

import org.exist.security.Permission;
import org.w3c.dom.DocumentType;
import org.xml.sax.ext.LexicalHandler;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

import javax.annotation.Nullable;

/**
 * Defines additional methods implemented by XML and binary
 * resources.
 *
 * @author wolf
 */
public interface EXistResource extends Resource, AutoCloseable {

    Date getCreationTime() throws XMLDBException;

    Date getLastModificationTime() throws XMLDBException;

    Permission getPermissions() throws XMLDBException;

    /**
     * The content length if known.
     *
     * @return The content length, or -1 if not known.
     *
     * @throws XMLDBException if an error occurs whilst getting the content's length.
     */
    long getContentLength() throws XMLDBException;

    void setLexicalHandler(LexicalHandler handler);

    /**
     * Set the Internet Media Type of the resource.
     *
     * @param mediaType the Internet Media Type.
     *
     * @deprecated Use {@link #setMediaType(String)} instead.
     */
    @Deprecated
    void setMimeType(String mediaType);

    /**
     * Set the Internet Media Type of the resource.
     *
     * @param mediaType the Internet Media Type.
     */
    void setMediaType(String mediaType);

    /**
     * Get the Internet Media Type of the resource.
     *
     * @return the Internet Media Type.
     *
     * @deprecated Use {@link #getMediaType()} instead.
     */
    @Deprecated
    String getMimeType() throws XMLDBException;

    /**
     * Get the Internet Media Type of the resource.
     *
     * @return the Internet Media Type.
     */
    String getMediaType() throws XMLDBException;

    DocumentType getDocType() throws XMLDBException;

    void setDocType(DocumentType doctype) throws XMLDBException;

    void setLastModificationTime(Date lastModificationTime) throws XMLDBException;

    void freeResources() throws XMLDBException;

    void setProperties(Properties properties);

    @Nullable Properties getProperties();

    String getTypeName();

    boolean isClosed();

    @Override
    void close() throws XMLDBException;
}
