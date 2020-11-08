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

    // TODO(AR) this file looks a lot like eXist-db's mime-types.xml -- https://docs.sdl.com/787645/584296/sdl-tridion-docs-14/adding-a-mime-type
    // TODO(AR) investigate the above

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
        assertAllResolveFromFileName("something.atom", "application/atom+xml", new String[] {"atom"}, StorageType.XML);
    }

    @Test
    public void allResolveCsvExtension() {
        assertAllResolveFromFileName("something.csv", "text/csv", new String[] {"csv"}, StorageType.BINARY);
    }

    @Test
    public void allResolveDocxExtension() {
        assertAllResolveFromFileName("something.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new String[] {"docx"}, StorageType.XML);
    }

    @Test
    public void allResolveDtdExtension() {
        assertAllResolveFromFileName("something.dtd", "application/xml-dtd", new String[] {"dtd"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGifExtension() {
        assertAllResolveFromFileName("something.gif", "image/gif", new String[] {"gif"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGmlExtension() {
        assertAllResolveFromFileName("something.gml", "application/gml+xml", new String[] {"gml"}, StorageType.XML);
    }

    @Test
    public void allResolveHtmExtension() {
        assertAllResolveFromFileName("something.htm", "text/html", new String[] {"htm", "html"}, StorageType.BINARY);
    }

    @Test
    public void allResolveHtmlExtension() {
        assertAllResolveFromFileName("something.html", "text/html", new String[] {"htm", "html"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJpegExtension() {
        assertAllResolveFromFileName("something.jpeg", "image/jpeg", new String[] {"jpe", "jpg", "jpeg"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJpgExtension() {
        assertAllResolveFromFileName("something.jpg", "image/jpeg", new String[] {"jpe", "jpg", "jpeg"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsExtension() {
        assertAllResolveFromFileName("something.js", "text/javascript", new String[] {"js", "mjs"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsonExtension() {
        assertAllResolveFromFileName("something.json", "application/json", new String[] {"json"}, StorageType.BINARY);
    }

    @Test
    public void allResolveMadsExtension() {
        assertAllResolveFromFileName("something.mads", "application/mads+xml", new String[] {"mads"}, StorageType.XML);
    }

    @Test
    public void allResolveMetsExtension() {
        assertAllResolveFromFileName("something.mets", "application/mets+xml", new String[] {"mets"}, StorageType.XML);
    }

    @Test
    public void allResolveModsExtension() {
        assertAllResolveFromFileName("something.mods", "application/mods+xml", new String[] {"mods"}, StorageType.XML);
    }

    @Test
    public void allResolveMrcxExtension() {
        assertAllResolveFromFileName("something.mrcx", "application/marcxml+xml", new String[] {"mrcx"}, StorageType.XML);
    }

    @Test
    public void allResolveN3Extension() {
        assertAllResolveFromFileName("something.n3", "text/n3", new String[] {"n3"}, StorageType.BINARY);
    }

    @Test
    public void allResolveNcxExtension() {
        assertAllResolveFromFileName("something.ncx", "application/x-dtbncx+xml", new String[] {"ncx"}, StorageType.XML);
    }

    @Test
    public void allResolveOdtExtension() {
        assertAllResolveFromFileName("something.odt", "application/vnd.oasis.opendocument.text", new String[] {"odt"}, StorageType.XML);
    }

    @Test
    public void allResolveOdpExtension() {
        assertAllResolveFromFileName("something.odp", "application/vnd.oasis.opendocument.presentation", new String[] {"odp"}, StorageType.XML);
    }

    @Test
    public void allResolveOdsExtension() {
        assertAllResolveFromFileName("something.ods", "application/vnd.oasis.opendocument.spreadsheet", new String[] {"ods"}, StorageType.XML);
    }

    @Test
    public void allResolveOpfExtension() {
        assertAllResolveFromFileName("something.opf", "application/oebps-package+xml", new String[] {"opf"}, StorageType.XML);
    }

    @Test
    public void allResolvePngExtension() {
        assertAllResolveFromFileName("something.png", "image/png", new String[] {"png"}, StorageType.BINARY);
    }

    @Test
    public void allResolvePptxExtension() {
        assertAllResolveFromFileName("something.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", new String[] {"pptx"}, StorageType.XML);
    }

    @Test
    public void allResolveRncExtension() {
        assertAllResolveFromFileName("something.rnc", "application/relax-ng-compact-syntax", new String[] {"rnc"}, StorageType.BINARY);
    }

    @Test
    public void allResolveRssExtension() {
        assertAllResolveFromFileName("something.rss", "application/rss+xml", new String[] {"rss"}, StorageType.XML);
    }

    @Test
    public void allResolveSruExtension() {
        assertAllResolveFromFileName("something.sru", "application/sru+xml", new String[] {"sru"}, StorageType.XML);
    }

    @Test
    public void allResolveTtlExtension() {
        assertAllResolveFromFileName("something.ttl", "text/turtle", new String[] {"ttl"}, StorageType.BINARY);
    }

    @Test
    public void allResolveTxtExtension() {
        assertAllResolveFromFileName("something.txt", "text/plain", new String[] {"txt", "def", "log", "in", "conf", "text", "list"}, StorageType.BINARY);
    }

    @Test
    public void allResolveWsdlExtension() {
        assertAllResolveFromFileName("something.wsdl", "application/wsdl+xml", new String[] {"wsdl"}, StorageType.XML);
    }

    @Test
    public void allResolveXhtExtension() {
        assertAllResolveFromFileName("something.xht", "application/xhtml+xml", new String[] {"xht", "xhtml"}, StorageType.XML);
    }

    @Test
    public void allResolveXhtmlExtension() {
        assertAllResolveFromFileName("something.xhtml", "application/xhtml+xml", new String[] {"xht", "xhtml"}, StorageType.XML);
    }

    @Test
    public void allResolveXlsxExtension() {
        assertAllResolveFromFileName("something.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new String[] {"xlsx"}, StorageType.XML);
    }

    @Test
    public void allResolveXplExtension() {
        assertAllResolveFromFileName("something.xpl", "application/xproc+xml", new String[] {"xpl"}, StorageType.XML);
    }

    @Test
    public void allResolveXsltExtension() {
        assertAllResolveFromFileName("something.xslt", "application/xslt+xml", new String[] {"xslt"}, StorageType.XML);
    }

    @Test
    public void allResolveAtomIdentifier() {
        assertAllResolveFromIdentifier("application/atom+xml", new String[] {"atom"}, StorageType.XML);
    }

    @Test
    public void allResolveCsvIdentifier() {
        assertAllResolveFromIdentifier("text/csv", new String[] {"csv"}, StorageType.BINARY);
    }

    @Test
    public void allResolveDocxIdentifier() {
        assertAllResolveFromIdentifier("application/vnd.openxmlformats-officedocument.wordprocessingml.document", new String[] {"docx"}, StorageType.XML);
    }

    @Test
    public void allResolveDtdIdentifier() {
        assertAllResolveFromIdentifier("application/xml-dtd", new String[] {"dtd"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGifIdentifier() {
        assertAllResolveFromIdentifier("image/gif", new String[] {"gif"}, StorageType.BINARY);
    }

    @Test
    public void allResolveGmlIdentifier() {
        assertAllResolveFromIdentifier("application/gml+xml", new String[] {"gml"}, StorageType.XML);
    }

    @Test
    public void allResolveHtmlIdentifier() {
        assertAllResolveFromIdentifier("text/html", new String[] {"htm", "html"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJpegIdentifier() {
        assertAllResolveFromIdentifier("image/jpeg", new String[] {"jpe", "jpg", "jpeg"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsIdentifier() {
        assertAllResolveFromIdentifier("text/javascript", new String[] {"js", "mjs"}, StorageType.BINARY);
    }

    @Test
    public void allResolveJsonIdentifier() {
        assertAllResolveFromIdentifier("application/json", new String[] {"json"}, StorageType.BINARY);
    }

    @Test
    public void allResolveMadsIdentifier() {
        assertAllResolveFromIdentifier("application/mads+xml", new String[] {"mads"}, StorageType.XML);
    }

    @Test
    public void allResolveMetsIdentifier() {
        assertAllResolveFromIdentifier("application/mets+xml", new String[] {"mets"}, StorageType.XML);
    }

    @Test
    public void allResolveModsIdentifier() {
        assertAllResolveFromIdentifier("application/mods+xml", new String[] {"mods"}, StorageType.XML);
    }

    @Test
    public void allResolveMrcxIdentifier() {
        assertAllResolveFromIdentifier("application/marcxml+xml", new String[] {"mrcx"}, StorageType.XML);
    }

    @Test
    public void allResolveN3Identifier() {
        assertAllResolveFromIdentifier("text/n3", new String[] {"n3"}, StorageType.BINARY);
    }

    @Test
    public void allResolveNcxIdentifier() {
        assertAllResolveFromIdentifier("application/x-dtbncx+xml", new String[] {"ncx"}, StorageType.XML);
    }

    @Test
    public void allResolveOdtIdentifier() {
        assertAllResolveFromIdentifier("application/vnd.oasis.opendocument.text", new String[] {"odt"}, StorageType.XML);
    }

    @Test
    public void allResolveOdpIdentifier() {
        assertAllResolveFromIdentifier("application/vnd.oasis.opendocument.presentation", new String[] {"odp"}, StorageType.XML);
    }

    @Test
    public void allResolveOdsIdentifier() {
        assertAllResolveFromIdentifier("application/vnd.oasis.opendocument.spreadsheet", new String[] {"ods"}, StorageType.XML);
    }

    @Test
    public void allResolveOpfIdentifier() {
        assertAllResolveFromIdentifier("application/oebps-package+xml", new String[] {"opf"}, StorageType.XML);
    }

    @Test
    public void allResolvePngIdentifier() {
        assertAllResolveFromIdentifier("image/png", new String[] {"png"}, StorageType.BINARY);
    }

    @Test
    public void allResolvePptxIdentifier() {
        assertAllResolveFromIdentifier("application/vnd.openxmlformats-officedocument.presentationml.presentation", new String[] {"pptx"}, StorageType.XML);
    }

    @Test
    public void allResolveRncIdentifier() {
        assertAllResolveFromIdentifier("application/relax-ng-compact-syntax", new String[] {"rnc"}, StorageType.BINARY);
    }

    @Test
    public void allResolveRssIdentifier() {
        assertAllResolveFromIdentifier("application/rss+xml", new String[] {"rss"}, StorageType.XML);
    }

    @Test
    public void allResolveSruIdentifier() {
        assertAllResolveFromIdentifier("application/sru+xml", new String[] {"sru"}, StorageType.XML);
    }

    @Test
    public void allResolveTtlIdentifier() {
        assertAllResolveFromIdentifier("text/turtle", new String[] {"ttl"}, StorageType.BINARY);
    }

    @Test
    public void allResolveTxtIdentifier() {
        assertAllResolveFromIdentifier("text/plain", new String[] {"txt", "def", "log", "in", "conf", "text", "list"}, StorageType.BINARY);
    }

    @Test
    public void allResolveWsdlIdentifier() {
        assertAllResolveFromIdentifier("application/wsdl+xml", new String[] {"wsdl"}, StorageType.XML);
    }

    @Test
    public void allResolveXhtmlIdentifier() {
        assertAllResolveFromIdentifier("application/xhtml+xml", new String[] {"xht", "xhtml"}, StorageType.XML);
    }

    @Test
    public void allResolveXlsxIdentifier() {
        assertAllResolveFromIdentifier("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new String[] {"xlsx"}, StorageType.XML);
    }

    @Test
    public void allResolveXplIdentifier() {
        assertAllResolveFromIdentifier("application/xproc+xml", new String[] {"xpl"}, StorageType.XML);
    }

    @Test
    public void allResolveXsltIdentifier() {
        assertAllResolveFromIdentifier("application/xslt+xml", new String[] {"xslt"}, StorageType.XML);
    }
    // </editor-fold>


    // <editor-fold desc="Media Type definitions which are default only (e.g. sourced from Apache HTTPD)">
    @Test
    public void defaultResolveRdfExtension() {
        assertDefaultResolveFromFileName("something.rdf", "application/rdf+xml", new String[] {"rdf"}, StorageType.XML);
    }

    /**
     * The Media Type for SVGZ is horribly broken, see: <a href="https://github.com/w3c/svgwg/issues/701">w3c/svgwg/issues/701</a>
     * So it is overridden in the application's own <code>mime.types</code> file,
     * see {@link #applicationResolveSvgExtension()}.
     */
    @Test
    public void defaultResolveSvgExtension() {
        assertDefaultResolveFromFileName("something.svg", "image/svg+xml", new String[] {"svg", "svgz"}, StorageType.XML);
    }

    /**
     * The Media Type for SVGZ is horribly broken, see:  <a href="https://github.com/w3c/svgwg/issues/701">w3c/svgwg/issues/701</a>
     * So it is overridden in the application's own <code>mime.types</code> file,
     * see {@link #applicationResolveSvgzExtension()}.
     */
    @Test
    public void defaultResolveSvgzExtension() {
        assertDefaultResolveFromFileName("something.svgz", "image/svg+xml", new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void defaultResolveTeiExtension() {
        assertDefaultResolveFromFileName("something.tei", "application/tei+xml", new String[] {"tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void defaultResolveTeicorpusExtension() {
        assertDefaultResolveFromFileName("something.teicorpus", "application/tei+xml", new String[] {"tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void defaultResolveXmlExtension() {
        assertDefaultResolveFromFileName("something.xml", "application/xml", new String[] {"xsl", "xml"}, StorageType.XML);
    }

    @Test
    public void defaultResolveXslExtension() {
        assertDefaultResolveFromFileName("something.xsl", "application/xml", new String[] {"xsl", "xml"}, StorageType.XML);
    }

    @Test
    public void defaultResolveRdfIdentifier() {
        assertDefaultResolveFromIdentifier("application/rdf+xml", new String[] {"rdf"}, StorageType.XML);
    }

    /**
     * The Media Type for SVGZ is horribly broken, see: <a href="https://github.com/w3c/svgwg/issues/701">w3c/svgwg/issues/701</a>
     * So it is overridden in the application's own <code>mime.types</code> file.
     */
    @Test
    public void defaultResolveSvgIdentifier() {
        assertDefaultResolveFromIdentifier("image/svg+xml", new String[] {"svg", "svgz"}, StorageType.XML);
    }

    @Test
    public void defaultResolveTeiIdentifier() {
        assertDefaultResolveFromIdentifier("application/tei+xml", new String[] {"tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void defaultResolveXmlIdentifier() {
        assertDefaultResolveFromIdentifier("application/xml", new String[] {"xsl", "xml"}, StorageType.XML);
    }
    // </editor-fold>


    // <editor-fold desc="Media Type definitions which are Application (e.g. FusionDB) specific">
    @Test
    public void applicationResolveDitaExtension() {
        assertApplicationResolveFromFileName("something.dita", "application/dita+xml", new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveDitamapExtension() {
        assertApplicationResolveFromFileName("something.ditamap", "application/dita+xml", new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveDitavalExtension() {
        assertApplicationResolveFromFileName("something.ditaval", "application/dita+xml", new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveFoExtension() {
        assertApplicationResolveFromFileName("something.fo", "application/xml",  new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveNvdlExtension() {
        assertApplicationResolveFromFileName("something.nvdl", "application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveOddExtension() {
        assertApplicationResolveFromFileName("something.odd", "application/tei+xml", new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveOwlExtension() {
        assertApplicationResolveFromFileName("something.owl", "application/rdf+xml", new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveMdExtension() {
        assertApplicationResolveFromFileName("something.md", "text/markdown", new String[] {"md"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveRdfExtension() {
        assertApplicationResolveFromFileName("something.rdf", "application/rdf+xml", new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveRngExtension() {
        assertApplicationResolveFromFileName("something.rng", "application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSchExtension() {
        assertApplicationResolveFromFileName("something.sch", "application/schematron+xml", new String[] {"sch"}, StorageType.XML);
    }

    @Test
    public void applicationResolveStxExtension() {
        assertApplicationResolveFromFileName("something.stx", "application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgExtension() {
        assertApplicationResolveFromFileName("something.svg", "image/svg+xml", new String[] {"svg"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgzExtension() {
        assertApplicationResolveFromFileName("something.svgz", "image/x.svg+gzip", new String[] {"svgz"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveTeiExtension() {
        assertApplicationResolveFromFileName("something.tei", "application/tei+xml", new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveTeicorpusExtension() {
        assertApplicationResolveFromFileName("something.teicorpus", "application/tei+xml", new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXarExtension() {
        assertApplicationResolveFromFileName("something.xar", "application/x.expath.xar+zip", new String[] {"xar"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXconfExtension() {
        assertApplicationResolveFromFileName("something.xconf", "application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmiExtension() {
        assertApplicationResolveFromFileName("something.xmi", "application/vnd.xmi+xml", new String[] {"xmi"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmlExtension() {
        assertApplicationResolveFromFileName("something.xml", "application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmpExtension() {
        assertApplicationResolveFromFileName("something.xmp", "application/rdf+xml", new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXqExtension() {
        assertApplicationResolveFromFileName("something.xq", "application/xquery", new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqlExtension() {
        assertApplicationResolveFromFileName("something.xql", "application/xquery", new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqmExtension() {
        assertApplicationResolveFromFileName("something.xqm", "application/xquery", new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqueryExtension() {
        assertApplicationResolveFromFileName("something.xquery", "application/xquery", new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqwsExtension() {
        assertApplicationResolveFromFileName("something.xqws", "application/xquery", new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqxExtension() {
        assertApplicationResolveFromFileName("something.xqx", "application/xquery+xml", new String[] {"xqx"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXqyExtension() {
        assertApplicationResolveFromFileName("something.xqy", "application/xquery", new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXsdExtension() {
        assertApplicationResolveFromFileName("something.xsd", "application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXslExtension() {
        assertApplicationResolveFromFileName("something.xsl", "application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveDitaIdentifier() {
        assertApplicationResolveFromIdentifier("application/dita+xml", new String[] {"dita", "ditamap", "ditaval"}, StorageType.XML);
    }

    @Test
    public void applicationResolveMdIdentifier() {
        assertApplicationResolveFromIdentifier("text/markdown", new String[] {"md"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveRdfIdentifier() {
        assertApplicationResolveFromIdentifier("application/rdf+xml", new String[] {"xmp", "owl", "rdf"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSchIdentifier() {
        assertApplicationResolveFromIdentifier("application/schematron+xml", new String[] {"sch"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgIdentifier() {
        assertApplicationResolveFromIdentifier("image/svg+xml", new String[] {"svg"}, StorageType.XML);
    }

    @Test
    public void applicationResolveSvgzIdentifier() {
        assertApplicationResolveFromIdentifier("image/x.svg+gzip", new String[] {"svgz"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveTeiIdentifier() {
        assertApplicationResolveFromIdentifier("application/tei+xml", new String[] {"odd", "tei", "teicorpus"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmiIdentifier() {
        assertApplicationResolveFromIdentifier("application/vnd.xmi+xml", new String[] {"xmi"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXmlIdentifier() {
        assertApplicationResolveFromIdentifier("application/xml", new String[] {"fo", "nvdl", "rng", "stx", "xconf", "xml", "xsd", "xsl"}, StorageType.XML);
    }

    @Test
    public void applicationResolveXqueryIdentifier() {
        assertApplicationResolveFromIdentifier("application/xquery", new String[] {"xq", "xql", "xqm", "xquery", "xqws", "xqy"}, StorageType.BINARY);
    }

    @Test
    public void applicationResolveXqueryXIdentifier() {
        assertApplicationResolveFromIdentifier("application/xquery+xml", new String[] {"xqx"}, StorageType.XML);
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

        final ApplicationMimetypesFileTypeMap mimetypesFileTypeMap = new ApplicationMimetypesFileTypeMap(
            moreSpecificApplicationTypesConfigDir,
            defaultApplicationTypesConfigDir);

        assertEquals("application/xml", mimetypesFileTypeMap.getContentType("something.xadam"));
        assertEquals("application/x.exist-collection-config+xml", mimetypesFileTypeMap.getContentType("something.xconf"));
        assertEquals("application/xml", mimetypesFileTypeMap.getContentType("something.xml"));
        assertEquals("application/xml", mimetypesFileTypeMap.getContentType("something.xsd"));
        assertEquals("application/x.xsl+xml", mimetypesFileTypeMap.getContentType("something.xsl"));

        final MediaTypeResolverImpl specificMediaTypeResolver = new MediaTypeResolverImpl(mimetypesFileTypeMap, MEDIA_TYPE_MAPPER);

        assertResolveFromFileName(specificMediaTypeResolver, "something.xadam", "application/xml", new String[] {"nvdl", "xml", "stx", "xsd", "fo", "rng", "xadam"}, StorageType.XML);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xconf", "application/x.exist-collection-config+xml", new String[] {"xconf"}, StorageType.XML);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xml", "application/xml", new String[] {"nvdl", "xml", "stx", "xsd", "fo", "rng", "xadam"}, StorageType.XML);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xsd", "application/xml", new String[] {"nvdl", "xml", "stx", "xsd", "fo", "rng", "xadam"}, StorageType.XML);
        assertResolveFromFileName(specificMediaTypeResolver, "something.xsl", "application/x.xsl+xml", new String[] {"xsl"}, StorageType.XML);

        assertResolveFromIdentifier(specificMediaTypeResolver, "application/xml", new String[] {"nvdl", "xml", "stx", "xsd", "fo", "rng", "xadam"}, StorageType.XML);
        assertResolveFromIdentifier(specificMediaTypeResolver, "application/x.exist-collection-config+xml", new String[] {"xconf"}, StorageType.XML);
        assertResolveFromIdentifier(specificMediaTypeResolver, "application/x.xsl+xml", new String[] {"xsl"}, StorageType.XML);
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
