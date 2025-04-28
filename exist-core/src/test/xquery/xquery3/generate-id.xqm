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

module namespace gid = "http://exist-db.org/xquery/test/generate-id";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace xmldb = "http://exist-db.org/xquery/xmldb";

declare
    %test:setUp
function gid:setup() {
  xmldb:create-collection("/db", "generate-id-test"),
  xmldb:store("/db/generate-id-test", "a.xml", <node>a</node>),
  xmldb:store("/db/generate-id-test", "b.xml", <node>b</node>),
  xmldb:store("/db/generate-id-test", "inner-a.xml", <node><inner>a</inner></node>),

  if ((("a.xml", "b.xml", "inner-a.xml") ! doc-available("/db/generate-id-test/" || .)) = false())
  then
    error(xs:QName("gid:missing-documents"), "Missing setup documents")
  else ()
};

declare
    %test:tearDown
function gid:cleanup() {
  xmldb:remove("/db/generate-id-test")
};


declare
    %test:assertFalse
function gid:parsed-in-memory-dom-identity() {
  let $node1 := parse-xml-fragment("<node>a</node>")/node(),
      $node2 := parse-xml-fragment("<node>b</node>")/node()
  return
    codepoint-equal(generate-id($node1), generate-id($node2))
};

declare
    %test:assertEquals("false", "true", "true")
function gid:in-memory-dom-element-identity() {
  let $node1 := <node>a</node>,
      $node2 := <node>b</node>
  return
  (
      codepoint-equal(generate-id($node1), generate-id($node2)),
      codepoint-equal(generate-id($node1), generate-id($node1)),
      codepoint-equal(generate-id($node2), generate-id($node2))
  )
};

declare
    %test:assertEquals("false", "true", "true")
function gid:in-memory-dom-document-identity() {
  let $node1 := document { <node>a</node> },
      $node2 := document { <node>b</node> }
  return
    (
        codepoint-equal(generate-id($node1), generate-id($node2)),
        codepoint-equal(generate-id($node1), generate-id($node1)),
        codepoint-equal(generate-id($node2), generate-id($node2))
    )
};

declare
    %test:assertEquals("false", "true", "true")
function gid:in-memory-dom-document-element-identity() {
  let $node1 := document { <node>a</node> }/element(),
      $node2 := document { <node>b</node> }/element()
  return
    (
        codepoint-equal(generate-id($node1), generate-id($node2)),
        codepoint-equal(generate-id($node1), generate-id($node1)),
        codepoint-equal(generate-id($node2), generate-id($node2))
    )
};

declare
    %test:assertEquals("true", "true")
function gid:in-memory-dom-element-identity-ascii-alphanum-only() {
  let $node1 := <node><inner>a</inner></node>,
      $id := generate-id($node1/inner)
  return
    (
        starts-with($id, "M"),
        matches($id, "[a-zA-Z0-9]+")
    )
};

declare
    %test:assertEquals("false", "true", "true")
function gid:persistent-dom-document-identity() {
  let $node1 := doc("/db/generate-id-test/a.xml"),
      $node2 := doc("/db/generate-id-test/b.xml")
  return
    (
        codepoint-equal(generate-id($node1), generate-id($node2)),
        codepoint-equal(generate-id($node1), generate-id($node1)),
        codepoint-equal(generate-id($node2), generate-id($node2))
    )
};

declare
    %test:assertEquals("false", "true", "true")
function gid:persistent-dom-document-element-identity() {
  let $node1 := doc("/db/generate-id-test/a.xml")/element(),
      $node2 := doc("/db/generate-id-test/b.xml")/element()
  return
    (
        codepoint-equal(generate-id($node1), generate-id($node2)),
        codepoint-equal(generate-id($node1), generate-id($node1)),
        codepoint-equal(generate-id($node2), generate-id($node2))
    )
};

declare
    %test:assertEquals("true", "true")
function gid:persistent-dom-element-identity-ascii-alphanum-only() {
  let $node1 := doc("/db/generate-id-test/inner-a.xml"),
      $id := generate-id($node1/node/inner)
  return
    (
        starts-with($id, "P"),
        matches($id, "[a-zA-Z0-9]+")
    )
};
