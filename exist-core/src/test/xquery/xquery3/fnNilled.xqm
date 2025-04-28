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

module namespace testNilled="http://exist-db.org/xquery/test/fnNilled";

declare namespace test="http://exist-db.org/xquery/xqsuite";

declare variable $testNilled:simple_node := <node>"1"</node>;

declare
    %test:assertError("err:XPDY0002")
function testNilled:nilled-no-param() {
    fn:nilled()
};

declare
    %test:assertFalse
function testNilled:nilled-node-is-false() {
    fn:nilled($testNilled:simple_node)
};

declare
    %test:assertExists
function testNilled:nilled-node-exists() {
    fn:nilled($testNilled:simple_node)
};

declare variable $testNilled:empty_seq := ();

declare
    %test:assertFalse
function testNilled:nilled-empty-is-false() {
    fn:nilled($testNilled:empty_seq)
};

declare
    %test:assertEmpty
function testNilled:nilled-empty-is-empty() {
    fn:nilled($testNilled:empty_seq)
};

declare variable $testNilled:one_element := <node>"just-one-element"</node>;

declare
    %test:assertFalse
function testNilled:nilled-single-is-false() {
    fn:nilled($testNilled:one_element)
};

declare
    %test:assertExists
function testNilled:nilled-single-is-not-empty() {
    fn:nilled($testNilled:one_element)
};

declare
    %test:args("one")
    %test:assertError("err:XPTY0004")
function testNilled:nilled-string-not-a-node($param) {
    fn:nilled($param)
};

declare variable $testNilled:two_elements := ("one","two");

declare
    %test:assertError("XPTY0004")
function testNilled:nilled-p2() {
    fn:nilled($testNilled:two_elements)
};

declare variable $testNilled:one_element_in_conjunction := <shoe xsi:nil="{fn:true()}"/>;

declare
    %test:assertFalse
function testNilled:nilled-conjunction-is-false() {
    fn:nilled($testNilled:one_element_in_conjunction) and fn:true()
};


declare
    %test:assertFalse
function testNilled:nilled-conjunction-element-is-false() {
    fn:nilled($testNilled:one_element_in_conjunction)
};

