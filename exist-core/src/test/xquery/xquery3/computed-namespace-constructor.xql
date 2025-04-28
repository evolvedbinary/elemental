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

(:~
 : Tests for Computed Namespace Constructors
 :)
module namespace cnc = "http://exist-db.org/xquery/test/computed-namespace-constructors";

import module namespace test = "http://exist-db.org/xquery/xqsuite" at "resource:org/exist/xquery/lib/xqsuite/xqsuite.xql";

declare namespace ok="http://place-on-interwebz.com/a-ok";
declare namespace doh="http://also-on-interwebz.com/problem";


declare
    %test:assertError("XQDY0102")
function cnc:cannot-override-no-ns() {
    element root {namespace {""} {"http://also-on-interwebz.com/problem"},
        namespace ok {"http://place-on-interwebz.com/a-ok"},
        for $n in 1 to 3
        return
            element stuff {$n}
    }
};

declare
    %test:assertEquals(3)
function cnc:ns-default-constructor() {
    count(
        element ok:root {namespace {""} {"http://also-on-interwebz.com/problem"},
            namespace ok {"http://place-on-interwebz.com/a-ok"},
            for $n in 1 to 3
            return
                element stuff {$n}
        }/stuff
    )
};
