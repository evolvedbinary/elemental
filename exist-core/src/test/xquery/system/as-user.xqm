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

module namespace sysau = "http://exist-db.org/test/system/as-user";

import module namespace sm = "http://exist-db.org/xquery/securitymanager";
import module namespace system = "http://exist-db.org/xquery/system";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare
    %test:assertEquals('admin')
function sysau:function-as-user-admin-inline() {
    system:function-as-user("admin", "", function() {
        (sm:id()//sm:username)[last()]/string(.)
    })
};

declare
    %test:assertEquals('guest')
function sysau:function-as-user-guest-inline() {
    system:function-as-user("guest", "guest", function() {
        (sm:id()//sm:username)[last()]/string(.)
    })
};

declare
    %test:assertEquals('admin')
function sysau:function-as-user-admin-reference() {
    system:function-as-user("admin", "", sysau:get-effective-user-id#0)
};

declare
    %test:assertEquals('guest')
function sysau:function-as-user-guest-reference() {
    system:function-as-user("guest", "guest", sysau:get-effective-user-id#0)
};

declare
    %test:assertError
function sysau:function-as-user-unknown() {
    system:function-as-user("unknown", "", function() {
        ()
    })
};

declare
    %private
function sysau:get-effective-user-id() as xs:string {
    (sm:id()//sm:username)[last()]/string(.)
};
