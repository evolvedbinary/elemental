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
xquery version "3.0";

module namespace axes="http://exist-db.org/xquery/test/axes";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $axes:NESTED_DIVS :=
    <body>
        <div>
            <head>1</head>
            <div>
                <head>2</head>
                <div>
                    <head>3</head>
                </div>
            </div>
            <div>
                <head>4</head>
                <div>
                    <head>5</head>
                </div>
                <div>
                    <head>6</head>
                    <div>
                        <head>7</head>
                    </div>
                </div>
            </div>
        </div>
    </body>;

declare variable $axes:in-mem-doc1 :=
    document {
        <root>
            <a/>
            <b/>
        </root>
    };

declare variable $axes:in-mem-doc2 :=
    document {
        <root>
            <a/>
        </root>
    };

declare variable $axes:pi-doc :=
    document {
        <?testpi?>,
        <root/>
    };

declare variable $axes:siblings :=
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
;

declare variable $axes:flat :=
    <xml>
        <a>A1</a>
        <b>B1</b>
        <c>C1</c>
        <b>B2</b>
        <a>A2</a>
        <d>D1</d>
        <c>C2</c>
    </xml>
;

declare variable $axes:cousins :=
    <xml>
        <alpha>
            <a>alphaA1</a>
            <b>alphaB1</b>
            <c>alphaC1</c>
            <b>alphaB2</b>
            <a>alphaA2</a>
            <d>alphaD1</d>
            <c>alphaC2</c>
        </alpha>
        <main>
             <a>A1</a>
             <b>B1</b>
             <c>C1</c>
             <b>B2</b>
             <a>A2</a>
             <d>D1</d>
             <c>C2</c>
        </main>
        <beta>
            <a>betaA1</a>
            <b>betaB1</b>
            <c>betaC1</c>
            <b>betaB2</b>
            <a>betaA2</a>
            <d>betaD1</d>
            <c>betaC2</c>
        </beta>
    </xml>
;

declare
    %test:setUp
function axes:setup() {
    xmldb:create-collection("/db", "axes-test"),
    xmldb:store("/db/axes-test", "test.xml", $axes:NESTED_DIVS),
    xmldb:store("/db/axes-test", "doc1.xml", $axes:in-mem-doc1),
    xmldb:store("/db/axes-test", "doc2.xml", $axes:in-mem-doc2),
    xmldb:store("/db/axes-test", "pi.xml", $axes:pi-doc),
    xmldb:store("/db/axes-test", "siblings.xml", $axes:siblings),
    xmldb:store("/db/axes-test", "flat.xml", $axes:flat),
    xmldb:store("/db/axes-test", "cousins.xml", $axes:cousins)
};

declare
    %test:tearDown
function axes:cleanup() {
    xmldb:remove("/db/axes-test")
};

(: ---------------------------------------------------------------
 : Descendant axis: check if nested elements are handled properly.
 : Wrong evaluation may lead to duplicate nodes being returned.
   --------------------------------------------------------------- :)

declare
    %test:assertEquals(6, 6)
function axes:descendant-axis-nested() {
    let $node := doc("/db/axes-test/test.xml")/body
    return (
        count($node/descendant::div/descendant::div),
        count($node//div//div)
    )
};

declare
    %test:assertEquals("<head>1</head>")
function axes:descendant-axis-except-nested1() {
    let $node := doc("/db/axes-test/test.xml")/body
    return
        ($node//div except $node//div//div)/head
};

declare
    %test:assertEquals("<head>2</head>", "<head>4</head>")
function axes:descendant-axis-except-nested2() {
    let $node := doc("/db/axes-test/test.xml")/body/div
    return
        ($node//div except $node//div//div)/head
};

declare
    %test:assertEquals("<b/>")
function axes:following-sibling-in-memory-by-name() {
    let $in-mem-tests := ($axes:in-mem-doc1, $axes:in-mem-doc2)/root
    return
        $in-mem-tests//a/following-sibling::*[name()='b']
};

declare
    %test:assertEquals("<b/>")
function axes:following-sibling-in-memory() {
    let $in-mem-tests := ($axes:in-mem-doc1, $axes:in-mem-doc2)/root
    return
        $in-mem-tests//a/following-sibling::b
};

declare
    %test:assertEquals("<b/>")
function axes:following-sibling-stored-by-name() {
    let $stored-tests := (
        doc("/db/axes-test/doc1.xml"),
        doc("/db/axes-test/doc2.xml")
    )/root
    return
        $stored-tests//a/following-sibling::*[name()='b']
};

declare
    %test:assertEquals("<b/>")
function axes:following-sibling-stored() {
    let $stored-tests := (
        doc("/db/axes-test/doc1.xml"),
        doc("/db/axes-test/doc2.xml")
    )/root
    return
        $stored-tests//a/following-sibling::b
};


declare
%test:assertError("err:XPDY0002")
%test:name("expect error because variable declaration should not change context sequence")
function axes:context-self() {
util:eval("
        declare variable $foo := 123;

        .
    ")
};

declare
%test:assertError("err:XPDY0002")
%test:name("expect error because preceding let should not change context sequence")
function axes:context-let-self() {
util:eval("
        let $a := 123
        let $b := .
        return
            $b
    ")
};

declare
    %test:assertEquals(1)
function axes:child-pi() {
    count(doc("/db/axes-test/pi.xml")/processing-instruction())
};

declare
    %test:assertEquals(1)
function axes:child-pi-named() {
    count(doc("/db/axes-test/pi.xml")/processing-instruction(testpi))
};

declare
    %test:assertEquals("false")
function axes:abbrevForwardStep-at-wildcard() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@*)
};

declare
    %test:assertEquals("true")
function axes:abbrevForwardStep-at-document-test() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@document-node())
};

declare
    %test:assertEquals("true")
function axes:abbrevForwardStep-at-element-test() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@element())
};

