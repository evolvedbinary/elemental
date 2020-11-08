/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to Elemental by
 * Evolved Binary, for the benefit of the Elemental Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to Elemental, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in Elemental.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
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
package xyz.elemental.mediatype;

import javax.annotation.Nullable;

/**
 * Information about a Media Type (aka MIME Type)
 * and how resources of that type should be stored into
 * the database.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface MediaType {

    /**
     * Get the identifier of the Media Type.
     *
     * For example {@code application/xml}.
     *
     * @return the identifier of the Media Type
     */
    String getIdentifier();

    /**
     * Get the file extensions that are known
     * to be associated with the Media Type.
     *
     * @return the known file extensions associated with the Media Type, or null if there are none
     */
    @Nullable String[] getKnownFileExtensions();

    /**
     * Get the database storage type that should
     * be used for resources of this Media Type.
     *
     * @return the database storage type of the Media Type
     */
    StorageType getStorageType();

    // <editor-fold desc="List of common Media Type Identifiers">
    String APPLICATION_BZIP2 = "application/x-bzip2";
    String APPLICATION_EXISTDB_COLLECTION_CONFIG_XML = "application/prs.existdb.collection-config+xml";
    String APPLICATION_EXPATH_PACKAGE_ZIP = "application/prs.expath.package+zip";
    String APPLICATION_GZIP = "application/gzip";
    String APPLICATION_JAVA_ARCHIVE = "application/java-archive";
    String APPLICATION_JSON = "application/json";
    String APPLICATION_OCTET_STREAM = "application/octet-stream";
    String APPLICATION_PDF = "application/pdf";
    String APPLICATION_RELAXNG_COMPACT = "application/relax-ng-compact-syntax";
    String APPLICATION_TAR = "application/x-tar";
    String APPLICATION_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    String APPLICATION_XML = "application/xml";
    String APPLICATION_XML_DTD = "application/xml-dtd";
    String APPLICATION_XHTML_XML = "application/xhtml+xml";
    String APPLICATION_XPROC_XML = "application/xproc+xml";
    String APPLICATION_XSLT_XML = "application/xslt+xml";
    String APPLICATION_XQUERY = "application/xquery";
    String APPLICATION_XZ = "application/x-xz";
    String APPLICATION_ZIP = "application/zip";
    String APPLICATION_ZSTD = "application/zstd";

    String TEXT_CSV = "text/csv";
    String TEXT_CSV_SCHEMA = "text/csv-schema";
    String TEXT_INVISIBLE_XML = "text/prs.ixml.grammar";
    String TEXT_HTML = "text/html";
    String TEXT_JAVASCRIPT = "text/javascript";
    String TEXT_MARKDOWN = "text/markdown";
    String TEXT_PLAIN = "text/plain";
    String TEXT_URI_LIST = "text/uri-list";
    // </editor-fold>
}
