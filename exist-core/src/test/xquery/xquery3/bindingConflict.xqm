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

module namespace ut="http://exist-db.org/xquery/update/test";

declare namespace test="http://exist-db.org/xquery/xqsuite";
declare namespace xmldb="http://exist-db.org/xquery/xmldb";

declare namespace myns="http://www.foo.com";
declare namespace myns2="http://www.foo.net";

(: insert node into a ns with a conflicting ns in parent tree :)
declare %test:assertError("XUDY0023")
function ut:insert-child-namespaced-attr-conflicted() {
    let $f  := xmldb:store('/db', 'xupdate.xml', <root xmlns:myns="http://www.bar.com" attr="1"><!-- foobar --><z blah="wah"/></root>)
    let $u  := update insert <child myns:baz="qux"/> into doc($f)/root/z
    return doc($f)
};

(: insert attr into a ns, but nothing contradictory in the tree - should add ns node :)
declare %test:assertEquals("<z blah=""wah""><child xmlns:myns=""http://www.foo.com"" myns:baz=""qux""/></z>")
function ut:insert-child-namespaced-attr() {
    let $f  := xmldb:store('/db', 'xupdate.xml', <root attr="1"><!-- foobar --><z blah="wah"/></root>)
    let $u  := update insert <child myns:baz="qux"/> into doc($f)/root/z
    return doc($f)/root/z
};

(: insert attr into a ns, but nothing contradictory in the tree - should add ns node :)
declare %test:assertEquals("<z blah=""wah""><myns:child xmlns:myns=""http://www.foo.com"" baz=""qux""/></z>")
function ut:insert-namespaced-child() {
    let $f  := xmldb:store('/db', 'xupdate.xml', <root attr="1"><!-- foobar --><z blah="wah"/></root>)
    let $u  := update insert <myns:child baz="qux"/> into doc($f)/root/z
    return doc($f)/root/z
};

(: We "manually" redefined xmlns:myns in <grand> -- what does the code see in <great myns:boz="chux"..>, and should we reject it ? :)
(: Do we need to code up the added namespaces, and check conflicts, thus this would XUDY0023 :)
(: or are we content to ignore manual redefinitions :)
declare %test:assertEquals("<z blah=""wah""><myns:child xmlns:myns=""http://www.foo.com"" baz=""qux""><grand xmlns:myns=""http://www.fubar.com""><great xmlns:myns2=""http://www.foo.net"" myns:boz=""chux"" myns2:pip=""dickens""/></grand></myns:child></z>")
function ut:insert-namespaced-child-deep() {
    let $f  := xmldb:store('/db', 'xupdate.xml', <root attr="1"><!-- foobar --><z blah="wah"/></root>)
    let $u  := update insert <myns:child baz="qux"><grand xmlns:myns="http://www.fubar.com"><great myns:boz="chux" myns2:pip="dickens"/></grand></myns:child> into doc($f)/root/z
    return fn:serialize(doc($f)/root/z)
};

(: insert attr into a ns, but nothing contradictory in the tree - should add ns node :)
declare %test:assertError("XUDY0023")
function ut:insert-namespaced-child-conflicted() {
    let $f  := xmldb:store('/db', 'xupdate.xml', <root xmlns:myns="http://www.bar.com" attr="1"><!-- foobar --><z blah="wah"/></root>)
    let $u  := update insert <myns:child baz="qux"/> into doc($f)/root/z
    return doc($f)/root/z
};

(: insert attr into a ns with a conflicting ns in parent tree :)
declare %test:assertError("XUDY0023")
function ut:insert-namespaced-attr-conflicted() {
    let $f  := xmldb:store('/db', 'xupdate.xml', <root xmlns:myns="http://www.bar.com" attr="1"><!-- foobar --><z blah="wah"/></root>)
    let $u  := update insert attribute myns:baz { "qux" } into doc($f)/root/z
    return doc($f)
};

(: insert attr into a ns, but nothing contradictory in the tree - should add ns node :)
declare %test:assertEquals("<z xmlns:myns=""http://www.foo.com"" blah=""wah"" myns:baz=""qux""/>")
function ut:insert-namespaced-attr() {
    let $f  := xmldb:store('/db', 'xupdate.xml', <root attr="1"><!-- foobar --><z blah="wah"/></root>)
    let $u  := update insert attribute myns:baz { "qux" } into doc($f)/root/z
    return doc($f)/root/z
};
