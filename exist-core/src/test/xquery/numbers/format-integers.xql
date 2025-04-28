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

module namespace fi="http://exist-db.org/xquery/test/format-integer";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare
    %test:args("#0")
    %test:assertEmpty
    %test:args("#0.0")
    %test:assertEmpty
    %test:args("#0.00")
    %test:assertEmpty
    %test:args("#0.000")
    %test:assertEmpty
    %test:args("#0.0#")
    %test:assertEmpty
function fi:format-empty($picture as xs:string) {
    format-integer((), $picture)
};

declare
    %test:args("#0")
    %test:assertEquals("0")
    %test:args("#0.0")
    %test:assertEquals("0.0")
    %test:args("#0.00")
    %test:assertEquals("0.00")
    %test:args("#0.000")
    %test:assertEquals("0.000")
function fi:format-zeros($picture as xs:string) {
    format-integer(0, $picture)
};

declare
    %test:args("#0", 1)
    %test:assertEquals("1")
    %test:args("#00", 1)
    %test:assertEquals("01")
    %test:args("#0.00", 1)
    %test:assertEquals("0.01")
    %test:args("#0.000", 1)
    %test:assertEquals("0.001")
function fi:format-values($picture as xs:string, $value as xs:integer) {
    format-integer($value, $picture)
};

declare
    %test:args("#0#,555", 10000)
    %test:assertError("FODF1310")
    %test:args("#3||456", 1)
    %test:assertError("FODF1310")
    %test:args("|1", 1)
    %test:assertError("FODF1310")
    %test:args("1+", 1)
    %test:assertError("FODF1310")
    %test:args("1#", 1)
    %test:assertError("FODF1310")
    %test:args("##|3#|45", 1)
    %test:assertError("FODF1310")
    %test:args("12,#45", 1)
    %test:assertError("FODF1310")
function fi:invalid-pictures($picture as xs:string, $value as xs:integer) {
    format-integer($value, $picture)
};

declare
    %test:args("##|#3|456", 5)
    %test:assertEquals("0|005")
    %test:args("##|#3|45", 5)
    %test:assertEquals("0|05")
    %test:args("##|#3|45", 56789)
    %test:assertEquals("5|67|89")
function fi:separator-pictures($picture as xs:string, $value as xs:integer) {
    format-integer($value, $picture)
};

declare
    %test:args("#|##3|456", "-123456789")
    %test:assertEquals("-123|456|789")
    %test:args("#|##3|456", "-0")
    %test:assertEquals("0|000")
function fi:format-negatives($picture as xs:string, $value as xs:integer) {
    format-integer($value, $picture)
};

