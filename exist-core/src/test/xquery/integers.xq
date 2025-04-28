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

module namespace it = "http://exist-db.org/xquery/test/integer-tests";
 
declare namespace test = "http://exist-db.org/xquery/xqsuite";

(:~
 : Tests for integers.
 :
 : Note, in some tests the purpose of the nested HoF is to circumvent
 : the Automatic Type Conversion provided by XQSuite,
 : see: http://www.exist-db.org/exist/apps/doc/xqsuite#type-conversion
 :)

declare
    %test:args("-1")
    %test:assertError("err:XPTY0004")
    %test:args("0")
    %test:assertError("err:XPTY0004")
    %test:args("1")
    %test:assertError("err:XPTY0004")
function it:function-signature-negative-integer-from-string($test-value as xs:string) {
    function($negative-integer as xs:negativeInteger) {
        $negative-integer
    }($test-value)
};

declare
    %test:args(0)
    %test:assertError("err:XPTY0004")
    %test:args(1)
    %test:assertError("err:XPTY0004")
function it:function-signature-negative-integer-from-integer($test-value as xs:integer) {
    function($negative-integer as xs:negativeInteger) {
        $negative-integer
    }($test-value)
};

declare 
    %test:args("-1")
    %test:assertError("err:XPTY0004")
    %test:args("0")
    %test:assertError("err:XPTY0004")
    %test:args("1")
    %test:assertError("err:XPTY0004")
function it:function-signature-positive-integer-from-string($test-value as xs:string) {
    function($positive-integer as xs:positiveInteger) {
        $positive-integer
    }($test-value)
};

declare
    %test:args(0)
    %test:assertError("err:XPTY0004")
    %test:args(1)
    %test:assertError("err:XPTY0004")
function it:function-signature-positive-integer-from-integer($test-value as xs:integer) {
    function($positive-integer as xs:positiveInteger) {
        $positive-integer
    }($test-value)
};

declare
    %test:args("-1")
    %test:assertEquals("-1")
    %test:args("0")
    %test:assertError("err:FORG0001")
    %test:args("1")
    %test:assertError("err:FORG0001")
function it:string-cast-as-negative-integer($string as xs:string) as xs:negativeInteger {
    $string cast as xs:negativeInteger
};

declare 
    %test:args("-1")
    %test:assertEquals("-1")
    %test:args(0)
    %test:assertError("err:FORG0001")
    %test:args(1)
    %test:assertError("err:FORG0001")
function it:integer-cast-as-negative-integer($integer as xs:integer) as xs:negativeInteger {
    $integer cast as xs:negativeInteger
};

declare
    %test:args("-1")
    %test:assertError("err:FORG0001")
    %test:args("0")
    %test:assertError("err:FORG0001")
    %test:args("1")
    %test:assertEquals(1)
function it:string-cast-as-positive-integer($string as xs:integer) as xs:positiveInteger {
    $string cast as xs:positiveInteger
};

declare 
    %test:args("-1")
    %test:assertError("err:FORG0001")
    %test:args(0)
    %test:assertError("err:FORG0001")
    %test:args(1)
    %test:assertEquals(1)
function it:integer-cast-as-positive-integer($integer as xs:integer) as xs:positiveInteger {
    $integer cast as xs:positiveInteger
};

declare
    %test:args("-1")
    %test:assertTrue
    %test:args("0")
    %test:assertFalse
    %test:args("1")
    %test:assertFalse
function it:string-castable-as-negative-integer($string as xs:string) as xs:boolean {
    $string castable as xs:negativeInteger
};

declare 
    %test:args("-1")
    %test:assertTrue
    %test:args(0)
    %test:assertFalse
    %test:args(1)
    %test:assertFalse
function it:integer-castable-as-negative-integer($integer as xs:integer) as xs:boolean {
    $integer castable as xs:negativeInteger
};

declare
    %test:args("-1")
    %test:assertFalse
    %test:args("0")
    %test:assertFalse
    %test:args("1")
    %test:assertTrue
function it:string-castable-as-positive-integer($string as xs:integer) as xs:boolean {
    $string castable as xs:positiveInteger
};

declare
    %test:args("-1")
    %test:assertFalse
    %test:args(0)
    %test:assertFalse
    %test:args(1)
    %test:assertTrue
function it:integer-castable-as-positive-integer($integer as xs:integer) as xs:boolean {
    $integer castable as xs:positiveInteger
};

declare
    %test:args("-1")
    %test:assertFalse
    %test:args("0")
    %test:assertFalse
    %test:args("1")
    %test:assertFalse
function it:string-instance-of-negative-integer($string as xs:string) as xs:boolean {
    $string instance of xs:negativeInteger
};

declare 
    %test:args("-1")
    %test:assertFalse
    %test:args(0)
    %test:assertFalse
    %test:args(1)
    %test:assertFalse
function it:integer-instance-of-negative-integer($integer as xs:integer) as xs:boolean {
    $integer instance of xs:negativeInteger
};

declare
    %test:args("-1")
    %test:assertFalse
    %test:args("0")
    %test:assertFalse
    %test:args("1")
    %test:assertFalse
function it:string-instance-of-positive-integer($string as xs:string) as xs:boolean  {
    $string instance of xs:positiveInteger
};

declare 
    %test:args("-1")
    %test:assertFalse
    %test:args(0)
    %test:assertFalse
    %test:args(1)
    %test:assertFalse
function it:integer-instance-of-positive-integer($integer as xs:integer) as xs:boolean  {
    $integer instance of xs:positiveInteger
};

declare
    %test:assertEquals(1)
function it:cast-as-union-type()  {
    (1 cast as xs:numeric) cast as xs:int
};

declare
    %test:assertTrue
function it:castable-as-union-type() as xs:boolean  {
    1 castable as xs:numeric
};