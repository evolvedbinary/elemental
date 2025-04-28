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

declare variable $testTransform:transform-67-xsl := document {
    <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
                xmlns:xs='http://www.w3.org/2001/XMLSchema'
                xmlns:my='http://www.w3.org/fots/fn/transform/myfunctions' version='2.0'>
                <xsl:output use-character-maps='map-one map-two'/>
                <xsl:character-map name='map-one'>
                  <xsl:output-character character='-' string='(hyphen)'/>
                  <xsl:output-character character='*' string='(asterisk)'/>
                </xsl:character-map>
                                <xsl:character-map name='map-two'>
                                  <xsl:output-character character='+' string='(sum)'/>
                                  <xsl:output-character character='/' string='(div)'/>
                                </xsl:character-map>
                <xsl:template name='main'>
                  <out>a-b*c+d/e</out>
                </xsl:template>
            </xsl:stylesheet> };

declare
    %test:assertTrue
function testTransform:transform-67() {
    let $xsl := $testTransform:transform-67-xsl
    let $result := fn:transform(map{
        "stylesheet-node":$xsl,
        "initial-template": fn:QName('','main'),
                    "delivery-format" : "serialized",
                    "serialization-params": map{"indent": true(), 'use-character-maps': map{'*':'(star)'}}})
    return contains($result?output,"a(hyphen)b(star)c(sum)d(div)e")
};

