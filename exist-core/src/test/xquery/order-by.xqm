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

module namespace obt = "http://exist-db.org/xquery/test/order-by";

declare namespace test = "http://exist-db.org/xquery/xqsuite";


declare variable $obt:collections :=
    <collection name="db" size="2">
        <collection name="test" size="3">
            <collection name="test2" size="2"/>
            <collection name="test1" size="1"/>
            <collection name="test3" size="4"/>
        </collection>
        <collection name="system" size="2">
            <collection name="security" size="2"/>
            <collection name="config" size="2"/>
        </collection>
    </collection>;


declare
    %test:assertError("err:XPTY0004")
function obt:non-comparable-types() {
  for $v in (xs:untypedAtomic("2017-09-18"), xs:date("1999-12-17"))
  order by $v
  return $v
};

declare
    %test:assertEquals("http://hello.com", "https://hello.com")
function obt:comparable-types() {
  for $v at $i in (xs:anyURI("http://hello.com"), xs:string("https://hello.com"))
  order by $v, $i
  return $v
};

declare
    %test:assertXPath("fn:count($result) eq 2 and $result[1] cast as xs:string eq 'NaN' and $result[2] eq xs:double(1)")
function obt:comparable-types-nan() as xs:double+ {
  let $numbers := (1, xs:double("NaN"))
  for $i in $numbers order by $i empty least
  return
    $i
};

declare
    %test:assertXPath("fn:count($result) eq 3 and $result[1] cast as xs:string eq '-INF' and $result[2] eq xs:double(1) and $result[3] cast as xs:string eq 'INF'")
function obt:comparable-types-inf() as xs:double+ {
  let $numbers := (1, xs:double("-INF"), xs:double("INF"))
  for $i in $numbers order by $i empty least
  return
    $i
};

declare %private function obt:list-ordered($collections as element()*) {
  for $collection in $collections
  where $collection/@size > 1
  order by $collection/@name ascending
  return
    <col>
    { $collection/@name, obt:list-ordered($collection/*) }
    </col>
};

declare
    %test:assertEquals(
        '<col name="db"><col name="system"><col name="config"/><col name="security"/></col><col name="test"><col name="test2"/><col name="test3"/></col></col>')
function obt:recursive-orderby() {
    obt:list-ordered($obt:collections)
};
