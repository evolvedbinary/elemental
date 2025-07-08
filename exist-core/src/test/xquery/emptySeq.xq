xquery version "3.1";

module namespace t="http://exist-db.org/xquery/test";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $t:XML := document {
    <F id="1"/>
};

declare
    %test:setUp
function t:setup() {
    xmldb:create-collection("/db", "test"),
    xmldb:store("/db/test", "test.xml", $t:XML)
};

declare
    %test:tearDown
function t:tearDown() {
    xmldb:remove("/db/test")
};

declare
    %test:assertTrue
function t:test-db() {
    exists(
        doc("/db/test/test.xml")//F[boolean(count(@id >= 2))]
    )
};

declare
    %test:assertTrue
function t:test-mem() {
    exists(
        $t:XML//F[boolean(count(@id >= 2))]
    )
};