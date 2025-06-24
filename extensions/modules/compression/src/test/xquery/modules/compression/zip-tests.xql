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

module namespace zt = "http://exist-db.org/xquery/cache/test/zip";

import module namespace compression = "http://exist-db.org/xquery/compression";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace util = "http://exist-db.org/xquery/util";
declare namespace xmldb = "http://exist-db.org/xquery/xmldb";

declare variable $zt:TEST_COLLECTION_NAME := "zip-test";
declare variable $zt:TEST_COLLECTION_PATH := "/db/" || $zt:TEST_COLLECTION_NAME;

declare variable $zt:DOC_1_NAME := "doc-1.xml";
declare variable $zt:DOC_1_PATH := $zt:TEST_COLLECTION_PATH || "/" || $zt:DOC_1_NAME;
declare variable $zt:DOC_1 := document { <test>123</test> };

declare variable $zt:DOC_2_NAME := "doc-2.xml";
declare variable $zt:DOC_2_PATH := $zt:TEST_COLLECTION_PATH || "/" || $zt:DOC_2_NAME;
declare variable $zt:DOC_2 := document { <test>abc</test> };

declare
    %test:setUp
function zt:setup() {
    xmldb:create-collection("/db", $zt:TEST_COLLECTION_NAME),
    xmldb:store($zt:TEST_COLLECTION_PATH, $zt:DOC_1_NAME, $zt:DOC_1),
    xmldb:store($zt:TEST_COLLECTION_PATH, $zt:DOC_2_NAME, $zt:DOC_2)
};

declare
	%test:assertExists
function zt:zip-xml-entries() {
    let $entries := (
        <entry type="xml" name="{$zt:DOC_1_NAME}">{doc($zt:DOC_1_PATH)}</entry>,
        <entry type="xml" name="{$zt:DOC_2_NAME}">{doc($zt:DOC_2_PATH)}</entry>
    ) return
        compression:zip($entries, true())
};

declare
	%test:assertExists
function zt:zip-uri-entries() {
    let $entries := (
        <entry type="xml" name="{$zt:DOC_1_NAME}">{$zt:DOC_1_PATH}</entry>,
        <entry type="xml" name="{$zt:DOC_2_NAME}">{$zt:DOC_2_PATH}</entry>
    ) return
        compression:zip($entries, true())
};

declare
	%test:assertExists
function zt:zip-uris() {
    let $entries := (
        xs:anyURI("xmldb:exist://" || $zt:DOC_1_PATH),
        xs:anyURI("xmldb:exist://" || $zt:DOC_2_PATH)
    ) return
        compression:zip($entries, true())
};

declare
	%test:assertExists
function zt:zip-collection-entry-with-hierarchy() {
    let $entries := (
        <entry type="collection" name="{$zt:TEST_COLLECTION_NAME}">{$zt:TEST_COLLECTION_PATH}</entry>
    ) return
        compression:zip($entries, true())
};

declare
	%test:assertExists
function zt:zip-collection-entry-without-hierarchy() {
    let $entries := (
        <entry type="collection" name="{$zt:TEST_COLLECTION_NAME}">{$zt:TEST_COLLECTION_PATH}</entry>
    ) return
        compression:zip($entries, false())
};

declare
	%test:assertExists
function zt:zip-collection-entry-with-hierarchy-strip-prefix() {
    let $entries := (
        <entry type="collection" name="{$zt:TEST_COLLECTION_NAME}">{$zt:TEST_COLLECTION_PATH}</entry>
    ) return
        compression:zip($entries, true(), "/db")
};

declare
	%test:assertExists
function zt:zip-collection-entry-without-hierarchy-strip-prefix() {
    let $entries := (
        <entry type="collection" name="{$zt:TEST_COLLECTION_NAME}">{$zt:TEST_COLLECTION_PATH}</entry>
    ) return
        compression:zip($entries, false(), "/db")
};

declare
	%test:assertExists
function zt:zip-collection-entry-with-hierarchy-strip-prefix-encoding-ascii() {
    let $entries := (
        <entry type="collection" name="{$zt:TEST_COLLECTION_NAME}">{$zt:TEST_COLLECTION_PATH}</entry>
    ) return
        compression:zip($entries, true(), "/db", "ASCII")
};

declare
	%test:assertExists
function zt:zip-collection-entry-with-hierarchy-strip-prefix-encoding-utf8() {
    let $entries := (
        <entry type="collection" name="{$zt:TEST_COLLECTION_NAME}">{$zt:TEST_COLLECTION_PATH}</entry>
    ) return
        compression:zip($entries, true(), "/db", "UTF-8")
};