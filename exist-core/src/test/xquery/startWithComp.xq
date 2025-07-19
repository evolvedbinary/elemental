xquery version "3.1";

module namespace t="http://exist-db.org/xquery/test";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $t:XML := document {
    <Y1 id="1">
        <H1 id="2" a2="2">true</H1>
    </Y1>
};

declare
    %test:setUp
function t:setup() {
    let $testCol := xmldb:create-collection("/db", "test")
    return
        xmldb:store("/db/test", "test.xml", $t:XML)
};

declare
    %test:tearDown
function t:tearDown() {
    xmldb:remove("/db/test")
};

declare
    %test:assertTrue
function t:test() {
    doc("/db/test/test.xml")//*[starts-with(@a2, "1") = false()]
    => exists()
};