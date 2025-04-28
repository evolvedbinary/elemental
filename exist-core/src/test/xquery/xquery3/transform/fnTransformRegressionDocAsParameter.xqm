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

declare variable $testTransform:stylesheet1 := <xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
    <xsl:param name='v'/>
    <xsl:template match='/'>
        <v><xsl:value-of select='$v'/></v>
    </xsl:template>
</xsl:stylesheet>;

declare variable $testTransform:stylesheet2 := <xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
    <xsl:param name='v'/>
    <xsl:template name='named-template' match='/'>
        <v><xsl:value-of select='$v'/></v>
    </xsl:template>
</xsl:stylesheet>;

declare variable $testTransform:document := <document>
    <catalog>
        <book id="bk101">
           <author>Gambardella, Matthew</author>
           <title>XML Developer's Guide</title>
           <genre>Computer</genre>
           <price>44.95</price>
           <publish_date>2000-10-01</publish_date>
           <description>An in-depth look at creating applications
           with XML.</description>
        </book>
        <book id="bk102">
           <author>Ralls, Kim</author>
           <title>Midnight Rain</title>
           <genre>Fantasy</genre>
           <price>5.95</price>
           <publish_date>2000-12-16</publish_date>
           <description>A former architect battles corporate zombies,
           an evil sorceress, and her own childhood to become queen
           of the world.</description>
        </book>
    </catalog>
</document>;

declare
    %test:setUp
function testTransform:setup() {
    let $coll := xmldb:create-collection("/db", "regression-test")
    let $storeStylesheet1 := xmldb:store($coll, "stylesheet1.xsl", $testTransform:stylesheet1, "application/xslt+xml")
    let $storeStylesheet2 := xmldb:store($coll, "stylesheet2.xsl", $testTransform:stylesheet2, "application/xslt+xml")
    let $storeDocument := xmldb:store($coll, "document.xml", $testTransform:document, "application/document")
    return ()
};

declare
    %test:tearDown
function testTransform:cleanup() {
    xmldb:remove("/db/regression-test")
};

declare
    %test:assertEquals("<v>Gambardella, MatthewXML Developer's GuideComputer44.952000-10-01An in-depth look at creating applications
           with XML.Ralls, KimMidnight RainFantasy5.952000-12-16A former architect battles corporate zombies,
           an evil sorceress, and her own childhood to become queen
           of the world.</v>")
function testTransform:regression-test-1() {
    let $in := parse-xml("<dummy/>")
    let $result := ( fn:transform(map{
        "source-node":$in,
        "stylesheet-node":doc("/db/regression-test/stylesheet1.xsl"),
        "stylesheet-params": map { QName("","v"): doc("/db/regression-test/document.xml") } } ) )?output
    return $result
};

declare
    %test:assertEquals("<v>Gambardella, MatthewXML Developer's GuideComputer44.952000-10-01An in-depth look at creating applications
           with XML.Ralls, KimMidnight RainFantasy5.952000-12-16A former architect battles corporate zombies,
           an evil sorceress, and her own childhood to become queen
           of the world.</v>")
function testTransform:regression-test-2() {
    let $in := parse-xml("<dummy/>")
    let $result := ( fn:transform(map{
           "source-node":$in,
           "stylesheet-node":doc("/db/regression-test/stylesheet2.xsl"),
           "initial-template": QName('', 'named-template'),
           "global-context-item" : fn:doc("/db/regression-test/document.xml"),
           "stylesheet-params": map {
             QName('', 'v'): fn:doc("/db/regression-test/document.xml")
           }}))?output
    return $result
};


