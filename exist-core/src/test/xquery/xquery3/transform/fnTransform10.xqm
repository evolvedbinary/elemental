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

declare variable $testTransform:transform-10-xsl := <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'
            xmlns:app='http://www.example.com'>
            <xsl:template match='/'>
                <x><xsl:value-of select='.' /></x>
            </xsl:template>
                <xsl:template name='app:main'>
                    <out>
                        <xsl:value-of select='.' />
                    </out>
                </xsl:template>
            </xsl:stylesheet>;

declare
    %test:assertTrue
function testTransform:transform-10-2() {
    let $style := $testTransform:transform-10-xsl
    let $result := transform(map{"stylesheet-node":$style, "source-node":parse-xml("<doc>this</doc>"), "initial-template": fn:QName('http://www.example.com','main') })
    return map:contains($result, "output")
};

declare
    %test:assertTrue
function testTransform:transform-10-3() {
    let $style := $testTransform:transform-10-xsl
    let $result := transform(map{"stylesheet-node":$style, "source-node":parse-xml("<doc>this</doc>"), "initial-template": fn:QName('http://www.example.com','main') })
    return $result?output//out = 'this'
};

declare
    %test:assertTrue
function testTransform:transform-10-4() {
    let $style := $testTransform:transform-10-xsl
    let $result := transform(map{"stylesheet-node":$style, "source-node":parse-xml("<doc>this</doc>"), "initial-template": fn:QName('http://www.example.com','main') })
    return $result?output instance of node()
};
