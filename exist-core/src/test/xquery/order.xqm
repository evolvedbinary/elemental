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
xquery version "1.0";

module namespace ord = "http://exist-db.org/test/order";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare variable $ord:items as element(item)+ := (
        <item>
         <ref>e1</ref>
         <msDesc type="Ll"/>
        </item>
        ,
        <item>
         <ref>e2</ref>
         <msDesc type="P"/>
        </item>
        ,
        <item>
         <ref>e3</ref>
         <msDesc type="Ll"/>
        </item>
        ,
        <item>
         <ref>e4</ref>
         <msDesc/>
        </item>
);

declare
    %test:assertEquals("e2", "e1", "e3", "e4")
function ord:default-order-for-empty-sequence-is-empty-least() {
    for $item in $ord:items
    let $sort-condition as xs:boolean? := $item/msDesc/@type eq 'P'
    order by $sort-condition descending
    return
        $item/ref/string()
};

declare
    %test:assertEquals("e2", "e1", "e3", "e4")
function ord:order-empty-sequence-as-empty-least() {
    for $item in $ord:items
    let $sort-condition as xs:boolean? := $item/msDesc/@type eq 'P'
    order by $sort-condition descending empty least
    return
        $item/ref/string()
};

declare
    %test:assertEquals("e4", "e2", "e1", "e3")
function ord:order-empty-sequence-as-empty-greatest() {
    for $item in $ord:items
    let $sort-condition as xs:boolean? := $item/msDesc/@type eq 'P'
    order by $sort-condition descending empty greatest
    return
        $item/ref/string()
};
