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

module namespace acl = "http://exist-db.org/test/securitymanager/acl";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace sm = "http://exist-db.org/xquery/securitymanager";

declare variable $acl:test-collection-name := "test-acl";
declare variable $acl:test-collection := xs:anyURI("/db/" || $acl:test-collection-name);

declare
    %test:setUp
function acl:setup() {
    xmldb:create-collection("/db", $acl:test-collection-name)
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="1"><ace index="0" target="USER" who="guest" access_type="ALLOWED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-user-ace-same-access-same-mode-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rwx")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="USER" who="guest" access_type="ALLOWED" mode="rw-"/><ace index="1" target="USER" who="admin" access_type="ALLOWED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-user-ace-same-access-same-mode-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rw-")
    let $_ := sm:add-user-ace($acl:test-collection, "admin", true(), "rwx")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rw-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="1"><ace index="0" target="USER" who="guest" access_type="ALLOWED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-user-ace-different-access-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rwx")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", false(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="1"><ace index="0" target="USER" who="guest" access_type="DENIED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-user-ace-different-access-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", false(), "rwx")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="USER" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="USER" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-user-ace-same-access-different-mode-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "-w-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="USER" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="USER" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-user-ace-same-access-different-mode-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="USER" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="USER" who="guest" access_type="DENIED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-user-ace-different-access-different-mode-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", false(), "-w-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="USER" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="USER" who="guest" access_type="DENIED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-user-ace-different-access-different-mode-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", false(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="USER" who="guest" access_type="DENIED" mode="r-x"/><ace index="1" target="USER" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-user-ace-different-access-different-mode-3() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", false(), "r-x")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "-w-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="USER" who="guest" access_type="DENIED" mode="r-x"/><ace index="1" target="USER" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-user-ace-different-access-different-mode-4() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-user-ace($acl:test-collection, "guest", false(), "r-x")
    let $_ := sm:add-user-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="1"><ace index="0" target="GROUP" who="guest" access_type="ALLOWED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-group-ace-same-access-same-mode-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rwx")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="GROUP" who="guest" access_type="ALLOWED" mode="rw-"/><ace index="1" target="GROUP" who="dba" access_type="ALLOWED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-group-ace-same-access-same-mode-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rw-")
    let $_ := sm:add-group-ace($acl:test-collection, "dba", true(), "rwx")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rw-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="1"><ace index="0" target="GROUP" who="guest" access_type="ALLOWED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-group-ace-different-access-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rwx")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", false(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="1"><ace index="0" target="GROUP" who="guest" access_type="DENIED" mode="rwx"/></acl>')
function acl:non-duplicate-contiguous-group-ace-different-access-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", false(), "rwx")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="GROUP" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="GROUP" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-group-ace-same-access-different-mode-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "-w-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="GROUP" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="GROUP" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-group-ace-same-access-different-mode-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="GROUP" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="GROUP" who="guest" access_type="DENIED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-group-ace-different-access-different-mode-1() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", false(), "-w-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="GROUP" who="guest" access_type="ALLOWED" mode="r-x"/><ace index="1" target="GROUP" who="guest" access_type="DENIED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-group-ace-different-access-different-mode-2() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "r-x")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", false(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="GROUP" who="guest" access_type="DENIED" mode="r-x"/><ace index="1" target="GROUP" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-group-ace-different-access-different-mode-3() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", false(), "r-x")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "-w-")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};

declare
    %test:assertEquals('<acl xmlns="http://exist-db.org/xquery/securitymanager" entries="2"><ace index="0" target="GROUP" who="guest" access_type="DENIED" mode="r-x"/><ace index="1" target="GROUP" who="guest" access_type="ALLOWED" mode="-w-"/></acl>')
function acl:non-duplicate-contiguous-group-ace-different-access-different-mode-4() {
    let $_ := sm:clear-acl($acl:test-collection)

    let $_ := sm:add-group-ace($acl:test-collection, "guest", false(), "r-x")
    let $_ := sm:add-group-ace($acl:test-collection, "guest", true(), "rwx")

    return
        sm:get-permissions($acl:test-collection)/sm:permission/sm:acl
};