declare
    %test:assertEquals("false")
function axes:abbrevForwardStep-at-attribute() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@attribute())
};

declare
    %test:assertEquals("true")
function axes:abbrevForwardStep-at-pi-test() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@processing-instruction())
};

declare
    %test:assertEquals("true")
function axes:abbrevForwardStep-at-comment-test() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@comment())
};

declare
    %test:assertEquals("true")
function axes:abbrevForwardStep-at-text-test() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@text())
};

declare
    %test:assertEquals("true")
function axes:abbrevForwardStep-at-namespace-node-test() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@namespace-node())
};

declare
    %test:assertEquals("false")
function axes:abbrevForwardStep-at-any-kind-test() {
    document {
    	<e1 a1="hello"/>
    }/e1/empty(@node())
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function axes:in-memory() {
    $axes:siblings//c[../preceding-sibling::a]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function axes:in-database() {
    doc("/db/axes-test/siblings.xml")//c[../preceding-sibling::a]
};


declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function axes:in-memory-predicate() {
    $axes:siblings//c[../preceding-sibling::a[1]]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function axes:in-database-predicate() {
    doc("/db/axes-test/siblings.xml")//c[../preceding-sibling::a[1]]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function axes:in-memory-position() {
    $axes:siblings//c[../preceding-sibling::a[position() eq 1]]
};

declare
    %test:assertEquals("<c>correct</c>", "<c>wrong</c>")
function axes:in-database-position() {
    doc("/db/axes-test/siblings.xml")//c[../preceding-sibling::a[position() eq 1]]
};

declare
    %test:assertEquals("<c>correct</c>")
function axes:in-memory-predicate-and-path() {
    $axes:siblings//c[../preceding-sibling::a[1]/b = 'B1']
};

declare
    %test:assertEquals("<c>correct</c>")
function axes:in-database-predicate-and-path() {
    doc("/db/axes-test/siblings.xml")//c[../preceding-sibling::a[1]/b = 'B1']
};

declare
    %test:assertEquals("<c>correct</c>")
function axes:in-memory-position-and-path() {
    $axes:siblings//c[../preceding-sibling::a[position() eq 1]/b = 'B1']
};

declare
    %test:assertEquals("<c>correct</c>")
function axes:in-database-position-and-path() {
    doc("/db/axes-test/siblings.xml")//c[../preceding-sibling::a[position() eq 1]/b = 'B1']
};

declare
    %test:assertEquals("<a>A1</a>", "<a>A2</a>")
function axes:in-database-flat-a() {
    doc("/db/axes-test/flat.xml")//d/preceding-sibling::a
};

declare
    %test:assertEquals("<c>C1</c>")
function axes:in-database-flat-c() {
    doc("/db/axes-test/flat.xml")//b/preceding-sibling::c
};

declare
    %test:assertEquals("<c>C1</c>", "<c>C2</c>")
function axes:in-database-flat-b() {
    doc("/db/axes-test/flat.xml")//b/following-sibling::c
};

declare
    %test:assertEquals("<c>alphaC1</c>", "<c>C1</c>", "<c>betaC1</c>")
function axes:in-database-cousins-c() {
    doc("/db/axes-test/cousins.xml")//b/preceding-sibling::c
};

declare
    %test:assertEquals("<c>alphaC1</c>", "<c>alphaC2</c>", "<c>C1</c>", "<c>C2</c>", "<c>betaC1</c>", "<c>betaC2</c>")
function axes:in-database-cousins-b() {
    doc("/db/axes-test/cousins.xml")//b/following-sibling::c
};

declare
    %test:assertEmpty
function axes:in-database-flat-root() {
    doc("/db/axes-test/flat.xml")//xml/preceding-sibling::any
};
