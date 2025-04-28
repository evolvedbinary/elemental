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

module namespace xt = "http://exist-db.org/xquery/xmldiff/test/compare";

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
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:persistent-same-content-different-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := fn:doc($xt:TEST_DOC2_PATH)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:persistent-different-content-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := fn:doc($xt:TEST_DOC3_PATH)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-same-content-same-docs() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := $xt:TEST_DOC1_CONTENT
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-same-content-different-docs() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := $xt:TEST_DOC2_CONTENT
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:memtree-different-content-docs() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := $xt:TEST_DOC3_CONTENT
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-same-content-same-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC1_PATH))
    let $b := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-same-content-different-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC2_PATH))
    let $b := (fn:doc($xt:TEST_DOC2_PATH), fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-persistent-different-content-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC3_PATH))
    let $b := (fn:doc($xt:TEST_DOC3_PATH), fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-same-content-same-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC1_CONTENT)
    let $b := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-same-content-different-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC2_CONTENT)
    let $b := ($xt:TEST_DOC2_CONTENT, $xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-memtree-different-content-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC3_CONTENT)
    let $b := ($xt:TEST_DOC3_CONTENT, $xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-persistent-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC2_PATH))
    let $b := (fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-memtree-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC2_CONTENT)
    let $b := ($xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-persistent-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC2_PATH))
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-memtree-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC2_CONTENT)
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:persistent-memtree-same-content-same-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := $xt:TEST_DOC1_CONTENT
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:persistent-memtree-same-content-different-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := $xt:TEST_DOC2_CONTENT
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:persistent-memtree-different-content-docs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := $xt:TEST_DOC3_CONTENT
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-persistent-same-content-same-docs() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := fn:doc($xt:TEST_DOC1_PATH)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-persistent-same-content-different-docs() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := fn:doc($xt:TEST_DOC2_PATH)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:memtree-persistent-different-content-docs() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := fn:doc($xt:TEST_DOC3_PATH)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-memtree-same-content-same-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC1_PATH))
    let $b := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-memtree-same-content-different-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC2_PATH))
    let $b := ($xt:TEST_DOC2_CONTENT, $xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-persistent-memtree-different-content-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC3_PATH))
    let $b := ($xt:TEST_DOC3_CONTENT, $xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-persistent-same-content-same-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC1_CONTENT)
    let $b := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-persistent-same-content-different-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC2_CONTENT)
    let $b := (fn:doc($xt:TEST_DOC2_PATH), fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-memtree-persistent-different-content-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC3_CONTENT)
    let $b := (fn:doc($xt:TEST_DOC3_PATH), fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-persistent-memtree-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), fn:doc($xt:TEST_DOC2_PATH))
    let $b := ($xt:TEST_DOC1_CONTENT)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-memtree-persistent-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, $xt:TEST_DOC2_CONTENT)
    let $b := (fn:doc($xt:TEST_DOC1_PATH))
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-persistent-memtree-docs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH), $xt:TEST_DOC2_CONTENT)
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-memtree-persistent-docs() {
    let $a := ($xt:TEST_DOC1_CONTENT, fn:doc($xt:TEST_DOC2_PATH))
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:persistent-same-content-same-attrs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    let $b := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:persistent-same-content-different-attrs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    let $b := fn:doc($xt:TEST_DOC2_PATH)/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:persistent-different-content-attrs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    let $b := fn:doc($xt:TEST_DOC3_PATH)/template/b/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-same-content-same-attrs() {
    let $a := $xt:TEST_DOC1_CONTENT/template/a/@x
    let $b := $xt:TEST_DOC1_CONTENT/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-same-content-different-attrs() {
    let $a := $xt:TEST_DOC1_CONTENT/template/a/@x
    let $b := $xt:TEST_DOC2_CONTENT/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:memtree-different-content-attrs() {
    let $a := $xt:TEST_DOC1_CONTENT/template/a/@x
    let $b := $xt:TEST_DOC3_CONTENT/template/b/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-same-content-same-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    let $b := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-same-content-different-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC2_PATH)/template/a/@x)
    let $b := (fn:doc($xt:TEST_DOC2_PATH)/template/a/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-persistent-different-content-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC3_PATH)/template/b/@x)
    let $b := (fn:doc($xt:TEST_DOC3_PATH)/template/b/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-same-content-same-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    let $b := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-same-content-different-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC2_CONTENT/template/a/@x)
    let $b := ($xt:TEST_DOC2_CONTENT/template/a/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-memtree-different-content-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC3_CONTENT/template/b/@x)
    let $b := ($xt:TEST_DOC3_CONTENT/template/b/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-persistent-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC2_PATH)/template/a/@x)
    let $b := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-memtree-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC2_CONTENT/template/a/@x)
    let $b := ($xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-persistent-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC2_PATH)/template/a/@x)
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-memtree-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC2_CONTENT/template/a/@x)
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:persistent-memtree-same-content-same-attrs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    let $b := $xt:TEST_DOC1_CONTENT/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:persistent-memtree-same-content-different-attrs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    let $b := $xt:TEST_DOC2_CONTENT/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:persistent-memtree-different-content-attrs() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    let $b := $xt:TEST_DOC3_CONTENT/template/b/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-persistent-same-content-same-attrs() {
    let $a := $xt:TEST_DOC1_CONTENT/template/a/@x
    let $b := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:memtree-persistent-same-content-different-attrs() {
    let $a := $xt:TEST_DOC1_CONTENT/template/a/@x
    let $b := fn:doc($xt:TEST_DOC2_PATH)/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:memtree-persistent-different-content-attrs() {
    let $a := $xt:TEST_DOC1_CONTENT/template/a/@x
    let $b := fn:doc($xt:TEST_DOC3_PATH)/template/b/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-memtree-same-content-same-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    let $b := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-persistent-memtree-same-content-different-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC2_PATH)/template/a/@x)
    let $b := ($xt:TEST_DOC2_CONTENT/template/a/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-persistent-memtree-different-content-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC3_PATH)/template/b/@x)
    let $b := ($xt:TEST_DOC3_CONTENT/template/b/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-persistent-same-content-same-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC1_CONTENT/template/a/@x)
    let $b := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertTrue
