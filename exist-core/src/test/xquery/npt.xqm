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

module namespace npt="http://exist-db.org/test/nested-positional-predicate";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $npt:DATA :=
    document {
        <xml>
            <a>
                <b>B1</b>
            </a>
            <a>
                <b>B2</b>
                <c>correct</c>
            </a>
            <a>
                <b>B3</b>
                <c>wrong</c>
            </a>
        </xml>
    };

declare
    %test:setUp
function npt:setup() {
    xmldb:create-collection("/db", "test"),
    xmldb:store("/db/test", "test.xml", $npt:DATA)
};

declare
    %test:tearDown
function npt:cleanup() {
    xmldb:remove("/db/test")
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npt:in-memory() {
    <result>{$npt:DATA//c[../preceding-sibling::a]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npt:in-database() {
    <result>{doc("/db/test/test.xml")//c[../preceding-sibling::a]}</result>
};


declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npt:in-memory-predicate() {
    <result>{$npt:DATA//c[../preceding-sibling::a[1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npt:in-database-predicate() {
    <result>{doc("/db/test/test.xml")//c[../preceding-sibling::a[1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npt:in-memory-position() {
    <result>{$npt:DATA//c[../preceding-sibling::a[position() eq 1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npt:in-database-position() {
    <result>{doc("/db/test/test.xml")//c[../preceding-sibling::a[position() eq 1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npt:in-memory-predicate-and-path() {
    <result>{$npt:DATA//c[../preceding-sibling::a[1]/b = 'B1']}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npt:in-database-predicate-and-path() {
    <result>{doc("/db/test/test.xml")//c[../preceding-sibling::a[1]/b = 'B1']}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npt:in-memory-position-and-path() {
    <result>{$npt:DATA//c[../preceding-sibling::a[position() eq 1]/b = 'B1']}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npt:in-database-position-and-path() {
    <result>{doc("/db/test/test.xml")//c[../preceding-sibling::a[position() eq 1]/b = 'B1']}</result>
};
