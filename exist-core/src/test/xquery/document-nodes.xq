(:
 : Elemental
 : Copyright (C) 2024, Evolved Binary Ltd
 :
 : admin@evolvedbinary.com
 : https://www.evolvedbinary.com | https://www.elemental.xyz
 :
 : Use of this software is governed by the Business Source License 1.1
 : included in the LICENSE file and at www.mariadb.com/bsl11.
 :
 : Change Date: 2028-04-27
 :
 : On the date above, in accordance with the Business Source License, use
 : of this software will be governed by the Apache License, Version 2.0.
 :
 : Additional Use Grant: Production use of the Licensed Work for a permitted
 : purpose. A Permitted Purpose is any purpose other than a Competing Use.
 : A Competing Use means making the Software available to others in a commercial
 : product or service that: substitutes for the Software; substitutes for any
 : other product or service we offer using the Software that exists as of the
 : date we make the Software available; or offers the same or substantially
 : similar functionality as the Software.
 :)
xquery version "3.0";

(:~ Additional tests for document-node() :)
module namespace dn = "http://exist-db.org/xquery/test/document-node";

declare namespace test="http://exist-db.org/xquery/xqsuite";

import module namespace xmldb="http://exist-db.org/xquery/xmldb";

declare variable $dn:TEST_COLLECTION_NAME := "test-document-node";
declare variable $dn:TEST_COLLECTION := "/db/" || $dn:TEST_COLLECTION_NAME;
declare variable $dn:TEST_DOC_NAME := "test1.xml";
declare variable $dn:TEST_DOC := $dn:TEST_COLLECTION || "/" || $dn:TEST_DOC_NAME;

declare
    %test:setUp
function dn:setup() {
    xmldb:create-collection("/db", $dn:TEST_COLLECTION_NAME),
    xmldb:store($dn:TEST_COLLECTION, $dn:TEST_DOC_NAME, <template/>)
};

declare
    %test:tearDown
function dn:cleanup() {
    xmldb:remove($dn:TEST_COLLECTION)
};

declare
    %test:assertEquals("<template/>")
function dn:persistent-document-node() {
    let $d as document-node() := doc($dn:TEST_DOC)
    return
    	$d
};

declare
    %test:assertEquals("<template/>")
function dn:persistent-document-node-element() {
    let $d as document-node(element()) := doc($dn:TEST_DOC)
    return
    	$d
};

declare
    %test:assertEquals("<template/>")
function dn:persistent-document-node-element-wildcard() {
    let $d as document-node(element(*)) := doc($dn:TEST_DOC)
    return
    	$d
};

declare
    %test:assertEquals("<template/>")
function dn:persistent-document-node-element-name() {
    let $d as document-node(element(template)) := doc($dn:TEST_DOC)
    return
    	$d
};

declare
    %test:assertError("err:XPTY0004")
function dn:persistent-document-node-element-wrong-name() {
    let $d as document-node(element(wrong)) := doc($dn:TEST_DOC)
    return
    	$d
};

declare
    %test:assertTrue
function dn:memtree-document-node() {
    document { element template {} } instance of document-node()
};

declare
    %test:assertTrue
function dn:memtree-document-node-element() {
    document { element template {} } instance of document-node(element())
};

declare
    %test:assertTrue
function dn:memtree-document-node-element-wildcard() {
    document { element template {} } instance of document-node(element(*))
};

declare
    %test:assertTrue
function dn:memtree-document-node-element-name() {
    document { element template {} } instance of document-node(element(template))
};

declare
    %test:assertFalse
function dn:memtree-document-node-element-wrong-name() {
    document { element template {} } instance of document-node(element(wrong))
};

declare
    %test:assertEmpty
function dn:persistent-document-node-from-collection() {
    collection($dn:TEST_COLLECTION)[1]/document-node()
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-doc() {
    collection($dn:TEST_COLLECTION)[1]/doc(document-uri(.))
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-self-axis() {
    collection($dn:TEST_COLLECTION)[1]/self::document-node()
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-ancestor-of-self-axis() {
    collection($dn:TEST_COLLECTION)[1]/ancestor-or-self::document-node()
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-parent-axis-node() {
    (collection($dn:TEST_COLLECTION)/template)[1]/parent::node()
};

declare
    %test:assertEmpty
function dn:persistent-document-node-from-collection-via-parent-axis-wildcard() {
    (collection($dn:TEST_COLLECTION)/template)[1]/parent::*
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-parent-axis-document-node() {
    (collection($dn:TEST_COLLECTION)/template)[1]/parent::document-node()
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-parent-axis-document-element() {
    (collection($dn:TEST_COLLECTION)/template)[1]/parent::document-node(element(template))
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-ancestor-axis-document-node() {
    (collection($dn:TEST_COLLECTION)/template)[1]/ancestor::document-node()
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-ancestor-axis-document-element() {
    (collection($dn:TEST_COLLECTION)/template)[1]/ancestor::document-node(element(template))
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-ancestor-or-self-axis-document-node() {
    (collection($dn:TEST_COLLECTION)/template)[1]/ancestor-or-self::document-node()
};

declare
    %test:assertExists
function dn:persistent-document-node-from-collection-via-ancestor-or-self-axis-document-element() {
    (collection($dn:TEST_COLLECTION)/template)[1]/ancestor-or-self::document-node(element(template))
};
