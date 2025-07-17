(:
 : Elemental
 : Copyright (C) 2024, Evolved Binary Ltd
 :
 : admin@evolvedbinary.com
 : https://www.evolvedbinary.com | https://www.elemental.xyz
 :
 : This library is free software; you can redistribute it and/or
 : modify it under the terms of the GNU Lesser General Public
 : License as published by the Free Software Foundation; version 2.1.
 :
 : This library is distributed in the hope that it will be useful,
 : but WITHOUT ANY WARRANTY; without even the implied warranty of
 : MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 : Lesser General Public License for more details.
 :
 : You should have received a copy of the GNU Lesser General Public
 : License along with this library; if not, write to the Free Software
 : Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 :
 : NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 :       The original license header is included below.
 :
 : =====================================================================
 :
 : eXist-db Open Source Native XML Database
 : Copyright (C) 2001 The eXist-db Authors
 :
 : info@exist-db.org
 : http://www.exist-db.org
 :
 : This library is free software; you can redistribute it and/or
 : modify it under the terms of the GNU Lesser General Public
 : License as published by the Free Software Foundation; either
 : version 2.1 of the License, or (at your option) any later version.
 :
 : This library is distributed in the hope that it will be useful,
 : but WITHOUT ANY WARRANTY; without even the implied warranty of
 : MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 : Lesser General Public License for more details.
 :
 : You should have received a copy of the GNU Lesser General Public
 : License along with this library; if not, write to the Free Software
 : Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 :)
xquery version "3.1";

module namespace xtj = "http://exist-db.org/xquery/test/xml-to-json";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare
    %test:assertEmpty
