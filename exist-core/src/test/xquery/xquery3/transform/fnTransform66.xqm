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

declare variable $testTransform:transform-66-xsl := document {
        <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
            xmlns:xs='http://www.w3.org/2001/XMLSchema'
            xmlns:my='http://www.w3.org/fots/fn/transform/myfunctions' version='3.0'>
            <xsl:output indent='yes' suppress-indentation='b my:b'/>
            <xsl:template name='main'>
              <my:a>
                <my:b><t>green</t></my:b>
                <my:c><t>blue</t></my:c>
                <b><t>red</t></b>
                <c><t>pink</t></c>
                <d><t>black</t></d>
              </my:a>
            </xsl:template>
        </xsl:stylesheet> };

declare
    %test:assertTrue
function testTransform:transform-66-green() {
    let $xsl := $testTransform:transform-66-xsl
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "serialization-params": map{'suppress-indentation': (QName('http://www.w3.org/fots/fn/transform/myfunctions','c'), QName('', 'c'))}})
    return contains($result("output"), "><t>green</t><")
};


declare
    %test:assertTrue
function testTransform:transform-66-black() {
    let $xsl := $testTransform:transform-66-xsl
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "serialization-params": map{'suppress-indentation': (QName('http://www.w3.org/fots/fn/transform/myfunctions','c'), QName('', 'c'))}})
    return fn:matches($result("output"), ">\s+<t>black</t>\s+<")
};

