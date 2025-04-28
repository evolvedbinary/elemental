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

module namespace fr="http://exist-db.org/xquery/test/fn-round";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare
    %test:args("2.3")
    %test:assertEquals("2.0")
    %test:args("2.5")
    %test:assertEquals("3.0")
    %test:args("-2.5")
    %test:assertEquals("-2.0")
    %test:args("-2.51")
    %test:assertEquals("-3.0")
function fr:round-double($number as xs:double) {
    fn:round($number)
};

declare
    %test:args(2.3, 1)
    %test:assertEquals(2.3)
    %test:args(2.5, 1)
    %test:assertEquals(2.5)
    %test:args("-2.5", 1)
    %test:assertEquals("-2.5")
    %test:args("-2.51", 1)
    %test:assertEquals("-2.5")
    %test:args("-2.51", 2)
    %test:assertEquals("-2.51")
function fr:round-double-precision($number as xs:double, $precision as xs:integer) {
    fn:round($number, $precision)
};

declare
    %test:args("2.5")
    %test:assertEquals("3.0")
    %test:args("2.4999")
    %test:assertEquals("2.0")
    %test:args("-2.5")
    %test:assertEquals("-2.0")
function fr:round-specification-examples-1($number as xs:double) {
    fn:round($number)
};

declare
    %test:args("1.125", 2)
    %test:assertEquals("1.13")
function fr:round-specification-examples-2-decimal($number as xs:decimal, $precision as xs:integer) {
    fn:round($number, $precision)
};

declare
    %test:args("1.125", 2)
    %test:assertEquals("1.13")
    %test:args("1.125", 2)
    %test:assertEquals("1.13")
    %test:args("8452", "-2")
    %test:assertEquals("8500")
    %test:args("3.1415e0", "2")
    %test:assertEquals("3.14e0")
function fr:round-specification-examples-2($number as xs:double, $precision as xs:integer) {
    fn:round($number, $precision)
};

declare
    %test:args("1.125", 2)
    %test:assertEquals("1.12")
    %test:args("1.135", 2)
    %test:assertEquals("1.14")
    %test:args("-1.125", 2)
    %test:assertEquals("-1.12")
    %test:args("-1.135", 2)
    %test:assertEquals("-1.14")
    %test:args(3.567812e+3, 2)
    %test:assertEquals(3567.81e0)
    %test:args(4.7564e-3, 2)
    %test:assertEquals(0.0e0)
    %test:args(35612.25, "-2")
    %test:assertEquals(35600)
function fr:round-half-to-even-precision($number as xs:double, $precision as xs:integer) {
    fn:round-half-to-even($number, $precision)
};

declare
    %test:args("0.5")
    %test:assertEquals(0)
    %test:args("1.5")
    %test:assertEquals(2.0)
    %test:args("2.5")
    %test:assertEquals(2.0)
function fr:round-half-to-even($number as xs:double) {
    fn:round-half-to-even($number)
};

declare
    %test:args("3.567812E+3", 4294967296)
    %test:assertEquals("3567.812")
function fr:round-half-to-even-high-precision($number as xs:double, $precision as xs:integer) {
    fn:round-half-to-even($number, $precision)
};

declare
    %test:args("-0.05", 1)
    %test:assertEquals("-0.1")
function fr:round-half-to-even-inexact-halfway-float($number as xs:float, $precision as xs:integer) {
    fn:round-half-to-even($number, $precision)
};

declare
    %test:args("-0.41",1)
    %test:assertEquals("-0.4")
    %test:args("-0.43",0)
    %test:assertEquals("-0")
    %test:args("-0.43",0)
    %test:assertEquals("0")
    %test:args("-0.43",0)
    %test:assertEquals(0)
function fr:round-negative-zero($number as xs:decimal, $precision as xs:integer) {
    fn:round($number, $precision)
};

declare
    %test:args("-0.41",1)
    %test:assertEquals("-0.4")
    %test:args("-0.41",0)
    %test:assertEquals("-0")
function fr:round-negative-zero-double($number as xs:double, $precision as xs:integer) {
    fn:round($number, $precision)
};

declare
    %test:args("-0.41",1)
    %test:assertEquals("-0.4")
    %test:args("-0.41",0)
    %test:assertEquals("-0")
function fr:round-negative-zero-float($number as xs:float, $precision as xs:integer) {
    fn:round($number, $precision)
};

declare
    %test:args("INF")
    %test:assertEquals("INF")
    %test:args("-INF")
    %test:assertEquals("-INF")
function fr:round-inf-float($number as xs:float) {
    fn:round($number)
};

declare
    %test:args("INF")
    %test:assertEquals("INF")
    %test:args("-INF")
    %test:assertEquals("-INF")
function fr:round-inf-double($number as xs:double) {
    fn:round($number)
};