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
xquery version "3.0";

(:~
 : Tests for Processing Instruction nodes.
 :
 : @author Adam Retter
 :)
module namespace pi = "http://exist-db.org/xquery/test/pi";

import module namespace test = "http://exist-db.org/xquery/xqsuite";

declare %private variable $pi:doc1 := document {
    <?my-pi my-pi-content ?>,
    <x/>
};

declare
    %test:setUp
function pi:setup() {
  let $test-collection := xmldb:create-collection("/db", "pi-test")
  return
    xmldb:store($test-collection, "doc1.xml", $pi:doc1)
};

declare
    %test:tearDown
function pi:tearDown() {
  xmldb:remove("/db/pi-test")
};

declare
    %test:assertEquals("my-pi")
function pi:in-memory-dom-pi-name() {
    $pi:doc1/processing-instruction()/name()
};

declare
    %test:assertEquals("my-pi")
function pi:persistent-dom-pi-name() {
    doc("/db/pi-test/doc1.xml")/processing-instruction()/name()
};
