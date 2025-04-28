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

declare variable $testTransform:books-4 := xs:string("<books>
    <book>
        <title>XSLT Programmer?s Reference</title>
        <author>Michael H. Kay</author>
    </book>
    <book>
        <title>XSLT</title>
        <author>Doug Tidwell</author>
        <author>Simon St. Laurent</author>
        <author>Robert Romano</author>
    </book>
</books>");

declare variable $testTransform:style-4 := <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:template match="/">
        <html>
            <body>
                <div>
                    <xsl:for-each select="books/book">
                        <b><xsl:value-of select="title"/></b>: <xsl:value-of select="author"
                        /><br/>
                    </xsl:for-each>
                </div>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>;

declare variable $testTransform:style-4s :=
xs:string('<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:template match="/">
        <html>
            <body>
                <div>
                    <xsl:for-each select="books/book">
                        <b><xsl:value-of select="title"/></b>: <xsl:value-of select="author"
                        /><br/>
                    </xsl:for-each>
                </div>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>');

declare
    %test:assertTrue
function testTransform:transform-4() {
    let $in := $testTransform:books-4,
    $style := $testTransform:style-4
    let $trn := transform(map{"source-node":fn:parse-xml($in), "stylesheet-node":$style, "serialization-params": map{"indent": true()} } )("output")
    return $trn//b = 'XSLT'
};

declare
    %test:assertTrue
function testTransform:transform-5() {
    let $in := $testTransform:books-4,
    $style := $testTransform:style-4s
    let $trn := transform(map{"source-node":fn:parse-xml($in), "stylesheet-text":$style, "serialization-params": map{"indent": true()} } )("output")
    return $trn//b = 'XSLT'
};
