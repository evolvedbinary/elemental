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
 : Tests for the op:numeric-divide operator.
 :)
module namespace ond = "http://exist-db.org/test/op-numeric-divide";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("0.9999999957802023")
function ond:numeric-divide-float-double($f as xs:float, $d as xs:double) {
    $f div $d
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("1.0000000042197978")
function ond:numeric-divide-double-float($d as xs:double, $f as xs:float) {
    $d div $f
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("1")
function ond:numeric-divide-double-decimal($d as xs:double, $dec as xs:decimal) {
    $d div $dec
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("1")
function ond:numeric-divide-decimal-double($dec as xs:decimal, $d as xs:double) {
    $dec div $d
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("1")
function ond:numeric-divide-decimal-float($dec as xs:decimal, $f as xs:float) {
    $dec div $f
};

declare
    %test:args("1.13", "1.13")
    %test:assertEquals("1")
function ond:numeric-divide-float-decimal($f as xs:float, $dec as xs:decimal) {
    $f div $dec
};
