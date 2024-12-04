(:
 : eXist-db Open Source Native XML Database
 : Copyright (C) 2001 The eXist-db Authors
 :
 : info@exist-db.org
 : http://www.exist-db.org
 :
 : This library is free software; you can redistribute it and/or
 : modify it under the terms of the GNU Lesser General Public
 : License as published by the Free Software Foundation; either
 : version 2.1 of the License, or (at your option) any later version.
 :
 : This library is distributed in the hope that it will be useful,
 : but WITHOUT ANY WARRANTY; without even the implied warranty of
 : MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 : Lesser General Public License for more details.
 :
 : You should have received a copy of the GNU Lesser General Public
 : License along with this library; if not, write to the Free Software
 : Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 :)
xquery version "3.1";

module namespace npp="http://exist-db.org/test/nested-positional-predicate";

declare namespace xmldb="http://exist-db.org/xquery/xmldb";
declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $npp:TEST_COLLECTION_NAME := "test-positional-nested";
declare variable $npp:TEST_COLLECTION_URI := "/db/" || $npp:TEST_COLLECTION_NAME;
declare variable $npp:TEST_DOC_NAME := "test.xml";
declare variable $npp:TEST_DOC_URI := $npp:TEST_COLLECTION_URI || "/" || $npp:TEST_DOC_NAME;

declare variable $npp:DATA :=
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
function npp:setup() {
    xmldb:create-collection("/db", $npp:TEST_COLLECTION_NAME),
    xmldb:store($npp:TEST_COLLECTION_URI, $npp:TEST_DOC_NAME, $npp:DATA)
};

declare
    %test:tearDown
function npp:cleanup() {
    xmldb:remove($npp:TEST_COLLECTION_URI)
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-memory-1() {
    <result>{$npp:DATA//c[../preceding-sibling::a]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-memory-2() {
    <result>{$npp:DATA//c[parent::node()/preceding-sibling::a]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-memory-3() {
    <result>{$npp:DATA//c/..[preceding-sibling::a]/c}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-memory-4() {
    <result>{$npp:DATA//c/parent::node()[preceding-sibling::a]/c}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-database-1() {
    <result>{doc($npp:TEST_DOC_URI)//c[../preceding-sibling::a]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-database-2() {
    <result>{doc($npp:TEST_DOC_URI)//c[parent::node()/preceding-sibling::a]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-database-3() {
    <result>{doc($npp:TEST_DOC_URI)//c/..[preceding-sibling::a]/c}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-database-4() {
    <result>{doc($npp:TEST_DOC_URI)//c/parent::node()[preceding-sibling::a]/c}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-memory-predicate() {
    <result>{$npp:DATA//c[../preceding-sibling::a[1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-database-predicate() {
    <result>{doc($npp:TEST_DOC_URI)//c[../preceding-sibling::a[1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-memory-position() {
    <result>{$npp:DATA//c[../preceding-sibling::a[position() eq 1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c><c>wrong</c></result>")
function npp:in-database-position() {
    <result>{doc($npp:TEST_DOC_URI)//c[../preceding-sibling::a[position() eq 1]]}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npp:in-memory-predicate-and-path() {
    <result>{$npp:DATA//c[../preceding-sibling::a[1]/b = 'B1']}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npp:in-database-predicate-and-path() {
    <result>{doc($npp:TEST_DOC_URI)//c[../preceding-sibling::a[1]/b = 'B1']}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npp:in-memory-position-and-path() {
    <result>{$npp:DATA//c[../preceding-sibling::a[position() eq 1]/b = 'B1']}</result>
};

declare
    %test:assertEquals("<result><c>correct</c></result>")
function npp:in-database-position-and-path() {
    <result>{doc($npp:TEST_DOC_URI)//c[../preceding-sibling::a[position() eq 1]/b = 'B1']}</result>
};
