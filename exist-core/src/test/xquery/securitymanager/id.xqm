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

module namespace id = "http://exist-db.org/test/securitymanager/id";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace mod1 = "http://module1";
declare namespace sm = "http://exist-db.org/xquery/securitymanager";

declare variable $id:TEST_COLLECTION_NAME := "test-id";
declare variable $id:TEST_COLLECTION_PATH := "/db/test-id";
declare variable $id:TEST_MODULE_NAME := "mod1.xqm";

declare
    %test:setUp
function id:setup() {
    xmldb:create-collection("/db", $id:TEST_COLLECTION_NAME),
    xmldb:store($id:TEST_COLLECTION_PATH, $id:TEST_MODULE_NAME, 'xquery version "3.0";

module namespace mod1 = "http://module1";

declare function mod1:function1() {
    <mod1>{sm:id()}</mod1>
};
    ', "application/xquery")
};

declare
    %test:tearDown
function id:cleanup() {
    xmldb:remove($id:TEST_COLLECTION_PATH)
};

declare
    %test:assertEquals(1)
function id:from-load-module() {
    let $mod1-fn := fn:load-xquery-module("http://module1", map {
        "location-hints": "xmldb:exist://" || $id:TEST_COLLECTION_PATH || "/" || $id:TEST_MODULE_NAME
    })?functions(xs:QName("mod1:function1"))?0
    return
    	fn:count($mod1-fn()//sm:username)
};

declare
    %test:assertEquals(1)
function id:from-inspect-module-functions() {
    let $mod1-fn := inspect:module-functions(xs:anyURI("xmldb:exist://" || $id:TEST_COLLECTION_PATH || "/" || $id:TEST_MODULE_NAME))[1]
    return
        fn:count($mod1-fn()//sm:username)
};
