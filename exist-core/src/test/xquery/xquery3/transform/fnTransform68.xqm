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

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $testTransform:transform-68-xsl-text := document { <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
                    xmlns:xs='http://www.w3.org/2001/XMLSchema'
                    xmlns:saxon='http://saxon.sf.net/'
                    xmlns:my='http://www.w3.org/fots/fn/transform/myfunctions' version='2.0'>
                    <xsl:param name='v'/>
                    <xsl:template name='main'>
                      <out><xsl:value-of select='$v'/></out>
                    </xsl:template>
                </xsl:stylesheet> };

declare
    %test:assertError("FOXT0001")
function testTransform:transform-68-supports-dynamic-evaluation() {
    let $xsl := $testTransform:transform-68-xsl-text
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "stylesheet-params": map { QName("","v"): "2" },
                    "requested-properties" : map{fn:QName('http://www.w3.org/1999/XSL/Transform','supports-dynamic-evaluation'):true()}})
    return contains($result?output,">2</out>")
};

declare
    %test:assertError("FOXT0001")
function testTransform:transform-68-supports-xalan() {
    let $xsl := $testTransform:transform-68-xsl-text
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "stylesheet-params": map { QName("","v"): "2" },
                    "requested-properties" : map{fn:QName('http://www.w3.org/1999/XSL/Transform','product-name'):"Xalan"}})
    return contains($result?output,">2</out>")
};

declare
    %test:assertTrue
function testTransform:transform-68-supports-saxon() {
    let $xsl := $testTransform:transform-68-xsl-text
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "stylesheet-params": map { QName("","v"): "2" },
                    "requested-properties" : map{fn:QName('http://www.w3.org/1999/XSL/Transform','product-name'):"SAXON"}})
    return contains($result?output,">2</out>")
};

declare
    %test:assertTrue
function testTransform:transform-68-vendor-saxonica() {
    let $xsl := $testTransform:transform-68-xsl-text
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "stylesheet-params": map { QName("","v"): "2" },
                    "requested-properties" : map{fn:QName('http://www.w3.org/1999/XSL/Transform','vendor'):"Saxonica"}})
    return contains($result?output,">2</out>")
};

declare
    %test:assertError("XPTY0004")
function testTransform:transform-68-vendor-empty() {
    let $xsl := $testTransform:transform-68-xsl-text
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "stylesheet-params": map { QName("","v"): "2" },
                    "requested-properties" : map{fn:QName('http://www.w3.org/1999/XSL/Transform','vendor'):()}})
    return contains($result?output,">2</out>")
};

declare
     %test:assertError("FOXT0001")
function testTransform:transform-68-unknown-property() {
    let $xsl := $testTransform:transform-68-xsl-text
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "stylesheet-params": map { QName("","v"): "2" },
                    "requested-properties" : map{fn:QName('http://www.w3.org/1999/XSL/Transform','wookie'):"Chewbacca"}})
    return contains($result?output,">2</out>")
};
