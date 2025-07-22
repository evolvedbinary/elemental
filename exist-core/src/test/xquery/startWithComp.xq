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
 :)
xquery version "3.1";

module namespace t="http://exist-db.org/xquery/test";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $t:XML := document {
    <Y1 id="1">
        <H1 id="2" a2="2">true</H1>
    </Y1>
};

declare
    %test:setUp
function t:setup() {
    let $testCol := xmldb:create-collection("/db", "test")
    return
        xmldb:store("/db/test", "test.xml", $t:XML)
};

declare
    %test:tearDown
function t:tearDown() {
    xmldb:remove("/db/test")
};

declare
    %test:assertTrue
function t:test() {
    doc("/db/test/test.xml")//*[starts-with(@a2, "1") = false()]
    => exists()
};