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

(:~
 : Inline Tests for XQSuite assertions.
 :
 : @author Adam Retter
 :)
module namespace xqtai = "http://exist-db.org/xquery/xqsuite/test/assertions-inline";

(:~
 : Resource functions to test through XQSuite.
 :)
import module namespace xqtares = "http://exist-db.org/xquery/xqsuite/test/assertions/resources"
    at "xqsuite-assertions.resources.xqm.ignore";

import module namespace test = "http://exist-db.org/xquery/xqsuite"
    at "resource:org/exist/xquery/lib/xqsuite/xqsuite.xql";

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtai:test-assertTrue-assertion-when-true() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertTrue-assertion-when-true#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertTrue-assertion-when-false() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertTrue-assertion-when-false#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 1, 0)
function xqtai:test-assertTrue-assertion-when-error() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertTrue-assertion-when-error#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertFalse-assertion-when-true() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertFalse-assertion-when-true#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtai:test-assertFalse-assertion-when-false() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertFalse-assertion-when-false#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 1, 0)
function xqtai:test-assertFalse-assertion-when-error() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertFalse-assertion-when-error#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtai:test-assertError-assertion-when-error() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-error#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-unexpected-error() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-unexpected-error#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtai:test-assertError-assertion-when-expected-error() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-expected-error#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-different-error() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-different-error#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-true() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-true#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-false() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-false#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-empty() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-empty#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-non-empty() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-non-empty#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-element() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-element#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-string() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-string#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtai:test-assertError-assertion-when-integer() {
  let $testsuite-result as element(testsuite) := test:suite(xqtares:assertError-assertion-when-integer#0)/testsuite
  return
      $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};