function xt:multiple-memtree-persistent-same-content-different-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC2_CONTENT/template/a/@x)
    let $b := (fn:doc($xt:TEST_DOC2_PATH)/template/a/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-memtree-persistent-different-content-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC3_CONTENT/template/b/@x)
    let $b := (fn:doc($xt:TEST_DOC3_PATH)/template/b/@x, fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-persistent-memtree-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, fn:doc($xt:TEST_DOC2_PATH)/template/a/@x)
    let $b := ($xt:TEST_DOC1_CONTENT/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-unequal-memtree-persistent-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, $xt:TEST_DOC2_CONTENT/template/a/@x)
    let $b := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x)
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-persistent-memtree-attrs() {
    let $a := (fn:doc($xt:TEST_DOC1_PATH)/template/a/@x, $xt:TEST_DOC2_CONTENT/template/a/@x)
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:multiple-empty-memtree-persistent-attrs() {
    let $a := ($xt:TEST_DOC1_CONTENT/template/a/@x, fn:doc($xt:TEST_DOC2_PATH)/template/a/@x)
    let $b := ()
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:persistent-doc-against-persistent-attr() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:persistent-doc-against-memtree-attr() {
    let $a := fn:doc($xt:TEST_DOC1_PATH)
    let $b := $xt:TEST_DOC1_CONTENT/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:memtree-doc-against-memtree-attr() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := $xt:TEST_DOC1_CONTENT/template/a/@x
    return
        xmldiff:compare($a, $b)
};

declare
    %test:assertFalse
function xt:memtree-doc-against-persistent-attr() {
    let $a := $xt:TEST_DOC1_CONTENT
    let $b := fn:doc($xt:TEST_DOC1_PATH)/template/a/@x
    return
        xmldiff:compare($a, $b)
};
