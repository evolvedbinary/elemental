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

declare variable $testSerialize:serialize-032-src.xml := document {
  <root>
      <title>A document</title>
      <p>A paragraph containing a character $ which should be mapped to a different one</p>
  </root>
};

declare variable $testSerialize:atomic := document {
    <atomic:root xmlns:atomic="http://www.w3.org/XQueryTest" xmlns:foo="http://www.example.com/foo"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <atomic:anyURI>http://www.example.com</atomic:anyURI>
      <atomic:NCName atomic:attr="aNCname">aNCname</atomic:NCName>
      <atomic:QName atomic:attr="foo:aQname">foo:aQname</atomic:QName>
    </atomic:root>
};

declare variable $testSerialize:params-026 :=
              <output:serialization-parameters
                   xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization"
                   xmlns:my-output="http://example.com/xslt-xquery-serialization">
                <output:indent value="yes"/>
                <my-output:indent-spaces value="2"/>
              </output:serialization-parameters>;

declare variable $testSerialize:params-026-error :=
              <output:serialization-parameters
                   xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization"
                   xmlns:my-output="http://example.com/xslt-xquery-serialization">
                <output:indent value="yes" value2="no"/>
                <my-output:indent-spaces value="2"/>
              </output:serialization-parameters>;

declare variable $testSerialize:params-032 :=
              <output:serialization-parameters
                   xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization">
                <output:use-character-maps>
                  <output:character-map character="$" map-string="£€" />
                </output:use-character-maps>
              </output:serialization-parameters>;

declare variable $testSerialize:serialize-032-src := document {
  <root>
      <title>A document</title>
      <p>A paragraph containing a character $ which should be mapped to a different one</p>
  </root>
};

(: confirm this doesn't throw an error on the 026 params :)
declare
    %test:assertTrue
function testSerialize:use_character_maps-026() {
    let $result := serialize($testSerialize:atomic,$testSerialize:params-026)
    return contains($result, "foo:aQname")
};

(: confirm this does throw an error on the 026 params with a bad attribute added :)
declare
    %test:assertError("err:SEPM0017")
function testSerialize:use_character_maps-026-error() {
    let $result := serialize($testSerialize:atomic,$testSerialize:params-026-error)
    return contains($result, "foo:aQname")
};

(: confirm <output:use-character-maps> works :)
declare
    %test:assertEquals("<root><title>A document</title><p>A paragraph containing a character £€ which should be mapped to a different one</p></root>")
function testSerialize:use_character_maps-032() {
    let $result := serialize($testSerialize:serialize-032-src,$testSerialize:params-032)
    return $result
};
