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
xquery version "3.0";

module namespace ue = "http://exist-db.org/xquery/test/util/expand";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace util = "http://exist-db.org/xquery/util";

declare
    %test:assertEquals("true", "true", "bar")
function ue:attribute() as item()+ {
   let $attr := util:expand( attribute foo { "bar" } )
   return
       (
               $attr instance of attribute(),
               node-name($attr) eq xs:QName("foo"),
               string($attr)
       )
};

declare
    %test:assertEquals("true", "true", "bar")
function ue:attributeNs() as item()+ {
    let $attr := util:expand( attribute ue:foo { "bar" } )
    return
        (
                $attr instance of attribute(),
                node-name($attr) eq xs:QName("ue:foo"),
                string($attr)
        )
};

declare
    %test:assertTrue
function ue:comment() as xs:boolean {
    util:expand( comment { "foo" } ) instance of comment()
};

declare
    %test:assertTrue
function ue:document() as xs:boolean {
    util:expand( document { element foo {()} } ) instance of document-node()
};

declare
    %test:assertTrue
function ue:documentNs() as xs:boolean {
    util:expand( document { element ue:foo {()} } ) instance of document-node()
};

declare
    %test:assertEquals("true", "true")
function ue:element() as xs:boolean+ {
    let $elem := util:expand( element foo {()} )
    return
        (
                $elem instance of element(),
                node-name($elem) eq xs:QName("foo")
        )
};

declare
    %test:assertEquals("true", "true")
function ue:elementNs() as xs:boolean+ {
    let $elem := util:expand( element ue:foo {()} )
    return
        (
                $elem instance of element(),
                node-name($elem) eq xs:QName("ue:foo")
        )
};

declare
    %test:assertTrue
function ue:pi() as xs:boolean {
    util:expand( processing-instruction foo { "" } ) instance of processing-instruction()
};

declare
    %test:assertTrue
function ue:text() as xs:boolean {
    util:expand( text { "foo" } ) instance of text()
};