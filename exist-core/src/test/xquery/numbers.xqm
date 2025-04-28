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

module namespace nums = "http://exist-db.org/xquery/test/numbers";

declare namespace test="http://exist-db.org/xquery/xqsuite";


declare
    %test:args(10000000000000001,  10000000000000002)
    %test:assertFalse
    %test:args(100000000000000001, 100000000000000002)
    %test:assertFalse
    %test:args(100000000000000010, 100000000000000020)
    %test:assertFalse
    %test:args(100000000000000100, 100000000000000200)
    %test:assertFalse
    %test:args(310000000000920000300, 310000000000920000200)
    %test:assertFalse
function nums:compare-eq($a, $b) {
    $a eq $b
};

declare
  %test:assertEquals(
      310000000000920000200,
      310000000000920000300,
      310000000000930001700,
      310000000000930001800,
      310000000000930002800,
      310000000000930003800,
      310000000000930003900,
      310000000000930004900,
      310000000000970000300,
      310000000000970000400
  )
function nums:order() {
  let $data := (
    310000000000920000300,
    310000000000930001800,
    310000000000930003900,
    310000000000970000400,
    310000000000930002800,
    310000000000920000200,
    310000000000930001700,
    310000000000930004900,
    310000000000930003800,
    310000000000970000300
  )
  return
    for $i in $data
    order by $i
    return $i
};
