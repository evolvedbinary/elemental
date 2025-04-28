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

import module namespace test = "http://exist-db.org/xquery/xqsuite"
    at "resource:org/exist/xquery/lib/xqsuite/xqsuite.xql";

declare variable $test-module-uri as xs:anyURI external;

(: hooks for sending external notifications about test events :)
declare variable $test-ignored-function as (function(xs:string) as empty-sequence())? external;
declare variable $test-started-function as (function(xs:string) as empty-sequence())? external;
declare variable $test-failure-function as (function(xs:string, map(xs:string, item()?), map(xs:string, item()?)?) as empty-sequence())? external;
declare variable $test-assumption-failed-function as (function(xs:string, map(xs:string, item()?)) as empty-sequence())? external;
declare variable $test-error-function as (function(xs:string, map(xs:string, item()?)?) as empty-sequence())? external;
declare variable $test-finished-function as (function(xs:string) as empty-sequence())? external;

test:suite(
    inspect:module-functions($test-module-uri),
    $test-ignored-function, $test-started-function, $test-failure-function,
    $test-assumption-failed-function, $test-error-function, $test-finished-function
)
