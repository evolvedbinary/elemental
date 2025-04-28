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

declare variable $testTransform:transform-82e-xsl := "<xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
    <xsl:variable name='v' select="".""/>
    <xsl:template match='.'>
            <out root-is-doc=""{$v instance of document-node()}"" this-is-doc=""{. instance of document-node()}"" xslt-version=""{system-property('xsl:version')}"">
                <xsl:value-of select='name($v)'/>
            </out>
        </xsl:template>
</xsl:stylesheet>";

declare
    %test:assertEquals('&lt;out root-is-doc="false" this-is-doc="false" xslt-version="3.0"&gt;dummy&lt;/out&gt;')
function testTransform:transform-82e() {
    let $xsl := parse-xml($testTransform:transform-82e-xsl)
    let $in := parse-xml("<dummy/>")
    let $result := fn:transform(map{
              "source-node": $in/*,
              "global-context-item": $in/*,
              "stylesheet-node": $xsl,
              "xslt-version": 2.0
           })
    return $result?output
};
