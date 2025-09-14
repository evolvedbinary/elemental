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
    <F id="1"/>
};

declare
    %test:setUp
function t:setup() {
    xmldb:create-collection("/db", "test"),
    xmldb:store("/db/test", "test.xml", $t:XML)
};

declare
    %test:tearDown
function t:tearDown() {
    xmldb:remove("/db/test")
};

declare
    %test:assertTrue
function t:test-db() {
    exists(
        doc("/db/test/test.xml")//F[boolean(count(@id >= 2))]
    )
};

declare
    %test:assertTrue
function t:test-mem() {
    exists(
        $t:XML//F[boolean(count(@id >= 2))]
    )
};