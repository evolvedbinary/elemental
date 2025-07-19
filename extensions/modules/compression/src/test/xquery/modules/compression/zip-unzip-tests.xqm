(:
 : Elemental
 : Copyright (C) 2024, Evolved Binary Ltd
 :
 : admin@evolvedbinary.com
 : https://www.evolvedbinary.com | https://www.elemental.xyz
 :
 : This library is free software; you can redistribute it and/or
 : modify it under the terms of the GNU Lesser General Public
 : License as published by the Free Software Foundation; version 2.1.
 :
 : This library is distributed in the hope that it will be useful,
 : but WITHOUT ANY WARRANTY; without even the implied warranty of
 : MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 : Lesser General Public License for more details.
 :
 : You should have received a copy of the GNU Lesser General Public
 : License along with this library; if not, write to the Free Software
 : Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 :)
xquery version "3.0";

module namespace zut = "http://exist-db.org/xquery/cache/test/zip-unzip";

import module namespace compression = "http://exist-db.org/xquery/compression";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace util = "http://exist-db.org/xquery/util";
declare namespace xmldb = "http://exist-db.org/xquery/xmldb";

declare variable $zut:TEST_COLLECTION_NAME := "zip-unzip-test";
declare variable $zut:TEST_COLLECTION_PATH := "/db/" || $zut:TEST_COLLECTION_NAME;

declare variable $zut:DOC_WITH_PIS_1_NAME := "doc-with-pis-1.xml";
declare variable $zut:DOC_WITH_PIS_1_PATH := $zut:TEST_COLLECTION_PATH || "/" || $zut:DOC_WITH_PIS_1_NAME;
declare variable $zut:DOC_WITH_PIS_1 := document {
    <?xml-model href="http://docbook.org/xml/5.0/rng/docbook.rng" schematypens="http://relaxng.org/ns/structure/1.0"?>,
    <?xml-model href="http://docbook.org/xml/5.0/rng/docbook.rng" type="application/xml" schematypens="http://purl.oclc.org/dsdl/schematron"?>,
    <article xmlns="http://docbook.org/ns/docbook" xmlns:xlink="http://www.w3.org/1999/xlink" version="5.0">
      <info>
        <title>Integration Testing</title>
        <date>2Q19</date>
        <keywordset>
          <keyword>application-development</keyword>
          <keyword>testing</keyword>
        </keywordset>
      </info>
    </article>
};

declare
    %test:setUp
function zut:setup() {
    xmldb:create-collection("/db", $zut:TEST_COLLECTION_NAME),
    xmldb:store($zut:TEST_COLLECTION_PATH, $zut:DOC_WITH_PIS_1_NAME, $zut:DOC_WITH_PIS_1)
};

declare
	%test:assertEquals(3)
function zut:entry-of-doc-with-pis() {
    let $entries := <entry type="xml" name="{$zut:DOC_WITH_PIS_1_NAME}">{doc($zut:DOC_WITH_PIS_1_PATH)}</entry>
    return
        let $zip-data := compression:zip($entries, true())
        return
            let $output-collection-path := $zut:TEST_COLLECTION_PATH || "/entry-of-doc-with-pis-output"
            let $_ := compression:unzip($zip-data, compression:no-filter#2, compression:db-store-entry3($output-collection-path))
            return
                count(doc($output-collection-path || "/" || $zut:DOC_WITH_PIS_1_NAME)/node())
};

declare
	%test:assertEquals(3)
function zut:entry-of-doc-child-nodes-including-pis() {
    let $entries := <entry type="xml" name="{$zut:DOC_WITH_PIS_1_NAME}">{doc($zut:DOC_WITH_PIS_1_PATH)/node()}</entry>
    return
        let $zip-data := compression:zip($entries, true())
        return
            let $output-collection-path := $zut:TEST_COLLECTION_PATH || "/entry-of-doc-child-nodes-including-pis"
            let $_ := compression:unzip($zip-data, compression:no-filter#2, compression:db-store-entry3($output-collection-path))
            return
                count(doc($output-collection-path || "/" || $zut:DOC_WITH_PIS_1_NAME)/node())
};

declare
	%test:assertEquals("<test>123</test>", "<test>abc</test>")
function zut:round-trip-no-filter2-db-store-entry-3-without-encoding() {
    let $entries := (
        <entry type="xml" name="test1.xml"><test>123</test></entry>,
        <entry type="xml" name="test2.xml"><test>abc</test></entry>
    ) return
        let $zip-data := compression:zip($entries, true())
        return
            let $output-collection-path := $zut:TEST_COLLECTION_PATH || "/round-trip-no-filter2-db-store-entry-3-without-encoding"
            let $_ := compression:unzip($zip-data, compression:no-filter#2, compression:db-store-entry3($output-collection-path))
            return
                collection($output-collection-path)
};

declare
	%test:assertEquals("<test>123</test>", "<test>abc</test>")
function zut:round-trip-no-filter2-db-store-entry-3-utf8-encoding() {
    let $entries := (
        <entry type="xml" name="test1.xml"><test>123</test></entry>,
        <entry type="xml" name="test2.xml"><test>abc</test></entry>
    ) return
        let $zip-data := compression:zip($entries, true())
        return
            let $output-collection-path := $zut:TEST_COLLECTION_PATH || "/round-trip-no-filter2-db-store-entry-3-utf8-encoding"
            let $_ := compression:unzip($zip-data, compression:no-filter#2, compression:db-store-entry3($output-collection-path), "UTF-8")
            return
                collection($output-collection-path)
};

declare
	%test:assertEquals("<test>123</test>", "<test>abc</test>")
function zut:round-trip-no-filter3-db-store-entry-4-without-encoding() {
    let $entries := (
        <entry type="xml" name="test1.xml"><test>123</test></entry>,
        <entry type="xml" name="test2.xml"><test>abc</test></entry>
    ) return
        let $zip-data := compression:zip($entries, true())
        return
            let $output-collection-path := $zut:TEST_COLLECTION_PATH || "/round-trip-no-filter3-db-store-entry-4-without-encoding"
            let $_ := compression:unzip($zip-data, compression:no-filter#3, <params/>, compression:db-store-entry4($output-collection-path), <params/>)
            return
                collection($output-collection-path)
};

declare
	%test:assertEquals("<test>123</test>", "<test>abc</test>")
function zut:round-trip-no-filter3-db-store-entry-4-utf8-encoding() {
    let $entries := (
        <entry type="xml" name="test1.xml"><test>123</test></entry>,
        <entry type="xml" name="test2.xml"><test>abc</test></entry>
    ) return
        let $zip-data := compression:zip($entries, true())
        return
            let $output-collection-path := $zut:TEST_COLLECTION_PATH || "/round-trip-no-filter3-db-store-entry-4-utf8-encoding"
            let $_ := compression:unzip($zip-data, compression:no-filter#3, <params/>, compression:db-store-entry4($output-collection-path), <params/>, "UTF-8")
            return
                collection($output-collection-path)
};