function xtj:xml-to-json-empty-sequence() {
    let $node := ()
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
    %test:arg('arg1', ' ')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-null-no-ns($arg1) {
    let $node := <null>{$arg1}</null>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('null')
    %test:arg('arg1', ' ')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-null-fn-ns($arg1) {
    let $node := <fn:null>{$arg1}</fn:null>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1','0')
    %test:assertError('FOJS0006')
    %test:arg('arg1','1')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-boolean-no-ns($arg1) {
    let $node := <boolean>{$arg1}</boolean>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1','')
    %test:assertEquals('false')
    %test:arg('arg1','0')
    %test:assertEquals('false')
    %test:arg('arg1','1')
    %test:assertEquals('true')
function xtj:xml-to-json-boolean-fn-ns($arg1) {
    let $node := <fn:boolean>{$arg1}</fn:boolean>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
    %test:arg('arg1', '0')
    %test:assertError('FOJS0006')
    %test:arg('arg1', '1')
    %test:assertError('FOJS0006')
    %test:arg('arg1', '-1')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-number-no-ns($arg1) {
    let $node := <number>{$arg1}</number>
    return lower-case(fn:xml-to-json($node))
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
    %test:arg('arg1', '0')
    %test:assertEquals('0')
    %test:arg('arg1', '1')
    %test:assertEquals('1')
    %test:arg('arg1', '-1')
    %test:assertEquals('-1')
    %test:arg('arg1', '01')
    %test:assertEquals('1')
    %test:arg('arg1', '08')
    %test:assertEquals('8')
    %test:arg('arg1', '3.1415')
    %test:assertEquals('3.1415')
    %test:arg('arg1', '0.31415e+1')
    %test:assertEquals('3.1415')
    %test:arg('arg1', '0.31415e1')
    %test:assertEquals('3.1415')
    %test:arg('arg1', '31.415e-1')
    %test:assertEquals('3.1415')
function xtj:xml-to-json-number-fn-ns($arg1) {
    let $node := <fn:number>{$arg1}</fn:number>
    return lower-case(fn:xml-to-json($node))
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
    %test:arg('arg1', ' ')
    %test:assertError('FOJS0006')
    %test:arg('arg1', 'a')
    %test:assertError('FOJS0006')
    %test:arg('arg1', 'ab')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-string-unescaped-no-ns($arg1) {
    let $node := <string>{$arg1}</string>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('""')
    %test:arg('arg1', ' ')
    %test:assertEquals('" "')
    %test:arg('arg1', 'a')
    %test:assertEquals('"a"')
    %test:arg('arg1', 'ab')
    %test:assertEquals('"ab"')
    %test:arg('arg1', '\')
    %test:assertEquals('"\\"')
    %test:arg('arg1', '"')
    %test:assertEquals('"\""')
    %test:arg('arg1', '&#10;')
    %test:assertEquals('"\n"')
    %test:arg('arg1', '/')
    %test:assertEquals('"/"')
(: TODO: needs implementation
    %test:arg('arg1', '&#127;')
    %test:assertEquals('"\u007F"')
:)
function xtj:xml-to-json-string-unescaped-fn-ns($arg1) {
    let $node := <fn:string>{$arg1}</fn:string>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
    %test:arg('arg1', ' ')
    %test:assertError('FOJS0006')
    %test:arg('arg1', 'a')
    %test:assertError('FOJS0006')
    %test:arg('arg1', 'ab')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-string-escaped-no-ns($arg1) {
    let $node := <string escaped="true">{$arg1}</string>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('""')
    %test:arg('arg1', ' ')
    %test:assertEquals('" "')
    %test:arg('arg1', 'a')
    %test:assertEquals('"a"')
    %test:arg('arg1', 'ab')
    %test:assertEquals('"ab"')
    %test:arg('arg1', '\n')
    %test:assertEquals('"\n"')
    %test:arg('arg1', '"')
    %test:assertError('FOJS0007')
    %test:arg('arg1', '&#10;')
    %test:assertError('FOJS0007')
(: TODO: needs implementation
    %test:arg('arg1', '/')
    %test:assertEquals('"\/"')
    %test:arg('arg1', ' /')
    %test:assertEquals('" \/"')
    %test:arg('arg1', '\/')
    %test:assertEquals('"\/"')
    %test:arg('arg1', '&#127;')
    %test:assertEquals('"\u007F"')
    %test:arg('arg1', '""')
    %test:assertEquals('"\"\""')
:)
function xtj:xml-to-json-string-escaped-fn-ns($arg1) {
    let $node := <fn:string escaped="true">{$arg1}</fn:string>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
    %test:arg('arg1', ' ')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-array-no-ns($arg1) {
    let $node := <array>{$arg1}</array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('[]')
    %test:arg('arg1', ' ')
    %test:assertEquals('[]')
function xtj:xml-to-json-array-fn-ns($arg1) {
    let $node := <fn:array>{$arg1}</fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-array-1-element-no-ns($arg1) {
    let $node := <fn:array><null>{$arg1}</null></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('[null]')
function xtj:xml-to-json-array-1-element-fn-ns($arg1) {
    let $node := <fn:array><fn:null>{$arg1}</fn:null></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-array-2-elements-no-ns($arg1) {
    let $node := <fn:array><fn:null>{$arg1}</fn:null><null>{$arg1}</null></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('[null,null]')
function xtj:xml-to-json-array-2-elements-fn-ns($arg1) {
    let $node := <fn:array><fn:null>{$arg1}</fn:null><fn:null>{$arg1}</fn:null></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('[null,null,null]')
function xtj:xml-to-json-array-3-elements-fn-ns($arg1) {
    let $node := <fn:array><fn:null>{$arg1}</fn:null><fn:null>{$arg1}</fn:null><fn:null>{$arg1}</fn:null></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
    %test:arg('arg1', ' ')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-empty-no-ns($arg1) {
    let $node := <map>{$arg1}</map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('{}')
    %test:arg('arg1', ' ')
    %test:assertEquals('{}')
function xtj:xml-to-json-map-empty-fn-ns($arg1) {
    let $node := <fn:map>{$arg1}</fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-emptyKey-element-no-ns($arg1) {
    let $node := <fn:map><null key="">{$arg1}</null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('{"":null}')
function xtj:xml-to-json-map-emptyKey-element-fn-ns($arg1) {
    let $node := <fn:map><fn:null key="">{$arg1}</fn:null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-nokey-element-no-ns($arg1) {
    let $node := <fn:map><null>{$arg1}</null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-nokey-element-fn-ns($arg1) {
    let $node := <fn:map><fn:null>{$arg1}</fn:null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-key-nullElement-no-ns($arg1) {
    let $node := <fn:map><null key="key1">{$arg1}</null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('{"key1":null}')
function xtj:xml-to-json-map-key-nullElement-fn-ns($arg1) {
    let $node := <fn:map><fn:null key="key1">{$arg1}</fn:null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', 'ke\y1')
    %test:arg('arg2', 'false')
    %test:assertError('FOJS0006')
    %test:arg('arg1', 'ke\y1')
    %test:arg('arg2', 'true')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-escapedKey-element-no-ns($arg1, $arg2) {
    let $node := <fn:map><null key="{$arg1}" escaped-key="{$arg2}"></null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', 'key1')
    %test:arg('arg2', 'false')
    %test:assertEquals('{"key1":null}')
    %test:arg('arg1', 'key1')
    %test:arg('arg2', 'true')
    %test:assertEquals('{"key1":null}')
    %test:arg('arg1', 'ke\y1')
    %test:arg('arg2', 'false')
    %test:assertEquals('{"ke\\y1":null}')
    %test:arg('arg1', 'ke\y1')
    %test:arg('arg2', 'true')
    %test:assertError('FOJS0007')
function xtj:xml-to-json-map-escapedKey-element-fn-ns($arg1, $arg2) {
    let $node := <fn:map><fn:null key="{$arg1}" escaped-key="{$arg2}"></fn:null></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '0')
    %test:assertError('FOJS0006')
    %test:arg('arg1', '1')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-key-element-no-ns($arg1) {
    let $node := <map><boolean key="key1">{$arg1}</boolean></map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '0')
    %test:assertEquals('{"key1":false}')
    %test:arg('arg1', '1')
    %test:assertEquals('{"key1":true}')
function xtj:xml-to-json-map-key-element-fn-ns($arg1) {
    let $node := <fn:map><fn:boolean key="key1">{$arg1}</fn:boolean></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', 'key1')
    %test:arg('arg2', '0')
    %test:arg('arg3', 'key2')
    %test:arg('arg4', '1')
    %test:assertEquals('true')
    %test:arg('arg1', '0')
    %test:arg('arg2', '0')
    %test:arg('arg3', '1')
    %test:arg('arg4', '1')
    %test:assertEquals('true')
    %test:arg('arg1', 'key1')
    %test:arg('arg2', '0')
    %test:arg('arg3', 'key1')
    %test:arg('arg4', '1')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-multipleKeys-multipleElements-fn-ns($arg1, $arg2, $arg3, $arg4) {
    let $node := <fn:map><fn:boolean key="{$arg1}">{$arg2}</fn:boolean><fn:boolean key="{$arg3}">{$arg4}</fn:boolean></fn:map>
    let $rc := fn:xml-to-json($node)
    return
        if (compare($rc, concat('{"', $arg1, '":false,"', $arg3, '":true}')) = 0) then (
            'true'
        ) else if (compare($rc, concat('{"', $arg3, '":true,"', $arg1, '":false}"')) = 0) then (
            'true'
        ) else (
            'false'
        )
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-array-array-no-ns($arg1) {
    let $node := <fn:array><array>{$arg1}</array></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('[[]]')
function xtj:xml-to-json-array-array-fn-ns($arg1) {
    let $node := <fn:array><fn:array>{$arg1}</fn:array></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-array-map-no-ns($arg1) {
    let $node := <fn:array><map>{$arg1}</map></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('[{}]')
function xtj:xml-to-json-array-map-fn-ns($arg1) {
    let $node := <fn:array><fn:map>{$arg1}</fn:map></fn:array>
    return fn:xml-to-json($node)
};


declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-map-emptyKey-array-no-ns($arg1) {
    let $node := <fn:map><array key="">{$arg1}</array></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('{"":[]}')
function xtj:xml-to-json-map-emptyKey-array-fn-ns($arg1) {
    let $node := <fn:map><fn:array key="">{$arg1}</fn:array></fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertError('FOJS0006')
function xtj:xml-to-json-toplevelElementKey-no-ns($arg1) {
    let $node := <null key="key1"/>
    return fn:xml-to-json($node)
};

declare
    %test:arg('arg1', '')
    %test:assertEquals('null')
function xtj:xml-to-json-toplevelElementKey-fn-ns($arg1) {
    let $node := <fn:null key="key1"/>
    return fn:xml-to-json($node)
};

declare
    %test:assertError('FOJS0006')
function xtj:xml-to-json-xmlInJsonString-no-ns() {
    let $node := <string>&lt;test&gt; \ &lt;/test&gt;</string>
    return fn:xml-to-json($node)
};

declare
    %test:assertEquals('"<test> \\ </test>"')
function xtj:xml-to-json-xmlInJsonString-fn-ns() {
    let $node := <fn:string>&lt;test&gt; \ &lt;/test&gt;</fn:string>
    return fn:xml-to-json($node)
};

declare
    %test:assertEquals('{"pcM9qSs":"YbFYeK10.e01xgS1DEJFaxxvm372Ru","wh5J8qAmnZx8WAHnHCeBpM":-1270212191.431,"ssEhB3U9zZhRNNH2Vm":["A","OIQwg4ICB9fkzihwpE.cQv1",false]}')
function xtj:xml-to-json-generatedFromSchema-1-fn-ns() {
    let $node :=
<map xmlns="http://www.w3.org/2005/xpath-functions"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <string key="pcM9qSs" escaped-key="false" escaped="false">YbFYeK10.e01xgS1DEJFaxxvm372Ru</string>
    <number key="wh5J8qAmnZx8WAHnHCeBpM" escaped-key="false">-1270212191.431</number>
    <array key="ssEhB3U9zZhRNNH2Vm" escaped-key="false">
        <string escaped="false">A</string>
        <string escaped="false">OIQwg4ICB9fkzihwpE.cQv1</string>
        <boolean>0</boolean>
    </array>
</map>
    return fn:xml-to-json($node)
};

declare
    %test:assertEquals('{"v-DhbQUwZO3zpW":[{"fRcP.5e9btnuR3dOnd":[false,"_aQ",null],"yVlXSsyg1pPatQ7ilEaSSA9":"DVbrO2wpIRJimrskkRk.7wg1Gvh","K9xGofqp":true,"PatQ7iK9xGof":false},11145450.201,584608693.252],"IU6lSWbLYTzc3QvIVAdmJ.CG":1600374222.048,"_o3UT5zEy":"WFUwRRW5Jc3rdwKCoO8iV3RYDu_5"}')
function xtj:xml-to-json-generatedFromSchema-2-fn-ns() {
    let $node :=
<map xmlns="http://www.w3.org/2005/xpath-functions"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <array key="v-DhbQUwZO3zpW" escaped-key="false">
        <map>
            <array key="fRcP.5e9btnuR3dOnd" escaped-key="false">
                <boolean>0</boolean>
                <string escaped="false">_aQ</string>
                <null/>
            </array>
            <string key="yVlXSsyg1pPatQ7ilEaSSA9" escaped-key="false" escaped="false">DVbrO2wpIRJimrskkRk.7wg1Gvh</string>
            <boolean key="K9xGofqp" escaped-key="false">true</boolean>
            <boolean key="PatQ7iK9xGof" escaped-key="false">false</boolean>
        </map>
        <number>11145450.201</number>
        <number>584608693.252</number>
    </array>
    <number key="IU6lSWbLYTzc3QvIVAdmJ.CG" escaped-key="false">1600374222.048</number>
    <string key="_o3UT5zEy" escaped-key="false" escaped="false">WFUwRRW5Jc3rdwKCoO8iV3RYDu_5</string>
</map>
    return fn:xml-to-json($node)
};

declare
    %test:assertError('FOJS0006')
function xtj:xml-to-json-unsupportedElement-no-ns() {
    let $node :=
    <fn:map>
        <my-element key=""></my-element>
    </fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:assertError('FOJS0006')
function xtj:xml-to-json-unsupportedElement-fn-ns() {
    let $node :=
    <fn:map>
        <fn:my-element key=""></fn:my-element>
    </fn:map>
    return fn:xml-to-json($node)
};

declare
    %test:assertEquals('[[],""]')
function xtj:xml-to-json-clearTextnodeBufferForNewElement-fn-ns() {
    let $node := <fn:array><fn:array> </fn:array><fn:string/></fn:array>
    return fn:xml-to-json($node)
};

declare
    %test:arg("int", "-1") %test:assertEquals('{"integer":-1}')
    %test:arg("int", "-1.0") %test:assertEquals('{"integer":-1.0}')
    %test:arg("int", "0") %test:assertEquals('{"integer":0}')
    %test:arg("int", "0.0") %test:assertEquals('{"integer":0.0}')
    %test:arg("int", "1") %test:assertEquals('{"integer":1}')
    %test:arg("int", "1.0") %test:assertEquals('{"integer":1.0}')
function xtj:xmlmap-to-json-for-int-precision-fn-ns($int as xs:string) as xs:string {
    fn:xml-to-json(
        <fn:map>
            <fn:number key="integer">{$int}</fn:number>
        </fn:map>
    )
};

declare
    %test:arg("int", "-1") %test:assertXPath('$result/fn:map/fn:number = ''-1''')
    %test:arg("int", "-1.0") %test:assertXPath('$result/fn:map/fn:number = ''-1.0''')
    %test:arg("int", "0") %test:assertXPath('$result/fn:map/fn:number = ''0''')
    %test:arg("int", "0.0") %test:assertXPath('$result/fn:map/fn:number = ''0.0''')
    %test:arg("int", "1") %test:assertXPath('$result/fn:map/fn:number = ''1''')
    %test:arg("int", "1.0") %test:assertXPath('$result/fn:map/fn:number = ''1.0''')
function xtj:json-to-xmlmap-for-int-precision($int as xs:string) as document-node() {
    fn:json-to-xml(
        '{"integer":' || $int || '}'
    )
};

declare
    %test:arg("int", "1E9") %test:assertEquals('{"integer":1E+9}')
    %test:arg("int", "1E+9") %test:assertEquals('{"integer":1E+9}')
    %test:arg("int", "1E-9") %test:assertEquals('{"integer":1E-9}')
    %test:arg("int", "1e9") %test:assertEquals('{"integer":1E+9}')
    %test:arg("int", "1e+9") %test:assertEquals('{"integer":1E+9}')
    %test:arg("int", "1e-9") %test:assertEquals('{"integer":1E-9}')
    %test:arg("int", "1.1E9") %test:assertEquals('{"integer":1.1E+9}')
    %test:arg("int", "1.1E+9") %test:assertEquals('{"integer":1.1E+9}')
    %test:arg("int", "1.1E-9") %test:assertEquals('{"integer":1.1E-9}')
    %test:arg("int", "1.1e9") %test:assertEquals('{"integer":1.1E+9}')
    %test:arg("int", "1.1e+9") %test:assertEquals('{"integer":1.1E+9}')
    %test:arg("int", "1.1e-9") %test:assertEquals('{"integer":1.1E-9}')
function xtj:xmlmap-to-json-for-exponent-fn-ns($int as xs:string) as xs:string {
    fn:xml-to-json(
        <fn:map>
            <fn:number key="integer">{$int}</fn:number>
        </fn:map>
    )
};

declare
    %test:arg("int", "1E9") %test:assertXPath('$result/fn:map/fn:number = ''1E9''')
    %test:arg("int", "1E+9") %test:assertXPath('$result/fn:map/fn:number = ''1E+9''')
    %test:arg("int", "1E-9") %test:assertXPath('$result/fn:map/fn:number = ''1E-9''')
    %test:arg("int", "1e9") %test:assertXPath('$result/fn:map/fn:number = ''1e9''')
    %test:arg("int", "1e+9") %test:assertXPath('$result/fn:map/fn:number = ''1e+9''')
    %test:arg("int", "1e-9") %test:assertXPath('$result/fn:map/fn:number = ''1e-9''')
    %test:arg("int", "1.1E9") %test:assertXPath('$result/fn:map/fn:number = ''1.1E9''')
    %test:arg("int", "1.1E+9") %test:assertXPath('$result/fn:map/fn:number = ''1.1E+9''')
    %test:arg("int", "1.1E-9") %test:assertXPath('$result/fn:map/fn:number = ''1.1E-9''')
    %test:arg("int", "1.1e9") %test:assertXPath('$result/fn:map/fn:number = ''1.1e9''')
    %test:arg("int", "1.1e+9") %test:assertXPath('$result/fn:map/fn:number = ''1.1e+9''')
    %test:arg("int", "1.1e-9") %test:assertXPath('$result/fn:map/fn:number = ''1.1e-9''')
function xtj:json-to-xmlmap-for-exponent($int as xs:string) as document-node() {
    fn:json-to-xml(
        '{"integer":' || $int || '}'
    )
};
