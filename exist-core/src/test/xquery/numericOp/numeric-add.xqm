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
 : Tests for the op:numeric-add operator.
 :)
module namespace ona = "http://exist-db.org/test/op-numeric-add";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("2.2599999952316283")
function ona:numeric-add-float-double($f as xs:float, $d as xs:double) {
    $f + $d
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("2.2599999952316283")
function ona:numeric-add-double-float($d as xs:double, $f as xs:float) {
    $d + $f
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("2.26")
function ona:numeric-add-double-decimal($d as xs:double, $dec as xs:decimal) {
    $d + $dec
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("2.26")
function ona:numeric-add-decimal-double($dec as xs:decimal, $d as xs:double) {
    $dec + $d
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("2.26")
function ona:numeric-add-decimal-float($dec as xs:decimal, $f as xs:float) {
    $dec + $f
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("2.26")
function ona:numeric-add-float-decimal($f as xs:float, $dec as xs:decimal) {
    $f + $dec
};
