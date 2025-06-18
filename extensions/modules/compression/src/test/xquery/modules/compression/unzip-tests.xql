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
xquery version "3.0";

module namespace ut = "http://exist-db.org/xquery/cache/test/unzip";

import module namespace compression = "http://exist-db.org/xquery/compression";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace util = "http://exist-db.org/xquery/util";


declare variable $ut:collection-name := "unzip-test";
declare variable $ut:collection := "/db/" || $ut:collection-name;


declare variable $ut:myFile-name := "!#$%()*+,-.:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{}~ ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿αßΓπΣσµτΦΘΩδ∞φε.xml";
declare variable $ut:myFile-serialized := "<file/>";

(: declare UTF8 encoded binary :)
declare variable $ut:myStaticUTF8ContentBase64 := xs:base64Binary("UEsDBBQACAgIAOBYl0UAAAAAAAAAAAAAAADCAAAAISMkJSgpKissLS46Oz0/QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW11eX2FiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e31+IMOHw7zDqcOiw6TDoMOlw6fDqsOrw6jDr8Ouw6zDhMOFw4nDpsOGw7TDtsOyw7vDucO/w5bDnMKiwqPCpeKCp8aSw6HDrcOzw7rDscORwqrCusK/zrHDn86Tz4DOo8+DwrXPhM6mzpjOqc604oiez4bOtS54bWyzsa/IzVEoSy0qzszPs1Uy1DNQUkjNS85PycxLt1UKDXHTtVCyt+OyScvMSdW3AwBQSwcIwWbL3zAAAAAuAAAAUEsBAhQAFAAICAgA4FiXRcFmy98wAAAALgAAAMIAAAAAAAAAAAAAAAAAAAAAACEjJCUoKSorLC0uOjs9P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltdXl9hYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ent9fiDDh8O8w6nDosOkw6DDpcOnw6rDq8Oow6/DrsOsw4TDhcOJw6bDhsO0w7bDssO7w7nDv8OWw5zCosKjwqXigqfGksOhw63Ds8O6w7HDkcKqwrrCv86xw5/Ok8+AzqPPg8K1z4TOps6YzqnOtOKIns+GzrUueG1sUEsFBgAAAAABAAEA8AAAACABAAAAAA==");

(: declare cp437 encoded binary :)
declare variable $ut:myStaticCP437ContentBase64 := xs:base64Binary("UEsDBBQACAAIAOBYl0UAAAAAAAAAAAAAAACIAAAAISMkJSgpKissLS46Oz0/QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW11eX2FiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e31+IICBgoOEhYaHiImKi4yNjo+QkZKTlJWWl5iZmpucnZ6foKGio6Slpqeo4OHi4+Tl5ufo6err7O3uLnhtbLOxr8jNUShLLSrOzM+zVTLUM1BSSM1Lzk/JzEu3VQoNcdO1ULK347JJy8xJ1bcDAFBLBwjBZsvfMAAAAC4AAABQSwECFAAUAAgACADgWJdFwWbL3zAAAAAuAAAAiAAAAAAAAAAAAAAAAAAAAAAAISMkJSgpKissLS46Oz0/QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW11eX2FiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e31+IICBgoOEhYaHiImKi4yNjo+QkZKTlJWWl5iZmpucnZ6foKGio6Slpqeo4OHi4+Tl5ufo6err7O3uLnhtbFBLBQYAAAAAAQABALYAAADmAAAAAAA=");


(: declare helper functions :)
declare function local:entry-data($path as xs:string, $type as xs:string, $data as item()?, $param as item()*) as item()?
{
    <entry>
        <path>{$path}</path>
        <type>{$type}</type>
        <data>{$data}</data>
    </entry>
};

(: Process every Zip Collections and Resources  :)
declare function local:entry-filter($path as xs:string, $type as xs:string, $param as item()*) as xs:boolean
{
    true()
};

declare
    %test:user("guest", "guest")
	%test:assertEquals("<entry><path>!#$%()*+,-.:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{}~ ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿αßΓπΣσµτΦΘΩδ∞φε.xml</path><type>resource</type><data><file/></data></entry>")
function ut:fnUzipUtf8Content() {
    compression:unzip($ut:myStaticUTF8ContentBase64, local:entry-filter#3, (), local:entry-data#4, (), "UTF8")
};

declare
    %test:user("guest", "guest")
	%test:assertEquals("<entry><path>!#$%()*+,-.:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{}~ ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿αßΓπΣσµτΦΘΩδ∞φε.xml</path><type>resource</type><data><file/></data></entry>")
function ut:fnUzipCp437Content() {
    compression:unzip($ut:myStaticCP437ContentBase64, local:entry-filter#3, (), local:entry-data#4, (), "Cp437")
};

declare
    %test:user("guest", "guest")
	%test:assertEquals("<entry><path>!#$%()*+,-.:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{}~ ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿αßΓπΣσµτΦΘΩδ∞φε.xml</path><type>resource</type><data><file/></data></entry>")
function ut:fnUzipUtf8ContentWrongEncoding() {
    (: This case is working due to the selected cp437 character in the filename :)
    compression:unzip($ut:myStaticUTF8ContentBase64, local:entry-filter#3, (), local:entry-data#4, (), "Cp437")
};

declare
    %test:user("guest", "guest")
	%test:assertError("(?:MALFORMED)|(?:malformed)")
function ut:fnUzipCp437ContentWrongEncoding() {
    (: This case is not working because the Unicode extended filename table is not present in non unicode encoded Zip :)
    compression:unzip($ut:myStaticCP437ContentBase64, local:entry-filter#3, (), local:entry-data#4, (), "UTF8")
};
