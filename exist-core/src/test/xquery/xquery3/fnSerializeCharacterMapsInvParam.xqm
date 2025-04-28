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

module namespace testSerialize="http://exist-db.org/xquery/test/function_serialize";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $testSerialize:atomic := document {
    <atomic:root xmlns:atomic="http://www.w3.org/XQueryTest" xmlns:foo="http://www.example.com/foo"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <atomic:anyURI>http://www.example.com</atomic:anyURI>
      <atomic:NCName atomic:attr="aNCname">aNCname</atomic:NCName>
      <atomic:QName atomic:attr="foo:aQname">foo:aQname</atomic:QName>
    </atomic:root>
};

declare variable $testSerialize:params-007 :=
              <output:serialization-parameters
                   xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization">
                <output:method value="xml"/>
                <output:indent value="yes"/>
                <output:xindent value="yes" xmlns:vendor="http://vendor.example.com/"/>
              </output:serialization-parameters>;

declare variable $testSerialize:params-007a :=
              <output:serialization-parameters
                   xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization">
                <output:method value="xml"/>
                <output:indent value="yes"/>
                <vendor:xindent value="yes" xmlns:vendor="http://vendor.example.com/"/>
              </output:serialization-parameters>;

declare
    %test:assertTrue
function testSerialize:serialize-xml-107() {
    let $result := serialize($testSerialize:atomic,map {
        "method" : "xml",
        "indent" : true(),
        "xindent" : true()
    })
    return contains($result,'atomic')
};

(: an invalid param in the standard ("output") namespace is an error :)
declare
    %test:assertError("err:SEPM0017")
function testSerialize:serialize-xml-007() {
    let $result := serialize($testSerialize:atomic,$testSerialize:params-007)
    return contains($result,'atomic')
};

(: an invalid param in a nonstandard ("vendor") namespace is NOT an error - just ignored :)
declare
    %test:assertTrue
function testSerialize:serialize-xml-007a() {
    let $result := serialize($testSerialize:atomic,$testSerialize:params-007a)
    return contains($result,'atomic')
};

