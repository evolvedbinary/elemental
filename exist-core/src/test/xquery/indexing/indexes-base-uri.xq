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

module namespace but="http://exist-db.org/xquery/indexes/base-uri-test";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $but:XML := <item/>;

declare variable $but:test-col-name := "but";
declare variable $but:test-col-uri := "/db/" || $but:test-col-name;
declare variable $but:test-col-a-name := "a";
declare variable $but:test-col-a-uri := $but:test-col-uri || "/" || $but:test-col-a-name;
declare variable $but:test-col-b-name := "b";
declare variable $but:test-col-b-uri := $but:test-col-uri || "/" || $but:test-col-b-name;

declare
    %test:setUp
function but:setup() {
    xmldb:create-collection("/db", $but:test-col-name),
    xmldb:create-collection($but:test-col-uri, $but:test-col-a-name),
    xmldb:create-collection($but:test-col-uri, $but:test-col-b-name)
};

declare
    %test:tearDown
function but:tearDown() {
    xmldb:remove($but:test-col-uri)
};

declare
    %test:pending("Each test interferes with each other test, we need to figure out how to have but:tearDown called after every test")
    %test:assertEquals("/db/but/a/data/test.xml", "/db/but/a/data/test.xml", "/db/but/b/data/test.xml")
function but:base-uri-after-collection-copy() {
    let $test-col-a-data-uri := xmldb:create-collection($but:test-col-a-uri, "data")
    let $doc-path := xmldb:store($test-col-a-data-uri, "test.xml", $but:XML)
    let $base-uri-before-copy := collection($but:test-col-uri)/item/base-uri(.)
    return

    let $_ := xmldb:copy-collection($test-col-a-data-uri, $but:test-col-b-uri)
    return
        let $base-uri-after-copy := collection($but:test-col-uri)/item/base-uri(.)
        return

            ($base-uri-before-copy, $base-uri-after-copy)
};

declare
    %test:assertEquals("/db/but/a/data/test.xml", "/db/but/b/data/test.xml")
function but:base-uri-after-collection-move() {
    let $test-col-a-data-uri := xmldb:create-collection($but:test-col-a-uri, "data")
    let $doc-path := xmldb:store($test-col-a-data-uri, "test.xml", $but:XML)
    let $base-uri-before-move := collection($but:test-col-uri)/item/base-uri(.)
    return

    let $_ := xmldb:move($test-col-a-data-uri, $but:test-col-b-uri)
    return
        let $base-uri-after-move := collection($but:test-col-uri)/item/base-uri(.)
        return

            ($base-uri-before-move, $base-uri-after-move)
};

declare
    %test:pending("Each test interferes with each other test, we need to figure out how to have but:tearDown called after every test")
    %test:assertEquals("/db/but/a/data/test.xml", "/db/but/a/data/test.xml", "/db/but/b/test.xml")
function but:base-uri-after-resource-copy() {
    let $test-col-a-data-uri := xmldb:create-collection($but:test-col-a-uri, "data")
    let $doc-path := xmldb:store($test-col-a-data-uri, "test.xml", $but:XML)
    let $base-uri-before-copy := collection($but:test-col-uri)/item/base-uri(.)
    return

    let $_ := xmldb:copy-resource($test-col-a-data-uri, "test.xml", $but:test-col-b-uri, ())
    return
        let $base-uri-after-copy := collection($but:test-col-uri)/item/base-uri(.)
        return

            ($base-uri-before-copy, $base-uri-after-copy)
};

declare
    %test:pending("Each test interferes with each other test, we need to figure out how to have but:tearDown called after every test")
    %test:assertEquals("/db/but/a/data/test.xml", "/db/but/b/test.xml")
function but:base-uri-after-resource-move() {
    let $test-col-a-data-uri := xmldb:create-collection($but:test-col-a-uri, "data")
    let $doc-path := xmldb:store($test-col-a-data-uri, "test.xml", $but:XML)
    let $base-uri-before-move := collection($but:test-col-uri)/item/base-uri(.)
    return

    let $_ := xmldb:move($test-col-a-data-uri, $but:test-col-b-uri, "test.xml")
    return
        let $base-uri-after-move := collection($but:test-col-uri)/item/base-uri(.)
        return

            ($base-uri-before-move, $base-uri-after-move)
};
