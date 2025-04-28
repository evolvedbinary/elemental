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
 : Test for binary value lifetimes.
 : This also checks that the escape analysis is correct for LetExpr calling XQueryContext#popLocalVariables.
 :
 : @author Adam Retter
 :)
module namespace bv = "http://exist-db.org/xquery/test/binary-value";

import module namespace test = "http://exist-db.org/xquery/xqsuite";
import module namespace util ="http://exist-db.org/xquery/util";
import module namespace xmldb ="http://exist-db.org/xquery/xmldb";

declare variable $bv:db := "/db";
declare variable $bv:src-collection-name := "test-binary-value-src";
declare variable $bv:dst-collection-name := "test-binary-value-dst";
declare variable $bv:doc1 := "doc1.bin";
declare variable $bv:doc2 := "doc2.bin";
declare variable $bv:doc3 := "doc3.bin";
declare variable $bv:doc4 := "doc4.bin";
declare variable $bv:doc5 := "doc5.bin";
declare variable $bv:doc6 := "doc6.bin";
declare variable $bv:doc7 := "doc7.bin";
declare variable $bv:doc8 := "doc8.bin";
declare variable $bv:doc9 := "doc9.bin";
declare variable $bv:doc10 := "doc10.bin";
declare variable $bv:bin := fn:current-dateTime() cast as xs:string;

declare
    %test:setUp
function bv:setup() {
  let $src-collection := xmldb:create-collection($bv:db, $bv:src-collection-name)
  let $_ := xmldb:create-collection($bv:db, $bv:dst-collection-name)
  for $doc-name in ($bv:doc1, $bv:doc2, $bv:doc3, $bv:doc4, $bv:doc5, $bv:doc6, $bv:doc7, $bv:doc8, $bv:doc9, $bv:doc10)
  return
    xmldb:store($src-collection, $doc-name, $bv:bin, "application/octet-stream")
};

declare
    %test:tearDown
function bv:teardown() {
  xmldb:remove($bv:db || "/" || $bv:src-collection-name)
};

declare
  %private
function bv:internal-escape-item-in-map() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc1)
  let $m := map { "content": $b }
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc1.bin")
function bv:escape-item-in-map() {
  let $m := bv:internal-escape-item-in-map()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc1, $m?content, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-filtered-item-in-map() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc2)
  let $m := map { "content": $b[1] }
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc2.bin")
function bv:escape-filtered-item-in-map() {
  let $m := bv:internal-escape-filtered-item-in-map()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc2, $m?content, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-sequence-item-in-map() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc3)
  let $m := map { "content": ($b, fn:true()) }
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc3.bin")
function bv:escape-sequence-item-in-map() {
  let $m := bv:internal-escape-sequence-item-in-map()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc3, $m?content[1], "application/octet-stream")
};

declare
  %private
function bv:internal-escape-map-item-in-map() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc4)
  let $m := map { "content": map { "content": $b } }
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc4.bin")
function bv:escape-map-item-in-map() {
  let $m := bv:internal-escape-map-item-in-map()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc4, $m?content?content, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-array-item-in-map() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc5)
  let $m := map { "content": [ $b ] }
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc5.bin")
function bv:escape-array-item-in-map() {
  let $m := bv:internal-escape-array-item-in-map()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc5, $m?content?1, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-array-item-in-array() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc6)
  let $m := [ [ $b ] ]
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc6.bin")
function bv:escape-array-item-in-array() {
  let $m := bv:internal-escape-array-item-in-array()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc6, $m?1?1, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-map-item-in-array() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc7)
  let $m := [ map { "content": $b } ]
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc7.bin")
function bv:escape-map-item-in-array() {
  let $m := bv:internal-escape-map-item-in-array()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc7, $m?1?content, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-item-in-array() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc8)
  let $m := [ $b ]
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc8.bin")
function bv:escape-item-in-array() {
  let $m := bv:internal-escape-item-in-array()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc8, $m?1, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-filtered-item-in-array() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc9)
  let $m := [ $b[1] ]
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc9.bin")
function bv:escape-filtered-item-in-array() {
  let $m := bv:internal-escape-filtered-item-in-array()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc9, $m?1, "application/octet-stream")
};

declare
  %private
function bv:internal-escape-sequence-in-array() {
  let $b := util:binary-doc($bv:db || "/" || $bv:src-collection-name || "/" || $bv:doc10)
  let $m := [ ($b, fn:true()) ]
  return
    $m
};

declare
  %test:assertEquals("/db/test-binary-value-dst/doc10.bin")
function bv:escape-sequence-in-array() {
  let $m := bv:internal-escape-sequence-in-array()
  let $dst-collection := $bv:db || "/" || $bv:dst-collection-name
  return
    xmldb:store($dst-collection, $bv:doc10, $m?1[1], "application/octet-stream")
};
