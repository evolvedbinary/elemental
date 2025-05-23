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

module namespace uni="http://exist-db.org/xquery/test/unicode";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $uni:SIP :=
    <container>
        <smp>&#x2a3c6;</smp>
    </container>
;

declare variable $uni:COLLECTION_NAME := "test-unicode";

declare
    %test:setUp
function uni:setup() {
    xmldb:create-collection("/db", $uni:COLLECTION_NAME)
};

declare
    %test:tearDown
function uni:cleanup() {
    xmldb:remove($uni:COLLECTION_NAME)
};

declare function uni:transform($element as element(container)) as element() {
    element { node-name($element) } {
        $element/@*,
        for $child in $element/node()
        return
            if ($child instance of element(smp))
            then $child/string()
            else $child
    }
};

declare
    %test:assertEquals(172998)
function uni:sip-dom-memtree() {
    string-to-codepoints($uni:SIP/smp)
};

declare
    %test:assertEquals(172998)
function uni:sip-dom-persistent() {
    let $stored := xmldb:store("/db/" || $uni:COLLECTION_NAME, "sip.xml", $uni:SIP)
    return
        fn:string-to-codepoints(fn:doc($stored)/container/smp)
};

declare
    %test:assertEquals(172998)
function uni:sip-dom-memtree-transform() {
    let $transformed := uni:transform($uni:SIP)
    return
        fn:string-to-codepoints($transformed)
};

declare
    %test:assertEquals(172998)
function uni:sip-dom-persistent-transform() {
    let $transformed := uni:transform($uni:SIP)
    let $stored := xmldb:store("/db/" || $uni:COLLECTION_NAME, "sip-transformed.xml", $transformed)
    return
        fn:string-to-codepoints(fn:doc($stored)/container)
};

declare
    %test:assertEquals(172998)
function uni:sip-dom-persistent-transform-2() {
    let $stored := xmldb:store("/db/" || $uni:COLLECTION_NAME, "sip.xml", $uni:SIP)
    let $transformed := uni:transform(fn:doc($stored)/container)
    let $stored := xmldb:store("/db/" || $uni:COLLECTION_NAME, "sip-transformed.xml", $transformed)
    return
        fn:string-to-codepoints(fn:doc($stored)/container)
};
