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

module namespace collations = "http://exist-db.org/xquery/test/collations";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

import module namespace xmldb = "http://exist-db.org/xquery/xmldb";

declare variable $collations:TEST-DOC-1 := document {
    <entry>
        <a>xxx</a>
        <b/>
    </entry>
};

declare
    %test:setUp
function collations:setup() {
    xmldb:create-collection("/db", "collations-test"),
    xmldb:store("/db/collations-test", "test.xml", $collations:TEST-DOC-1)
};

declare
    %test:tearDown
function collations:cleanup() {
    xmldb:remove("/db/collations-test")
};

declare
    %test:assertEquals("<a>xxx</a>")
function collations:non-empty-string-contains() {
    doc("/db/collations-test/test.xml")//a[contains(.,'x',"?lang=en-US")]
};

declare
    %test:assertEmpty
function collations:empty-string-contains() {
    doc("/db/collations-test/test.xml")//b[contains(.,'x',"?lang=en-US")]
};

declare
    %test:assertEquals("<a>xxx</a>")
function collations:non-empty-string-starts-with() {
    doc("/db/collations-test/test.xml")//a[starts-with(.,'x',"?lang=en-US")]
};

 declare
    %test:assertEmpty
function collations:empty-string-starts-with() {
    doc("/db/collations-test/test.xml")//b[starts-with(.,'x',"?lang=en-US")]
};

declare
    %test:assertEquals("<a>xxx</a>")
function collations:non-empty-string-ends-with() {
    doc("/db/collations-test/test.xml")//a[ends-with(.,'x',"?lang=en-US")]
};

 declare
    %test:assertEmpty
function collations:empty-string-ends-with() {
    doc("/db/collations-test/test.xml")//b[ends-with(.,'x',"?lang=en-US")]
};

declare
    %test:assertEquals("")
    function collations:substring-after-empty-string() {
        substring-after("", "test", "?lang=en-US")
};

declare
    %test:assertEquals("")
    function collations:substring-before-empty-string() {
        substring-before("", "test", "?lang=en-US")
};

declare
    %test:assertEquals("")
    function collations:substring-after-empty-sequence() {
        substring-after((), "test", "?lang=en-US")
};

declare
    %test:assertEquals("")
    function collations:substring-before-empty-sequence() {
        substring-before((), "test", "?lang=en-US")
};