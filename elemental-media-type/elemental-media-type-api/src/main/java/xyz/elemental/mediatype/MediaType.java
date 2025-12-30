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
    String APPLICATION_ATOM = "application/atom+xml";
    String APPLICATION_BZIP2 = "application/x-bzip2";
    String APPLICATION_DITA = "application/dita+xml";
    String APPLICATION_ELEMENTAL_DOCUMENT = "application/vnd.elemental.document";
    String APPLICATION_ELEMENTAL_DOCUMENT_BINARY = "application/vnd.elemental.document+binary";
    String APPLICATION_ELEMENTAL_DOCUMENT_XML = "application/vnd.elemental.document+xml";
    String APPLICATION_ELEMENTAL_COLLECTION = "application/vnd.elemental.collection";
    String APPLICATION_EXPATH_PACKAGE_ZIP = "application/prs.expath.package+zip";
    String APPLICATION_GML = "application/gml+xml";
    String APPLICATION_GZIP = "application/gzip";
    String APPLICATION_INVISIBLE_XML_GRAMMAR = "application/prs.invisible-xml.grammar";
    String APPLICATION_INVISIBLE_XML_GRAMMAR_XML = "application/prs.invisible-xml.grammar+xml";
    String APPLICATION_JAVA_ARCHIVE = "application/java-archive";
    String APPLICATION_JSON = "application/json";
    String APPLICATION_MADS = "application/mads+xml";
    String APPLICATION_MARC = "application/marcxml+xml";
    String APPLICATION_METS = "application/mets+xml";
    String APPLICATION_MODS = "application/mods+xml";
    String APPLICATION_NCX = "application/x-dtbncx+xml";
    String APPLICATION_OCTET_STREAM = "application/octet-stream";
    String APPLICATION_OEBPS_PACKAGE = "application/oebps-package+xml";
    String APPLICATION_OPENDOCUMENT_PRESENTATION = "application/vnd.oasis.opendocument.presentation";
    String APPLICATION_OPENDOCUMENT_TEXT = "application/vnd.oasis.opendocument.text";
    String APPLICATION_OPENDOCUMENT_SPREADSHEET = "application/vnd.oasis.opendocument.spreadsheet";
    String APPLICATION_OPENXML_PRESENTATION = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    String APPLICATION_OPENXML_SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    String APPLICATION_OPENXML_WORDPROCESSING = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    String APPLICATION_PACK200 = "application/x-java-pack200";
    String APPLICATION_PDF = "application/pdf";
    String APPLICATION_RDF_XML = "application/rdf+xml";
    String APPLICATION_RELAXNG_COMPACT = "application/relax-ng-compact-syntax";
    String APPLICATION_RSS = "application/rss+xml";
    String APPLICATION_SCHEMATRON = "application/schematron+xml";
    String APPLICATION_SRU = "application/sru+xml";
    String APPLICATION_TAR = "application/x-tar";
    String APPLICATION_TEI = "application/tei+xml";
    String APPLICATION_WSDL = "application/wsdl+xml";
    String APPLICATION_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    String APPLICATION_XMI = "application/vnd.xmi+xml";
    String APPLICATION_XML = "application/xml";
    String APPLICATION_XML_DTD = "application/xml-dtd";
    String APPLICATION_XHTML = "application/xhtml+xml";
    String APPLICATION_XPROC = "application/xproc+xml";
    String APPLICATION_XSLT = "application/xslt+xml";
    String APPLICATION_XQUERY = "application/xquery";
    String APPLICATION_XQUERY_XML = "application/xquery+xml";
    String APPLICATION_XZ = "application/x-xz";
    String APPLICATION_ZIP = "application/zip";
    String APPLICATION_ZSTD = "application/zstd";

    String IMAGE_GIF = "image/gif";
    String IMAGE_JPEG = "image/jpeg";
    String IMAGE_PNG = "image/png";
    String IMAGE_SVG = "image/svg+xml";
    String IMAGE_SVG_GZIP = "image/svg+xml";
    // See: https://github.com/w3c/svgwg/issues/701
//    String IMAGE_SVG_GZIP = "image/x.svg+gzip";

    String MULTIPART_ALTERNATIVE = "multipart/alternative";
    String MULTIPART_MIXED = "multipart/mixed";

    String TEXT_CSS = "text/css";
    String TEXT_CSV = "text/csv";
    String TEXT_CSV_SCHEMA = "text/csv-schema";
    String TEXT_HTML = "text/html";
    String TEXT_JAVASCRIPT = "text/javascript";
    String TEXT_MARKDOWN = "text/markdown";
    String TEXT_N3 = "text/n3";
    String TEXT_PLAIN = "text/plain";
    String TEXT_TURTLE = "text/turtle";
    String TEXT_URI_LIST = "text/uri-list";

    /**
     * Expect for use within the type attribute of an XML Processing Instruction
     * this is probably not what you need, and you should likely use {@link #APPLICATION_XML} instead.
     */
    @Deprecated
    String TEXT_XSL = "text/xsl";
    // </editor-fold>

    // <editor-fold desc="Deprecated Media Type Identifiers">
    String APPLICATION_EXISTDB_COLLECTION_CONFIG_XML = "application/prs.existdb.collection-config+xml";

    /**
     * Use {@link #APPLICATION_ELEMENTAL_DOCUMENT} instead.
     */
    @Deprecated
    String APPLICATION_EXISTDB_DOCUMENT = "application/vnd.existdb.document";

    /**
     * Use {@link #APPLICATION_ELEMENTAL_DOCUMENT_BINARY} instead.
     */
    @Deprecated
    String APPLICATION_EXISTDB_DOCUMENT_BINARY = "application/vnd.existdb.document+binary";

    /**
     * Use {@link #APPLICATION_ELEMENTAL_DOCUMENT_XML} instead.
     */
    @Deprecated
    String APPLICATION_EXISTDB_DOCUMENT_XML = "application/vnd.existdb.document+xml";

    /**
     * Use {@link #APPLICATION_ELEMENTAL_COLLECTION} instead.
     */
    @Deprecated
    String APPLICATION_EXISTDB_COLLECTION = "application/vnd.existdb.collection";
    // </editor-fold>
}
