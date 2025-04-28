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

module namespace em="http://exist-db.org/xquery/indexing/tests/exist-matches";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $em:XCONF1 :=
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index>
            <lucene>
                <text qname="x"/>
            </lucene>
            <ngram qname="y"/>
        </index>
    </collection>;

declare variable $em:testCol := xmldb:create-collection("/db", "exist-matches");
declare variable $em:confCol := xmldb:create-collection("/db/system/config/db", "exist-matches");

declare
    %test:setUp
function em:setup() {

    (
        xmldb:store($em:confCol, "collection.xconf", $em:XCONF1),
        xmldb:store($em:testCol, "test1.xml",
                <root>
                        <x>Hello</x>
                        <y>Hello</y>
                </root>
        )
    )
};

declare
    %test:tearDown
function em:tearDown() {
    xmldb:remove($em:testCol),
    xmldb:remove($em:confCol)
};

declare
    %test:args("Hello")
    (:we are only seeing one match for exist match in this state because :)
    (:the matching is broken in both ft:query and ngram check  https://github.com/eXist-db/exist/issues/2102#issuecomment-1442410050 for more info:)
    %test:assertEquals(1) (:this should be 2 instead of 1:)
function em:expand-node-lucene-ngram-matches($query as xs:string) {
    let $doc := doc($em:testCol || "/test1.xml")
    let $x-hits := $doc/root[ft:query(x, $query)]
    let $y-hits := $doc/root[ngram:contains(y, $query)]
    let $hits := ($x-hits | $y-hits)
    return count(util:expand($hits)//exist:match)
};
