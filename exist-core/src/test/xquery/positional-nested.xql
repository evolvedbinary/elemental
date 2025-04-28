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

declare variable $npt:TEST_COLLECTION_NAME := "test-positional-nested";
declare variable $npt:TEST_COLLECTION_URI := "/db/" || $npt:TEST_COLLECTION_NAME;
declare variable $npt:TEST_DOC_NAME := "test.xml";
declare variable $npt:TEST_DOC_URI := $npt:TEST_COLLECTION_URI || "/" || $npt:TEST_DOC_NAME;

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
    xmldb:create-collection("/db", $npt:TEST_COLLECTION_NAME),
    xmldb:store($npt:TEST_COLLECTION_URI, $npt:TEST_DOC_NAME, $npt:DATA)
};

declare
    %test:tearDown
function npt:cleanup() {
    xmldb:remove($npt:TEST_COLLECTION_URI)
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-memory-1() {
    $npt:DATA//c[../preceding-sibling::a]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-memory-2() {
    $npt:DATA//c[parent::node()/preceding-sibling::a]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-memory-3() {
    $npt:DATA//c/..[preceding-sibling::a]/c
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-memory-4() {
    $npt:DATA//c/parent::node()[preceding-sibling::a]/c
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-database-1() {
    doc($npt:TEST_DOC_URI)//c[../preceding-sibling::a]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-database-2() {
    doc($npt:TEST_DOC_URI)//c[parent::node()/preceding-sibling::a]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-database-3() {
    doc($npt:TEST_DOC_URI)//c/..[preceding-sibling::a]/c
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-database-4() {
    doc($npt:TEST_DOC_URI)//c/parent::node()[preceding-sibling::a]/c
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-memory-predicate() {
    $npt:DATA//c[../preceding-sibling::a[1]]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-database-predicate() {
    doc($npt:TEST_DOC_URI)//c[../preceding-sibling::a[1]]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-memory-position() {
    $npt:DATA//c[../preceding-sibling::a[position() eq 1]]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function npt:in-database-position() {
    doc($npt:TEST_DOC_URI)//c[../preceding-sibling::a[position() eq 1]]
};

declare
    %test:assertEquals("<c>correct</c>")
function npt:in-memory-predicate-and-path() {
    $npt:DATA//c[../preceding-sibling::a[1]/b = 'B1']
};

declare
    %test:assertEquals("<c>correct</c>")
function npt:in-database-predicate-and-path() {
    doc($npt:TEST_DOC_URI)//c[../preceding-sibling::a[1]/b = 'B1']
};

declare
    %test:assertEquals("<c>correct</c>")
function npt:in-memory-position-and-path() {
    $npt:DATA//c[../preceding-sibling::a[position() eq 1]/b = 'B1']
};

declare
    %test:assertEquals("<c>correct</c>")
function npt:in-database-position-and-path() {
    doc($npt:TEST_DOC_URI)//c[../preceding-sibling::a[position() eq 1]/b = 'B1']
};
