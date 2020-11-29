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
package org.exist.util;


import xyz.elemental.mediatype.MediaType;
import xyz.elemental.mediatype.MediaTypeResolver;
import xyz.elemental.mediatype.StorageType;

import javax.annotation.Nullable;

/**
 * @deprecated Use {@link xyz.elemental.mediatype.MediaType} instead.
 */
@Deprecated
public class MimeType {

    /**
     * Use {@link StorageType#XML} instead.
     */
    @Deprecated
    public final static int XML = 0;

    /**
     * Use {@link StorageType#BINARY} instead.
     */
    @Deprecated
    public final static int BINARY = 1;

    /**
     * Use {@link MediaTypeResolver#forUnknown()} instead.
     */
    @Deprecated
    public final static MimeType BINARY_TYPE =
        new MimeType(MediaType.APPLICATION_OCTET_STREAM, BINARY);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.APPLICATION_XML)} instead.
     */
    @Deprecated
    public final static MimeType XML_TYPE =
        new MimeType(MediaType.APPLICATION_XML, XML);

    @Deprecated
    public final static MimeType XML_CONTENT_TYPE =
        new MimeType(MediaType.APPLICATION_XML + "; charset=UTF-8", XML);

    @Deprecated
    public final static MimeType XML_LEGACY_TYPE =
    	new MimeType("text/xml", XML);

    @Deprecated
    public final static MimeType XSL_TYPE =
        new MimeType("text/xsl", XML);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.APPLICATION_XSLT_XML)} instead.
     */
    @Deprecated
    public final static MimeType XSLT_TYPE =
        new MimeType(MediaType.APPLICATION_XSLT, XML);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.APPLICATION_XQUERY)} instead.
     */
    @Deprecated
    public final static MimeType XQUERY_TYPE =
        new MimeType(MediaType.APPLICATION_XQUERY, BINARY);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.APPLICATION_XPROC_XML)} instead.
     */
    @Deprecated
    public final static MimeType XPROC_TYPE =
        new MimeType(MediaType.APPLICATION_XPROC, XML);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.TEXT_CSS)} instead.
     */
    @Deprecated
    public final static MimeType CSS_TYPE =
        new MimeType(MediaType.TEXT_CSS, BINARY);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.TEXT_HTML)} instead.
     */
    @Deprecated
    public final static MimeType HTML_TYPE =
        new MimeType(MediaType.TEXT_HTML, BINARY);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.TEXT_PLAIN)} instead.
     */
    @Deprecated
    public final static MimeType TEXT_TYPE =
        new MimeType(MediaType.TEXT_PLAIN, BINARY);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.APPLICATION_WWW_FORM_URLENCODED)} instead.
     */
    @Deprecated
    public final static MimeType URL_ENCODED_TYPE =
    	new MimeType(MediaType.APPLICATION_OCTET_STREAM, BINARY);

    /**
     * @deprecated Use {@code MediaTypeResolver#fromString(MediaType.APPLICATION_EXPATH_PACKAGE_ZIP)} instead.
     */
    @Deprecated
    public final static MimeType EXPATH_PKG_TYPE =
        new MimeType(MediaType.APPLICATION_EXPATH_PACKAGE_ZIP, BINARY);


    private final String name;
    private String description;
    private final int type;
 
    public MimeType(final String name, final int type) {
        this.name = name;
        this.type = type;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getName() {
        return name;
    }
    public int getType() {
        return type;
    }
    
    public String getXMLDBType() {
        return isXMLType() ? "XMLResource" : "BinaryResource";
    }
    
    public boolean isXMLType() {
        return type == XML;
    }

    @Override
    public String toString() {
        return name + ": " + description;
    }

    /**
     * Get this as an Internet Media Type.
     *
     * @return the Internet Media Type.
     */
    public MediaType toMediaType() {
        return new MediaTypeAdapter();
    }

    public class MediaTypeAdapter implements MediaType {
        @Override
        public String getIdentifier() {
            return name;
        }

        @Nullable
        @Override
        public String[] getKnownFileExtensions() {
            return null;
        }

        @Override
        public StorageType getStorageType() {
            if (isXMLType()) {
                return StorageType.XML;
            } else {
                return StorageType.BINARY;
            }
        }
    }
}