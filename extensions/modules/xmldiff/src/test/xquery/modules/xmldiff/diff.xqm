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
xquery version "3.1";

module namespace xt = "http://exist-db.org/xquery/xmldiff/test/diff";

import module namespace xmldb = "http://exist-db.org/xquery/xmldb";
import module namespace xmldiff = "http://exist-db.org/xquery/xmldiff";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare variable $xt:TEST_COLLECTION_NAME := "test-xmldiff-compare";
declare variable $xt:TEST_COLLECTION := "/db/" || $xt:TEST_COLLECTION_NAME;

declare variable $xt:TEST_DOC1_NAME := "doc1.xml";
declare variable $xt:TEST_DOC1_PATH := $xt:TEST_COLLECTION || "/" || $xt:TEST_DOC1_NAME;
declare variable $xt:TEST_DOC1_CONTENT := document { <template><a x="1"/></template> };

declare variable $xt:TEST_DOC2_NAME := "doc2.xml";
declare variable $xt:TEST_DOC2_PATH := $xt:TEST_COLLECTION || "/" || $xt:TEST_DOC2_NAME;
declare variable $xt:TEST_DOC2_CONTENT := document { <template><a x="1"/></template> };

declare variable $xt:TEST_DOC3_NAME := "doc3.xml";
declare variable $xt:TEST_DOC3_PATH := $xt:TEST_COLLECTION || "/" || $xt:TEST_DOC3_NAME;
declare variable $xt:TEST_DOC3_CONTENT := document { <template><b x="2"/></template> };


declare
    %test:setUp
function xt:setup() {
    xmldb:create-collection("/db", $xt:TEST_COLLECTION_NAME),
    xmldb:store($xt:TEST_COLLECTION, $xt:TEST_DOC1_NAME, $xt:TEST_DOC1_CONTENT),
    xmldb:store($xt:TEST_COLLECTION, $xt:TEST_DOC2_NAME, $xt:TEST_DOC2_CONTENT),
    xmldb:store($xt:TEST_COLLECTION, $xt:TEST_DOC3_NAME, $xt:TEST_DOC3_CONTENT)
};

declare
    %test:tearDown
function xt:cleanup() {
    xmldb:remove($xt:TEST_COLLECTION)
};

declare
    %test:assertTrue
function xt:persistent-same-content-same-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := fn:doc($xt:TEST_DOC1_PATH)
    return
        xmldiff:diff($a, $b)?equivalent
};

declare
    %test:assertTrue
function xt:persistent-same-content-different-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := fn:doc($xt:TEST_DOC2_PATH)
    return
        xmldiff:diff($a, $b)?equivalent
};

declare
    %test:assertEquals('false', 1, "Expected element tag name 'a' but was 'b'")
function xt:persistent-different-content-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := fn:doc($xt:TEST_DOC3_PATH)
    return
        let $result := xmldiff:diff($a, $b)
        return
            ($result?equivalent, $result?position, fn:substring-before($result?message, " -"))
};
