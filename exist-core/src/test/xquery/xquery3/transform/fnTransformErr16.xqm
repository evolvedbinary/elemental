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

declare variable $testTransform:xsl-16 := "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
                                     xmlns:my='http://www.w3.org/fots/fn/transform/myfunctions' version='3.0'>
                                         <xsl:function name='my:user-function'>
                                             <xsl:param name='param1'/>
                                             <xsl:param name='param2'/>
                                             <this>
                                             <xsl:value-of select='$param1'/><xsl:value-of select='$param2'/>
                                             </this>
                                         </xsl:function>
                                         <xsl:template name='main'>
                                             <out>
                                                 <xsl:value-of select='.' />
                                             </out>
                                         </xsl:template>
                                     </xsl:stylesheet>";

declare
    %test:assertError("err:FOXT0002")
function testTransform:transform-err-16() {
    let $xsl := $testTransform:xsl-16
    let $result := fn:transform(map{"stylesheet-text":$xsl, "source-node":parse-xml("<doc>this</doc>"),
            "initial-function": fn:QName('http://www.w3.org/fots/fn/transform/myfunctions','user-function')
            })
    return $result?output
};
