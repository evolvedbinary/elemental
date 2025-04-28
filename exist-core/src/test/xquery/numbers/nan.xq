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
xquery version "3.0";

module namespace nan = "http://exist-db.org/xquery/test/nan";

declare namespace test = "http://exist-db.org/xquery/xqsuite";


declare
    %test:assertFalse
function nan:nan-atomic-eq-nan() {
    number(()) eq number(())
};

declare
    %test:assertFalse
function nan:nan-atomic-eq-1() {
    number(()) eq 1
};

declare
    %test:assertFalse
function nan:nan-atomic-lt-nan() {
    number(()) lt number(())
};

declare
    %test:assertFalse
function nan:nan-atomic-lt-1() {
    number(()) lt 1
};

declare
    %test:assertFalse
function nan:nan-atomic-le-nan() {
    number(()) le number(())
};

declare
    %test:assertFalse
function nan:nan-atomic-le-1() {
    number(()) le 1
};

declare
    %test:assertFalse
function nan:nan-atomic-gt-nan() {
    number(()) gt number(())
};

declare
    %test:assertFalse
function nan:nan-atomic-gt-1() {
    number(()) gt 1
};

declare
    %test:assertFalse
function nan:nan-atomic-ge-nan() {
    number(()) ge number(())
};

declare
    %test:assertFalse
function nan:nan-atomic-ge-1() {
    number(()) ge 1
};

declare
    %test:assertTrue
function nan:nan-atomic-ne-nan() {
    number(()) ne number(())
};

declare
    %test:assertTrue
function nan:nan-atomic-ne-1() {
    number(()) ne 1
};

declare
    %test:assertFalse
function nan:nan-sequence-eq-nan() {
    number(()) = number(())
};

declare
    %test:assertFalse
function nan:nan-sequence-eq-1() {
    number(()) = 1
};

declare
    %test:assertFalse
function nan:nan-sequence-lt-nan() {
    number(()) < number(())
};

declare
    %test:assertFalse
function nan:nan-sequence-lt-1() {
    number(()) < 1
};

declare
    %test:assertFalse
function nan:nan-sequence-le-nan() {
    number(()) <= number(())
};

declare
    %test:assertFalse
function nan:nan-sequence-le-1() {
    number(()) <= 1
};

declare
    %test:assertFalse
function nan:nan-sequence-gt-nan() {
    number(()) > number(())
};

declare
    %test:assertFalse
function nan:nan-sequence-gt-1() {
    number(()) > 1
};

declare
    %test:assertFalse
function nan:nan-sequence-ge-nan() {
    number(()) >= number(())
};

declare
    %test:assertFalse
function nan:nan-sequence-ge-1() {
    number(()) >= 1
};

declare
    %test:assertTrue
function nan:nan-sequence-ne-nan() {
    number(()) != number(())
};

declare
    %test:assertTrue
function nan:nan-sequence-ne-1() {
    number(()) != 1
};
