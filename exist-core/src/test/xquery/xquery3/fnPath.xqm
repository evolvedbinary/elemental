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

module namespace fnp="http://exist-db.org/xquery/test/function_path";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare
    %test:assertEmpty
function fnp:empty() {
    fn:path()
};

declare
    %test:assertEquals("/Q{http://example.com/one}p[1]/@author")
function fnp:author() {
    let $e := document {
        <p xmlns="http://example.com/one" xml:lang="de" author="Friedrich von Schiller">
        Freude, schoner Gotterfunken,<br/>
        Tochter aus Elysium,<br/>
        Wir betreten feuertrunken,<br/>
        Himmlische, dein Heiligtum.</p>
    }
    return fn:path($e/*:p/@author)
};

declare
    %test:assertEquals("/Q{http://example.com/one}p[1]/Q{http://example.com/one}br[2]")
function fnp:br() {
    let $e := document {
        <p xmlns="http://example.com/one" xml:lang="de" author="Friedrich von Schiller">
        Freude, schoner Gotterfunken,<br/>
        Tochter aus Elysium,<br/>
        Wir betreten feuertrunken,<br/>
        Himmlische, dein Heiligtum.</p>
    }
    return fn:path($e/*:p/*:br[2])
};

declare
    %test:assertEquals("/")
function fnp:document() {
    let $e := document {
        <p xmlns="http://example.com/one" xml:lang="de" author="Friedrich von Schiller">
        Freude, schoner Gotterfunken,<br/>
        Tochter aus Elysium,<br/>
        Wir betreten feuertrunken,<br/>
        Himmlische, dein Heiligtum.</p>
    }
    return fn:path($e)
};

declare
    %test:assertEquals("Q{http://www.w3.org/2005/xpath-functions}root()/Q{}empnr[1]")
function fnp:empnr() {
    let $e := <employee xml:id="ID21256">
                  <empnr>E21256</empnr>
                  <first>John</first>
                  <last>Brown</last>
              </employee>

    return fn:path($e/empnr)
};

declare
    %test:assertEquals("Q{http://www.w3.org/2005/xpath-functions}root()/@Q{http://www.w3.org/XML/1998/namespace}id")
function fnp:id() {
    let $e := <employee xml:id="ID21256">
                  <empnr>E21256</empnr>
                  <first>John</first>
                  <last>Brown</last>
              </employee>

    return fn:path($e/@xml:id)
};

declare
    %test:assertEquals("/Q{http://example.com/one}p[1]/@Q{http://www.w3.org/XML/1998/namespace}lang")
function fnp:lang() {
    let $e := document {
        <p xmlns="http://example.com/one" xml:lang="de" author="Friedrich von Schiller">
        Freude, schoner Gotterfunken,<br/>
        Tochter aus Elysium,<br/>
        Wir betreten feuertrunken,<br/>
        Himmlische, dein Heiligtum.</p>
    }
    return fn:path($e/*:p/@xml:lang)
};

declare
    %test:assertEquals("/Q{http://example.com/one}p[1]")
function fnp:p() {
    let $e := document {
        <p xmlns="http://example.com/one" xml:lang="de" author="Friedrich von Schiller">
        Freude, schoner Gotterfunken,<br/>
        Tochter aus Elysium,<br/>
        Wir betreten feuertrunken,<br/>
        Himmlische, dein Heiligtum.</p>
    }
    return fn:path($e/*:p)
};

declare
    %test:assertEquals("Q{http://www.w3.org/2005/xpath-functions}root()")
function fnp:root() {
    let $e := <employee xml:id="ID21256">
                  <empnr>E21256</empnr>
                  <first>John</first>
                  <last>Brown</last>
              </employee>

    return fn:path($e)
};

declare
    %test:assertEquals("/Q{http://example.com/one}p[1]/text()[2]")
function fnp:text() {
    let $e := document {
        <p xmlns="http://example.com/one" xml:lang="de" author="Friedrich von Schiller">
        Freude, schoner Gotterfunken,<br/>
        Tochter aus Elysium,<br/>
        Wir betreten feuertrunken,<br/>
        Himmlische, dein Heiligtum.</p>
    }
    return fn:path($e//text()[starts-with(normalize-space(), 'Tochter')])
};
