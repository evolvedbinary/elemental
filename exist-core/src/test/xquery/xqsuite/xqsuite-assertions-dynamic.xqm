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
 : Dynamic Tests for XQSuite assertions.
 :
 : @author Adam Retter
 :)
module namespace xqtad = "http://exist-db.org/xquery/xqsuite/test/assertions-dynamic";

(:~
 : Resource functions to test through XQSuite.
 :)
declare namespace xqtares = "http://exist-db.org/xquery/xqsuite/test/assertions/resources";

import module namespace test = "http://exist-db.org/xquery/xqsuite"
    at "resource:org/exist/xquery/lib/xqsuite/xqsuite.xql";

declare
    %private
function xqtad:get-resource-function($name as xs:string, $arity as xs:integer) as function() as item()* {
  let $resource-functions :=
      fn:load-xquery-module(
          "http://exist-db.org/xquery/xqsuite/test/assertions/resources",
          map {
            "location-hints": "xqsuite-assertions.resources.xqm.ignore"
          }
      )?functions
  let $resource-function := $resource-functions(xs:QName("xqtares:" || $name))($arity)
  return
    $resource-function
};

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtad:test-assertTrue-assertion-when-true() {
  let $fn-to-test := xqtad:get-resource-function("assertTrue-assertion-when-true", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertTrue-assertion-when-false() {
  let $fn-to-test := xqtad:get-resource-function("assertTrue-assertion-when-false", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 1, 0)
function xqtad:test-assertTrue-assertion-when-error() {
  let $fn-to-test := xqtad:get-resource-function("assertTrue-assertion-when-error", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertFalse-assertion-when-true() {
  let $fn-to-test := xqtad:get-resource-function("assertFalse-assertion-when-true", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtad:test-assertFalse-assertion-when-false() {
  let $fn-to-test := xqtad:get-resource-function("assertFalse-assertion-when-false", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 1, 0)
function xqtad:test-assertFalse-assertion-when-error() {
  let $fn-to-test := xqtad:get-resource-function("assertFalse-assertion-when-error", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtad:test-assertError-assertion-when-error() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-error", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-unexpected-error() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-unexpected-error", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 0, 0, 0)
function xqtad:test-assertError-assertion-when-expected-error() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-expected-error", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-different-error() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-different-error", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-true() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-true", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-false() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-false", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-empty() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-empty", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-non-empty() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-non-empty", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};


declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-element() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-element", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-string() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-string", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};

declare
    %test:assertEquals(1, 1, 0, 0)
function xqtad:test-assertError-assertion-when-integer() {
  let $fn-to-test := xqtad:get-resource-function("assertError-assertion-when-integer", 0)
  return
    let $testsuite-result as element(testsuite) := test:suite($fn-to-test)/testsuite
    return
        $testsuite-result/(@tests|@failures|@errors|@pending)/xs:integer(.)
};
