xquery version "3.1";

module namespace t = "http://exist-db.org/testsuite/scheduler-external-variable-default";

import module namespace test = "http://exist-db.org/xquery/xqsuite" at "resource:org/exist/xquery/lib/xqsuite/xqsuite.xql";

declare namespace scheduler = "http://exist-db.org/xquery/scheduler";
declare namespace xmldb = "http://exist-db.org/xquery/xmldb";
declare namespace util = "http://exist-db.org/xquery/util";

declare variable $t:collection := "/db/scheduler-default-test";
declare variable $t:job-name := "scheduler-default-test-job";

declare variable $t:worker-query :=
    "xquery version '3.1';
    declare variable $addressee external := 'default';
    xmldb:store('" || $t:collection || "', 'result.xml', <result>{$addressee}</result>)";

declare
    %test:setUp
function t:setup() {
    xmldb:create-collection("/db", "scheduler-default-test"),
    xmldb:store($t:collection, "worker.xql", $t:worker-query)
};

declare
    %test:tearDown
function t:cleanup() {
    scheduler:delete-scheduled-job($t:job-name),
    xmldb:remove($t:collection)
};

declare
    %test:assertTrue
function t:schedule-job-with-parameter() as xs:boolean {
    scheduler:schedule-xquery-periodic-job(
        $t:collection || "/worker.xql",
        0,
        $t:job-name,
        <parameters>
            <param name="addressee" value="overridden"/>
        </parameters>,
        0,
        0
    )
};

declare
    %test:assertEquals("overridden")
function t:external-variable-was-overridden() as xs:string {
    util:wait(1000),
    doc($t:collection || "/result.xml")/result/string()
};