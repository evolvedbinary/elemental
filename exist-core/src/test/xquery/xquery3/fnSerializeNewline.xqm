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

module namespace ser="http://exist-db.org/xquery/test/serialize";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare
    %test:assertTrue
function ser:exist-insert-final-newline-false() {
    let $doc := <root>
    <nested />
    </root>
    let $serialized := fn:serialize($doc,
        map {xs:QName("exist:insert-final-newline"): false()})
    return fn:ends-with($serialized, ">")
};

declare
    %test:assertTrue
function ser:exist-insert-final-newline-true() {
    let $doc := <root>
    <nested />
    </root>
    let $serialized := fn:serialize($doc,
        map {xs:QName("exist:insert-final-newline"): true()})
    return fn:ends-with($serialized, "&#x0A;")
};

declare
    %test:assertTrue
function ser:exist-insert-final-newline-false-json() {
    let $doc := map { "a": 1 }
    let $serialized := fn:serialize($doc,
        map {
            "method": "json",
            "exist:insert-final-newline": false()
        }
    )
    return fn:ends-with($serialized, "}")
};

declare
    %test:assertTrue
function ser:exist-insert-final-newline-true-json() {
    let $doc := map { "a": 1 }
    let $serialized := fn:serialize($doc,
        map {
            "method": "json",
            "exist:insert-final-newline": true()
        }
    )
    return fn:ends-with($serialized, "&#x0A;")
};
