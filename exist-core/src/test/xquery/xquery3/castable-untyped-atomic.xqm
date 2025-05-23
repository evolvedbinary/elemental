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

(:
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
 (:~
  : @see https://github.com/eXist-db/exist/issues/4518
  :)
xquery version "3.1";

module namespace ca="http://exist-db.org/xquery/test";

declare namespace test="http://exist-db.org/xquery/xqsuite";

(: atomic values not explicitly converted to strings were falsely returning false to _castable_as_
   in other words, all these tests were returning nc="false"
:)
declare
%test:args("ixml") %test:assertEquals("<result name=""ixml"" nc=""true""/>")
%test:args("comment") %test:assertEquals("<result name=""comment"" nc=""true""/>")
%test:args("apple-jack") %test:assertEquals("<result name=""apple-jack"" nc=""true""/>")
%test:args("123") %test:assertEquals("<result name=""123"" nc=""false""/>")
%test:args("élève") %test:assertEquals("<result name=""élève"" nc=""true""/>")
%test:args("pfx:suffix") %test:assertEquals("<result name=""pfx:suffix"" nc=""false""/>")
function ca:n-castable($d) {
    let $doc := <e name="e"><f n="{$d}"/></e>
    let $el := $doc/descendant::*[exists(@n)]
    let $n := $el/@n
    return <result name="{$n}" nc="{$n castable as xs:NCName}"/>
};

(: explicit string conversion was always working - for reference :)
declare
%test:args("ixml") %test:assertEquals("<result name=""ixml"" nc=""true""/>")
%test:args("comment") %test:assertEquals("<result name=""comment"" nc=""true""/>")
%test:args("apple-jack") %test:assertEquals("<result name=""apple-jack"" nc=""true""/>")
%test:args("123") %test:assertEquals("<result name=""123"" nc=""false""/>")
%test:args("élève") %test:assertEquals("<result name=""élève"" nc=""true""/>")
%test:args("pfx:suffix") %test:assertEquals("<result name=""pfx:suffix"" nc=""false""/>")
function ca:s-castable($d) {
    let $doc := <e name="e"><f n="{$d}"/></e>
    let $el := $doc/descendant::*[exists(@n)]
    let $n := $el/@n, $s := string($n)
    return <result name="{$n}" nc="{$s castable as xs:NCName}"/>
};

(: sanity check that casting accepts the same values as castable as :)
declare
%test:args("ixml") %test:assertEquals("<result name=""ixml"" nc=""true""/>")
%test:args("comment") %test:assertEquals("<result name=""comment"" nc=""true""/>")
%test:args("apple-jack") %test:assertEquals("<result name=""apple-jack"" nc=""true""/>")
%test:args("123") %test:assertError("err:")
%test:args("élève") %test:assertEquals("<result name=""élève"" nc=""true""/>")
%test:args("pfx:suffix") %test:assertError("err:")
function ca:s-cast-castable($d) {
    let $doc := <e name="e"><f n="{$d}"/></e>
    let $el := $doc/descendant::*[exists(@n)]
    let $n := $el/@n, $ncname := xs:NCName($n)
    return <result name="{$n}" nc="{$ncname castable as xs:NCName}"/>
};
