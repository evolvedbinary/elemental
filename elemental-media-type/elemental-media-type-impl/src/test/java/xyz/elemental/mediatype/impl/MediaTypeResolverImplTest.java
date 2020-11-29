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
package xyz.elemental.mediatype.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.elemental.mediatype.MediaType;
import xyz.elemental.mediatype.MediaTypeResolver;
import xyz.elemental.mediatype.StorageType;

import javax.annotation.Nullable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MediaTypeResolverImplTest {

    // TODO(AR) if an explicit content type is provided, e.g. HTTP PUT, store the mime type with the document data??? what if its not provided, lookup and store, or lookup on retrieval?

    private static MediaTypeMapper MEDIA_TYPE_MAPPER = null;
    private static MediaTypeResolver DEFAULT_MEDIA_RESOLVER = null;
    private static MediaTypeResolver APPLICATION_MEDIA_RESOLVER = null;

    @BeforeAll
    public static void setupMediaResolvers() throws URISyntaxException {
        @Nullable final URL mediaTypeMappings = MediaTypeResolverImplTest.class.getResource("media-type-mappings.xml");
        assertNotNull(mediaTypeMappings);
        final Path configDir = Paths.get(mediaTypeMappings.toURI()).getParent();
        MEDIA_TYPE_MAPPER = new MediaTypeMapper(configDir);

        final ApplicationMimetypesFileTypeMap defaultMimetypesFileTypeMap = new ApplicationMimetypesFileTypeMap((Path[]) null);
        DEFAULT_MEDIA_RESOLVER = new MediaTypeResolverImpl(defaultMimetypesFileTypeMap, MEDIA_TYPE_MAPPER);

        APPLICATION_MEDIA_RESOLVER = new MediaTypeResolverFactoryImpl().newMediaTypeResolver(configDir);
    }

    // <editor-fold desc="Media Type definitions which are consistent across resolvers">
    @Test
    public void allResolveAtomExtension() {
        assertAllResolveFromFileName("something.atom", MediaType.APPLICATION_ATOM, new String[] {"atom"}, StorageType.XML);
    }

    @Test
    public void allResolveCsvExtension() {
        assertAllResolveFromFileName("something.csv", MediaType.TEXT_CSV, new String[] {"csv"}, StorageType.BINARY);
    }

    @Test
    public void allResolveDocxExtension() {
        assertAllResolveFromFileName("something.docx", MediaType.APPLICATION_OPENXML_WORDPROCESSING, new String[] {"docx"}, StorageType.XML);
    }

    @Test
    public void allResolveDtdExtension() {
        assertAllResolveFromFileName("something.dtd", MediaType.APPLICATION_XML_DTD, new String[] {"dtd"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGifExtension() {
        assertAllResolveFromFileName("something.gif", MediaType.IMAGE_GIF, new String[] {"gif"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGmlExtension() {
        assertAllResolveFromFileName("something.gml", MediaType.APPLICATION_GML, new String[] {"gml"}, StorageType.XML);
    }

    @Test
    public void allResolveHtmExtension() {
        assertAllResolveFromFileName("something.htm", MediaType.TEXT_HTML, new String[] {"htm", "html"}, StorageType.BINARY);
    }

    @Test
    public void allResolveHtmlExtension() {
        assertAllResolveFromFileName("something.html", MediaType.TEXT_HTML, new String[] {"htm", "html"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJpegExtension() {
        assertAllResolveFromFileName("something.jpeg", MediaType.IMAGE_JPEG, new String[] {"jpe", "jpg", "jpeg"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJpgExtension() {
        assertAllResolveFromFileName("something.jpg", MediaType.IMAGE_JPEG, new String[] {"jpe", "jpg", "jpeg"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsExtension() {
        assertAllResolveFromFileName("something.js", MediaType.TEXT_JAVASCRIPT, new String[] {"js", "mjs"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsonExtension() {
        assertAllResolveFromFileName("something.json", MediaType.APPLICATION_JSON, new String[] {"json"}, StorageType.BINARY);
    }

    @Test
    public void allResolveMadsExtension() {
        assertAllResolveFromFileName("something.mads", MediaType.APPLICATION_MADS, new String[] {"mads"}, StorageType.XML);
    }

    @Test
    public void allResolveMetsExtension() {
        assertAllResolveFromFileName("something.mets", MediaType.APPLICATION_METS, new String[] {"mets"}, StorageType.XML);
    }

    @Test
    public void allResolveModsExtension() {
        assertAllResolveFromFileName("something.mods", MediaType.APPLICATION_MODS, new String[] {"mods"}, StorageType.XML);
    }

    @Test
    public void allResolveMrcxExtension() {
        assertAllResolveFromFileName("something.mrcx", MediaType.APPLICATION_MARC, new String[] {"mrcx"}, StorageType.XML);
    }

    @Test
    public void allResolveN3Extension() {
        assertAllResolveFromFileName("something.n3", MediaType.TEXT_N3, new String[] {"n3"}, StorageType.BINARY);
    }

    @Test
    public void allResolveNcxExtension() {
        assertAllResolveFromFileName("something.ncx", MediaType.APPLICATION_NCX, new String[] {"ncx"}, StorageType.XML);
    }

    @Test
    public void allResolveOdtExtension() {
        assertAllResolveFromFileName("something.odt", MediaType.APPLICATION_OPENDOCUMENT_TEXT, new String[] {"odt"}, StorageType.XML);
    }

    @Test
    public void allResolveOdpExtension() {
        assertAllResolveFromFileName("something.odp", MediaType.APPLICATION_OPENDOCUMENT_PRESENTATION, new String[] {"odp"}, StorageType.XML);
    }

    @Test
    public void allResolveOdsExtension() {
        assertAllResolveFromFileName("something.ods", MediaType.APPLICATION_OPENDOCUMENT_SPREADSHEET, new String[] {"ods"}, StorageType.XML);
    }

    @Test
    public void allResolveOpfExtension() {
        assertAllResolveFromFileName("something.opf", MediaType.APPLICATION_OEBPS_PACKAGE, new String[] {"opf"}, StorageType.XML);
    }

    @Test
    public void allResolvePngExtension() {
        assertAllResolveFromFileName("something.png", MediaType.IMAGE_PNG, new String[] {"png"}, StorageType.BINARY);
    }

    @Test
    public void allResolvePptxExtension() {
        assertAllResolveFromFileName("something.pptx", MediaType.APPLICATION_OPENXML_PRESENTATION, new String[] {"pptx"}, StorageType.XML);
    }

    @Test
    public void allResolveRncExtension() {
        assertAllResolveFromFileName("something.rnc", MediaType.APPLICATION_RELAXNG_COMPACT, new String[] {"rnc"}, StorageType.BINARY);
    }

    @Test
    public void allResolveRssExtension() {
        assertAllResolveFromFileName("something.rss", MediaType.APPLICATION_RSS, new String[] {"rss"}, StorageType.XML);
    }

    @Test
    public void allResolveSruExtension() {
        assertAllResolveFromFileName("something.sru", MediaType.APPLICATION_SRU, new String[] {"sru"}, StorageType.XML);
    }

    @Test
    public void allResolveTtlExtension() {
        assertAllResolveFromFileName("something.ttl", MediaType.TEXT_TURTLE, new String[] {"ttl"}, StorageType.BINARY);
    }

    @Test
    public void allResolveTxtExtension() {
        assertAllResolveFromFileName("something.txt", MediaType.TEXT_PLAIN, new String[] {"txt", "def", "log", "in", "conf", "text", "list"}, StorageType.BINARY);
    }

    @Test
    public void allResolveWsdlExtension() {
        assertAllResolveFromFileName("something.wsdl", MediaType.APPLICATION_WSDL, new String[] {"wsdl"}, StorageType.XML);
    }

    @Test
    public void allResolveXhtExtension() {
        assertAllResolveFromFileName("something.xht", MediaType.APPLICATION_XHTML, new String[] {"xht", "xhtml"}, StorageType.XML);
    }

    @Test
    public void allResolveXhtmlExtension() {
        assertAllResolveFromFileName("something.xhtml", MediaType.APPLICATION_XHTML, new String[] {"xht", "xhtml"}, StorageType.XML);
    }

    @Test
    public void allResolveXlsxExtension() {
        assertAllResolveFromFileName("something.xlsx", MediaType.APPLICATION_OPENXML_SPREADSHEET, new String[] {"xlsx"}, StorageType.XML);
    }

    @Test
    public void allResolveXplExtension() {
        assertAllResolveFromFileName("something.xpl", MediaType.APPLICATION_XPROC, new String[] {"xpl"}, StorageType.XML);
    }

    @Test
    public void allResolveXsltExtension() {
        assertAllResolveFromFileName("something.xslt", MediaType.APPLICATION_XSLT, new String[] {"xslt"}, StorageType.XML);
    }

    @Test
    public void allResolveAtomIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_ATOM, new String[] {"atom"}, StorageType.XML);
    }

    @Test
    public void allResolveCsvIdentifier() {
        assertAllResolveFromIdentifier(MediaType.TEXT_CSV, new String[] {"csv"}, StorageType.BINARY);
    }

    @Test
    public void allResolveDocxIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_OPENXML_WORDPROCESSING, new String[] {"docx"}, StorageType.XML);
    }

    @Test
    public void allResolveDtdIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_XML_DTD, new String[] {"dtd"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGifIdentifier() {
        assertAllResolveFromIdentifier(MediaType.IMAGE_GIF, new String[] {"gif"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGmlIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_GML, new String[] {"gml"}, StorageType.XML);
    }

    @Test
    public void allResolveHtmlIdentifier() {
        assertAllResolveFromIdentifier(MediaType.TEXT_HTML, new String[] {"htm", "html"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJpegIdentifier() {
        assertAllResolveFromIdentifier(MediaType.IMAGE_JPEG, new String[] {"jpe", "jpg", "jpeg"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsIdentifier() {
        assertAllResolveFromIdentifier(MediaType.TEXT_JAVASCRIPT, new String[] {"js", "mjs"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsonIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_JSON, new String[] {"json"}, StorageType.BINARY);
    }

    @Test
    public void allResolveMadsIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_MADS, new String[] {"mads"}, StorageType.XML);
    }

    @Test
    public void allResolveMetsIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_METS, new String[] {"mets"}, StorageType.XML);
    }

    @Test
    public void allResolveModsIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_MODS, new String[] {"mods"}, StorageType.XML);
    }

    @Test
    public void allResolveMrcxIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_MARC, new String[] {"mrcx"}, StorageType.XML);
    }

    @Test
    public void allResolveN3Identifier() {
        assertAllResolveFromIdentifier(MediaType.TEXT_N3, new String[] {"n3"}, StorageType.BINARY);
    }

    @Test
    public void allResolveNcxIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_NCX, new String[] {"ncx"}, StorageType.XML);
    }

    @Test
    public void allResolveOdtIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_OPENDOCUMENT_TEXT, new String[] {"odt"}, StorageType.XML);
    }

    @Test
    public void allResolveOdpIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_OPENDOCUMENT_PRESENTATION, new String[] {"odp"}, StorageType.XML);
    }

    @Test
    public void allResolveOdsIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_OPENDOCUMENT_SPREADSHEET, new String[] {"ods"}, StorageType.XML);
    }

    @Test
    public void allResolveOpfIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_OEBPS_PACKAGE, new String[] {"opf"}, StorageType.XML);
    }

    @Test
    public void allResolvePngIdentifier() {
        assertAllResolveFromIdentifier(MediaType.IMAGE_PNG, new String[] {"png"}, StorageType.BINARY);
    }

    @Test
    public void allResolvePptxIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_OPENXML_PRESENTATION, new String[] {"pptx"}, StorageType.XML);
    }

    @Test
    public void allResolveRncIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_RELAXNG_COMPACT, new String[] {"rnc"}, StorageType.BINARY);
    }

    @Test
    public void allResolveRssIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_RSS, new String[] {"rss"}, StorageType.XML);
    }

    @Test
    public void allResolveSruIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_SRU, new String[] {"sru"}, StorageType.XML);
    }

    @Test
    public void allResolveTtlIdentifier() {
        assertAllResolveFromIdentifier(MediaType.TEXT_TURTLE, new String[] {"ttl"}, StorageType.BINARY);
    }

    @Test
    public void allResolveTxtIdentifier() {
        assertAllResolveFromIdentifier(MediaType.TEXT_PLAIN, new String[] {"txt", "def", "log", "in", "conf", "text", "list"}, StorageType.BINARY);
    }

    @Test
    public void allResolveWsdlIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_WSDL, new String[] {"wsdl"}, StorageType.XML);
    }

    @Test
    public void allResolveXhtmlIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_XHTML, new String[] {"xht", "xhtml"}, StorageType.XML);
    }

    @Test
    public void allResolveXlsxIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_OPENXML_SPREADSHEET, new String[] {"xlsx"}, StorageType.XML);
    }

    @Test
    public void allResolveXplIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_XPROC, new String[] {"xpl"}, StorageType.XML);
    }

    @Test
    public void allResolveXsltIdentifier() {
        assertAllResolveFromIdentifier(MediaType.APPLICATION_XSLT, new String[] {"xslt"}, StorageType.XML);
    }
    // </editor-fold>


    // <editor-fold desc="Media Type definitions which are default only (e.g. sourced from Apache HTTPD)">
    @Test
    public void defaultResolveRdfExtension() {
        assertDefaultResolveFromFileName("something.rdf", MediaType.APPLICATION_RDF_XML, new String[] {"rdf"}, StorageType.XML);
    }

    /**
     * The Media Type for SVGZ is horribly broken, see: <a href="https://github.com/w3c/svgwg/issues/701">w3c/svgwg/issues/701</a>
     * So it is overridden in the application's own <code>mime.types</code> file,
     * see {@link #applicationResolveSvgExtension()}.
     */
    @Test
    public void defaultResolveSvgExtension() {
        assertDefaultResolveFromFileName("something.svg", MediaType.IMAGE_SVG, new String[] {"svg", "svgz"}, StorageType.XML);
    }

    /**
     * The Media Type for SVGZ is horribly broken, see:  <a href="https://github.com/w3c/svgwg/issues/701">w3c/svgwg/issues/701</a>
     * So it is overridden in the application's own <code>mime.types</code> file,
     * see {@link #applicationResolveSvgzExtension()}.
     */
    @Test
    public void defaultResolveSvgzExtension() {
        assertDefaultResolveFromFileName("something.svgz", MediaType.IMAGE_SVG, new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void defaultResolveTeiExtension() {
        assertDefaultResolveFromFileName("something.tei", MediaType.APPLICATION_TEI, new String[] {"tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void defaultResolveTeicorpusExtension() {
        assertDefaultResolveFromFileName("something.teicorpus", MediaType.APPLICATION_TEI, new String[] {"tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void defaultResolveXmlExtension() {
        assertDefaultResolveFromFileName("something.xml", MediaType.APPLICATION_XML, new String[] {"xsl", "xml"}, StorageType.XML);
    }

    @Test
    public void defaultResolveXslExtension() {
        assertDefaultResolveFromFileName("something.xsl", MediaType.APPLICATION_XML, new String[] {"xsl", "xml"}, StorageType.XML);
    }

    @Test
    public void defaultResolveRdfIdentifier() {
        assertDefaultResolveFromIdentifier(MediaType.APPLICATION_RDF_XML, new String[] {"rdf"}, StorageType.XML);
    }

    /**
     * The Media Type for SVGZ is horribly broken, see: <a href="https://github.com/w3c/svgwg/issues/701">w3c/svgwg/issues/701</a>
     * So it is overridden in the application's own <code>mime.types</code> file.
     */
    @Test
    public void defaultResolveSvgIdentifier() {
        assertDefaultResolveFromIdentifier(MediaType.IMAGE_SVG, new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void defaultResolveTeiIdentifier() {
        assertDefaultResolveFromIdentifier(MediaType.APPLICATION_TEI, new String[] {"tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void defaultResolveXmlIdentifier() {
        assertDefaultResolveFromIdentifier(MediaType.APPLICATION_XML, new String[] {"xsl", "xml"}, StorageType.XML);
    }
    // </editor-fold>


    // <editor-fold desc="Media Type definitions which are Application (e.g. FusionDB) specific">
    @Test
    public void applicationResolveDitaExtension() {
        assertApplicationResolveFromFileName("something.dita",MediaType.APPLICATION_DITA, new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveDitamapExtension() {
        assertApplicationResolveFromFileName("something.ditamap",MediaType.APPLICATION_DITA, new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveDitavalExtension() {
        assertApplicationResolveFromFileName("something.ditaval",MediaType.APPLICATION_DITA, new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveFoExtension() {
        assertApplicationResolveFromFileName("something.fo", MediaType.APPLICATION_XML,  new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveNvdlExtension() {
        assertApplicationResolveFromFileName("something.nvdl", MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveOddExtension() {
        assertApplicationResolveFromFileName("something.odd", MediaType.APPLICATION_TEI, new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveOwlExtension() {
        assertApplicationResolveFromFileName("something.owl", MediaType.APPLICATION_RDF_XML, new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveMdExtension() {
        assertApplicationResolveFromFileName("something.md", MediaType.TEXT_MARKDOWN, new String[] {"md"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveRdfExtension() {
        assertApplicationResolveFromFileName("something.rdf", MediaType.APPLICATION_RDF_XML, new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveRngExtension() {
        assertApplicationResolveFromFileName("something.rng", MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSchExtension() {
        assertApplicationResolveFromFileName("something.sch", MediaType.APPLICATION_SCHEMATRON, new String[] {"sch"}, StorageType.XML);
    }

    @Test
    public void applicationResolveStxExtension() {
        assertApplicationResolveFromFileName("something.stx", MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgExtension() {
        assertApplicationResolveFromFileName("something.svg", MediaType.IMAGE_SVG, new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgzExtension() {
        assertApplicationResolveFromFileName("something.svgz", MediaType.IMAGE_SVG_GZIP, new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void applicationResolveTeiExtension() {
        assertApplicationResolveFromFileName("something.tei", MediaType.APPLICATION_TEI, new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveTeicorpusExtension() {
        assertApplicationResolveFromFileName("something.teicorpus", MediaType.APPLICATION_TEI, new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXarExtension() {
        assertApplicationResolveFromFileName("something.xar", MediaType.APPLICATION_EXPATH_PACKAGE_ZIP, new String[] {"xar"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXconfExtension() {
        assertApplicationResolveFromFileName("something.xconf", MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmiExtension() {
        assertApplicationResolveFromFileName("something.xmi", MediaType.APPLICATION_XMI, new String[] {"xmi"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmlExtension() {
        assertApplicationResolveFromFileName("something.xml", MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmpExtension() {
        assertApplicationResolveFromFileName("something.xmp", MediaType.APPLICATION_RDF_XML, new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXqExtension() {
        assertApplicationResolveFromFileName("something.xq", MediaType.APPLICATION_XQUERY, new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqlExtension() {
        assertApplicationResolveFromFileName("something.xql", MediaType.APPLICATION_XQUERY, new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqmExtension() {
        assertApplicationResolveFromFileName("something.xqm", MediaType.APPLICATION_XQUERY, new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqueryExtension() {
        assertApplicationResolveFromFileName("something.xquery", MediaType.APPLICATION_XQUERY, new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqwsExtension() {
        assertApplicationResolveFromFileName("something.xqws", MediaType.APPLICATION_XQUERY, new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqxExtension() {
        assertApplicationResolveFromFileName("something.xqx", MediaType.APPLICATION_XQUERY_XML, new String[] {"xqx"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXqyExtension() {
        assertApplicationResolveFromFileName("something.xqy", MediaType.APPLICATION_XQUERY, new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXsdExtension() {
        assertApplicationResolveFromFileName("something.xsd", MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXslExtension() {
        assertApplicationResolveFromFileName("something.xsl", MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveDitaIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_DITA, new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveMdIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.TEXT_MARKDOWN, new String[] {"md"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveRdfIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_RDF_XML, new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSchIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_SCHEMATRON, new String[] {"sch"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.IMAGE_SVG, new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgzIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.IMAGE_SVG_GZIP, new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void applicationResolveTeiIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_TEI, new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmiIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_XMI, new String[] {"xmi"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmlIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_XML, new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXqueryIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_XQUERY, new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqueryXIdentifier() {
        assertApplicationResolveFromIdentifier(MediaType.APPLICATION_XQUERY_XML, new String[] {"xqx"}, StorageType.XML);
    }
    // </editor-fold>


    /**
     * Check that multiple levels of mime.types files
     * yield correct lookups via. both
     * ApplicationMimetypesFileTypeMap and MediaTypeResolverImpl.
     */
    @Test
    public void resolveFromCorrectLevel() throws URISyntaxException {
        @Nullable final URL defaultApplicationMimeTypes = MediaTypeResolverImplTest.class.getResource("mime.types");
        assertNotNull(defaultApplicationMimeTypes);
        final Path defaultApplicationTypesConfigDir = Paths.get(defaultApplicationMimeTypes.toURI()).getParent();
        final String packageNamePath = MediaTypeResolverImplTest.class.getPackage().getName().replace('.', '/');

        @Nullable final URL moreSpecificMimeTypes = MediaTypeResolverImplTest.class.getResource("/" + packageNamePath + "/test/levels/mime.types");
        assertNotNull(moreSpecificMimeTypes);
        final Path moreSpecificApplicationTypesConfigDir = Paths.get(moreSpecificMimeTypes.toURI()).getParent();

        // NOTE(AR) moreSpecificApplicationTypesConfigDir is provided first so that it has highest priority
        final ApplicationMimetypesFileTypeMap mimetypesFileTypeMap = new ApplicationMimetypesFileTypeMap(
            moreSpecificApplicationTypesConfigDir,
            defaultApplicationTypesConfigDir);

        assertEquals("test/extensible-markup-language", mimetypesFileTypeMap.getContentType("something.xadam"));
        assertEquals("test/prs.existdb.collection-config+xml", mimetypesFileTypeMap.getContentType("something.xconf"));
        assertEquals("test/extensible-markup-language", mimetypesFileTypeMap.getContentType("something.xml"));
        assertEquals(MediaType.APPLICATION_XML, mimetypesFileTypeMap.getContentType("something.xsd"));
        assertEquals("test/x.xsl+xml", mimetypesFileTypeMap.getContentType("something.xsl"));

        final MediaTypeResolverImpl specificMediaTypeResolver = new MediaTypeResolverImpl(mimetypesFileTypeMap, MEDIA_TYPE_MAPPER);

        assertResolveFromFileName(specificMediaTypeResolver, "something.xadam", "test/extensible-markup-language", new String[] {"xml", "xadam"}, StorageType.BINARY);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xconf", "test/prs.existdb.collection-config+xml", new String[] {"xconf"}, StorageType.XML);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xml", "test/extensible-markup-language", new String[] {"xml", "xadam"}, StorageType.BINARY);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xsd", MediaType.APPLICATION_XML, new String[] {"nvdl", "stx", "xsd", "fo", "rng"}, StorageType.XML);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xsl", "test/x.xsl+xml", new String[] {"xsl"}, StorageType.XML);

        assertResolveFromIdentifier(specificMediaTypeResolver, "test/extensible-markup-language", new String[] { "xml", "xadam" }, StorageType.BINARY);
        assertResolveFromIdentifier(specificMediaTypeResolver, "test/prs.existdb.collection-config+xml", new String[] {"xconf"}, StorageType.XML);
        assertResolveFromIdentifier(specificMediaTypeResolver, MediaType.APPLICATION_XML, new String[] {"nvdl", "stx", "xsd", "fo", "rng"}, StorageType.XML);
        assertResolveFromIdentifier(specificMediaTypeResolver, "test/x.xsl+xml", new String[] {"xsl"}, StorageType.XML);
    }

    // <editor-fold desc="Checks for looking up unknown/invalid media types">
    @Test
    public void allResolveNonExistentExtension() {
        final String fileName = "something.nonexistent";

        final MediaTypeResolver[] allResolvers = {
                DEFAULT_MEDIA_RESOLVER,
                APPLICATION_MEDIA_RESOLVER
        };

        for (final MediaTypeResolver resolver : allResolvers) {
            assertNotNull(resolver);

            @Nullable MediaType mediaType = resolver.fromFileName(fileName);
            assertNull(mediaType);
            mediaType = resolver.fromFileName(Paths.get(fileName));
            assertNull(mediaType);
        }
    }

    @Test
    public void allResolveNonExistentIdentifier() {
        final String identifier = "non/existent";

        final MediaTypeResolver[] allResolvers = {
                DEFAULT_MEDIA_RESOLVER,
                APPLICATION_MEDIA_RESOLVER
        };

        for (final MediaTypeResolver resolver : allResolvers) {
            assertNotNull(resolver);

            @Nullable MediaType mediaType = resolver.fromString(identifier);
            assertNull(mediaType);
        }
    }
    // </editor-fold>

    private void assertAllResolveFromFileName(final String fileName, final String expectedIdentifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        assertDefaultResolveFromFileName(fileName, expectedIdentifier, expectedExtensions, expectedStorageType);
        assertApplicationResolveFromFileName(fileName, expectedIdentifier, expectedExtensions, expectedStorageType);
    }

    private void assertDefaultResolveFromFileName(final String fileName, final String expectedIdentifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        assertNotNull(DEFAULT_MEDIA_RESOLVER);
        assertResolveFromFileName(DEFAULT_MEDIA_RESOLVER, fileName, expectedIdentifier, expectedExtensions, expectedStorageType);
    }

    private void assertApplicationResolveFromFileName(final String fileName, final String expectedIdentifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        assertNotNull(APPLICATION_MEDIA_RESOLVER);
        assertResolveFromFileName(APPLICATION_MEDIA_RESOLVER, fileName, expectedIdentifier, expectedExtensions, expectedStorageType);
    }

    private void assertResolveFromFileName(final MediaTypeResolver mediaTypeResolver, final String fileName, final String expectedIdentifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        // by String
        @Nullable MediaType mediaType = mediaTypeResolver.fromFileName(fileName);
        assertNotNull(mediaType);
        assertEquals(expectedIdentifier, mediaType.getIdentifier());
        assertArrayAnyOrderEquals(expectedExtensions, mediaType.getKnownFileExtensions());
        assertEquals(expectedStorageType, mediaType.getStorageType());

        // by Path
        mediaType = mediaTypeResolver.fromFileName(Paths.get(fileName));
        assertNotNull(mediaType);
        assertEquals(expectedIdentifier, mediaType.getIdentifier());
        assertArrayAnyOrderEquals(expectedExtensions, mediaType.getKnownFileExtensions());
        assertEquals(expectedStorageType, mediaType.getStorageType());
    }

    private void assertAllResolveFromIdentifier(final String identifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        assertDefaultResolveFromIdentifier(identifier, expectedExtensions, expectedStorageType);
        assertApplicationResolveFromIdentifier(identifier, expectedExtensions, expectedStorageType);
    }

    private void assertDefaultResolveFromIdentifier(final String identifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        assertNotNull(DEFAULT_MEDIA_RESOLVER);
        assertResolveFromIdentifier(DEFAULT_MEDIA_RESOLVER, identifier, expectedExtensions, expectedStorageType);
    }

    private void assertApplicationResolveFromIdentifier(final String identifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        assertNotNull(APPLICATION_MEDIA_RESOLVER);
        assertResolveFromIdentifier(APPLICATION_MEDIA_RESOLVER,  identifier, expectedExtensions, expectedStorageType);
    }

    private void assertResolveFromIdentifier(final MediaTypeResolver mediaTypeResolver, final String identifier, final String[] expectedExtensions, final StorageType expectedStorageType) {
        final @Nullable MediaType mediaType = mediaTypeResolver.fromString(identifier);
        assertNotNull(mediaType);
        assertArrayAnyOrderEquals(expectedExtensions, mediaType.getKnownFileExtensions());
        assertEquals(expectedStorageType, mediaType.getStorageType());
    }

    private static <T> void assertArrayAnyOrderEquals(final T[] expected, final T[] actual) {
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertArrayEquals(expected, actual);
    }
}
