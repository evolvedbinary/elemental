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

module namespace fnuc="http://exist-db.org/xquery/test/function_uri_collection";

declare namespace test="http://exist-db.org/xquery/xqsuite";
declare namespace x="httpx://x/ns/1.0";

declare variable $fnuc:COLLECTION_NAME := "/test-collection";
declare variable $fnuc:SUBCOLLECTION_NAME := $fnuc:COLLECTION_NAME||"/subcol";
declare variable $fnuc:COLLECTION := collection("/db"||$fnuc:COLLECTION_NAME);
declare variable $fnuc:SUBCOLLECTION := collection("/db"||$fnuc:SUBCOLLECTION_NAME);

declare
    %test:setUp
function fnuc:setup() {
    let $collection := xmldb:create-collection("/db", "test-collection")

    return
        (
            xmldb:create-collection("/db", $fnuc:SUBCOLLECTION_NAME),
            xmldb:store("/db"||$fnuc:SUBCOLLECTION_NAME, "test-subcol.xml", <container><a/><b/></container>),
            xmldb:store($collection, "test.bin", "binary", "application/octet-stream"),
            xmldb:store($collection, "test.xml", document { <container><a/><b/></container>})
        )
};

declare
    %test:tearDown
function fnuc:cleanup() {
    xmldb:remove("/db/test-collection")
};

declare
    %test:assertEquals("/db")
function fnuc:no-argument() {
    fn:uri-collection()
};

declare
    %test:assertError("FODC0004")
function fnuc:invalid-uri() {
    fn:uri-collection(":invalid-uri")
};

declare
    %test:assertEquals("/db/test-collection/test.bin", "/db/test-collection/test.xml", "/db/test-collection/subcol")
function fnuc:all-uris() {
    fn:uri-collection("/db/test-collection")
};

declare
    %test:assertEquals("/db/test-collection/subcol")
function fnuc:subcollection-uris() {
    fn:uri-collection("/db/test-collection?content-type=application/vnd.existdb.collection")
};

declare
    %test:assertEquals("/db/test-collection/test.bin", "/db/test-collection/test.xml")
function fnuc:document-uris() {
    fn:uri-collection("/db/test-collection?content-type=application/vnd.existdb.document")
};

declare
    %test:assertEquals("/db/test-collection/test.xml")
function fnuc:xml-document-uris() {
    fn:uri-collection("/db/test-collection?content-type=application/vnd.existdb.document+xml")
};

declare
    %test:assertEquals("/db/test-collection/test.bin")
function fnuc:binary-document-uris() {
    fn:uri-collection("/db/test-collection?content-type=application/vnd.existdb.document+binary")
};

declare
    %test:assertEquals("/db/test-collection/test.bin")
function fnuc:match-uris() {
    fn:uri-collection("/db/test-collection?match=.*\.bin")
};

declare
    %test:assertEmpty
function fnuc:no-match-uris() {
    fn:uri-collection("/db/test-collection?match=.*\.nonexisting")
};

declare
    %test:assertEquals("/db/test-collection/test.bin", "/db/test-collection/test.xml", "/db/test-collection/subcol")
function fnuc:stable() {
    let $c1 := fn:uri-collection("/db/test-collection?stable=yes")
    let $r  := xmldb:remove("/db/test-collection", "test.xml")
    let $c2 := fn:uri-collection("/db/test-collection?stable=yes")
    let $a  := xmldb:store("/db/test-collection", "test.xml", document { <container><a/><b/></container>})
    return $c2
};

declare
    %test:assertEquals("/db/test-collection/test.bin", "/db/test-collection/subcol")
function fnuc:not-stable() {
    let $c1 := fn:uri-collection("/db/test-collection?stable=no")
    let $r  := xmldb:remove("/db/test-collection", "test.xml")
    let $c2 := fn:uri-collection("/db/test-collection?stable=no")
    let $a  := xmldb:store("/db/test-collection", "test.xml", document { <container><a/><b/></container>})
    return $c2
};

declare
    %test:assertError("FODC0002")
function fnuc:non-existent-resource() {
    fn:uri-collection("thisfileshouldnotexists")
};

