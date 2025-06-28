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

(:~ Additional tests for the fn:parse-xml and fn:parse-xml-fragment functions :)
module namespace px="http://exist-db.org/xquery/test/parse-xml";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare
    %test:assertEmpty
function px:fragment-type-1() {
    fn:parse-xml-fragment(())
};

declare
    %test:assertTrue
function px:fragment-type-2() {
    fn:parse-xml-fragment("") instance of document-node()
};

declare
    %test:assertEmpty
function px:fragment-children-1() {
    fn:parse-xml-fragment("")/node()
};

declare
    %test:assertTrue
function px:fragment-type-3() {
    fn:parse-xml-fragment(" ") instance of document-node()
};

declare
    %test:assertTrue(" ")
function px:fragment-children-2() {
    fn:parse-xml-fragment(" ")/node()
};

declare
    %test:assertTrue
function px:fragment-type-4() {
    fn:parse-xml-fragment("<alpha>abcd</alpha><beta>abcd</beta>") instance of document-node()
};

declare
    %test:assertEquals("<alpha>abcd</alpha>", "<beta>abcd</beta>")
function px:fragment-children-3() {
    fn:parse-xml-fragment("<alpha>abcd</alpha><beta>abcd</beta>")/node()
};

declare
    %test:assertTrue
function px:fragment-type-5() {
    fn:parse-xml-fragment("He was <i>so</i> kind") instance of document-node()
};

declare
    %test:assertEquals(1)
function px:fragment-count() {
    count(parse-xml-fragment("He was <i>so</i> kind"))
};

declare
    %test:assertEquals(3)
function px:fragment-node-count() {
    count(parse-xml-fragment("He was <i>so</i> kind")/node())
};

declare
    %test:assertTrue
function px:fragment-xml-decl() {
    fn:parse-xml-fragment('<?xml version="1.0"?><a/>') instance of document-node()
};

declare
    %test:assertError("FODC0006")
function px:fragment-xml-decl-standalone-yes() {
    fn:parse-xml-fragment('<?xml version="1.0" standalone="yes"?><a/>')
};

declare
    %test:assertError("FODC0006")
function px:fragment-xml-decl-standalone-no() {
    fn:parse-xml-fragment('<?xml version="1.0" standalone="no"?><a/>')
};

declare
    %test:assertTrue
function px:fragment-xml-decl-encoding() {
    fn:parse-xml-fragment('<?xml version="1.0" encoding="utf8"?><a/>') instance of document-node()
};

declare
    %test:assertError("FODC0006")
function px:fragment-xml-decl-encoding-standalone-yes() {
    fn:parse-xml-fragment('<?xml version="1.0" encoding="utf8" standalone="yes"?><a/>')
};

declare
    %test:assertError("FODC0006")
function px:fragment-xml-decl-encoding-standalone-no() {
    fn:parse-xml-fragment('<?xml version="1.0" encoding="utf8" standalone="no"?><a/>')
};
