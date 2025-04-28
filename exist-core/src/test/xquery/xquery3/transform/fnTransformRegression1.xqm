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

module namespace testTransform="http://exist-db.org/xquery/test/function_transform";
import module namespace xmldb="http://exist-db.org/xquery/xmldb";
declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $testTransform:stylesheet := <xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
    <xsl:param name='v'/>
    <xsl:template match='/'>
        <v><xsl:value-of select='$v'/></v>
    </xsl:template>
</xsl:stylesheet>;

declare
    %test:setUp
function testTransform:setup() {
    let $coll := xmldb:create-collection("/db", "regression-test-1")
    return (
        xmldb:store($coll, "stylesheet.xsl", $testTransform:stylesheet, "application/xslt+xml")
    )
};

declare
    %test:tearDown
function testTransform:cleanup() {
    xmldb:remove("/db/regression-test-1")
};

declare
    %test:assertEquals("<v>2</v>")
function testTransform:regression-test-1() {
    let $in := parse-xml("<dummy/>")
    let $result := ( fn:transform(map{
        "source-node":$in,
        "stylesheet-node":doc("/db/regression-test-1/stylesheet.xsl"),
        "stylesheet-params": map { QName("","v"): "2" } } ) )?output
    return $result
};

