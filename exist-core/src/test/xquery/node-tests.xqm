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

module namespace nt = "http://exist-db.org/xquery/test/node-tests";

import module namespace test="http://exist-db.org/xquery/xqsuite" at "resource:org/exist/xquery/lib/xqsuite/xqsuite.xql";


declare %private function nt:f-document-node($a as document-node()) {
  $a//text()
};

declare %private function nt:f-document-node-with-document-element($a as document-node(element())) {
  $a//text()
};

declare %private function nt:f-document-node-with-named-document-element($a as document-node(element(a))) {
  $a//text()
};


declare
    %test:assertEmpty
function nt:test-document-node-valid-1() {
  nt:f-document-node(document { () })
};

declare
    %test:assertEquals("is not b")
function nt:test-document-node-valid-2() {
  nt:f-document-node(document {<a>is not b</a>})
};

declare
    %test:assertError("XPTY0004")
function nt:test-document-node-invalid() {
  nt:f-document-node(<a>is not b</a>)
};

declare
    %test:assertEquals("is not b")
function nt:test-document-node-with-document-element-valid() {
  nt:f-document-node-with-document-element(document {<a>is not b</a>})
};

declare
    %test:pending("BaseX returns an empty-sequence, but Saxon raises the error XPTY0004... which should it be?")
    %test:assertError("XPTY0004")
function nt:test-document-node-with-document-element-invalid-1() {
  nt:f-document-node-with-document-element(document { () })
};

declare
    %test:pending("BaseX returns an empty-sequence, but Saxon raises the error XPTY0004... which should it be?")
    %test:assertError("XPTY0004")
function nt:test-document-node-with-document-element-invalid-2() {
  nt:f-document-node-with-document-element(document {<!-- comment -->})
};

declare
    %test:assertEquals("is not b")
function nt:test-document-node-with-named-document-element-valid() {
  nt:f-document-node-with-named-document-element(document {<a>is not b</a>})
};

declare
    %test:assertError("XPTY0004")
function nt:test-document-node-with-named-document-element-invalid-1() {
  nt:f-document-node-with-named-document-element(document { () })
};

declare
    %test:assertError("XPTY0004")
function nt:test-document-node-with-named-document-element-invalid-2() {
  nt:f-document-node-with-named-document-element(document {<!-- comment -->})
};

declare
    %test:assertError("XPTY0004")
function nt:test-document-node-with-named-document-element-invalid-3() {
  nt:f-document-node-with-named-document-element(document {<b>is b</b>})
};
