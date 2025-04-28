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

declare variable $testTransform:transform-87-xsl :=
"<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
             version='1.0'>
             <xsl:template match='/'>
               <xsl:variable name='in' select='.'/>
               <xsl:for-each select='1 to 3'>
                 <xsl:result-document href='output{.}' omit-xml-declaration='yes'>
                    <xsl:sequence select='number($in/a/b) + .'/>
                 </xsl:result-document>
               </xsl:for-each>
             </xsl:template>
         </xsl:stylesheet>";

declare
    %test:assertEquals("96")
function testTransform:transform-87() {
    let $xsl := $testTransform:transform-87-xsl
    let $trans-result := fn:transform(map{"stylesheet-text" : $xsl,
                                                                   "delivery-format" : "raw",
                                                                   "base-output-uri" : "http://example.com/",
                                                                   "source-node"     : parse-xml('<a><b>89</b></a>'),
                                                                   "post-process"    : function($uri, $val) { $val + 5 }
                                                                  })
    return $trans-result("http://example.com/output2")
};
