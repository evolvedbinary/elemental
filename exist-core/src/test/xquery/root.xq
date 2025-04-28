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

module namespace rt = "http://exist-db.org/xquery/test/fn-root";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare
    %test:assertEquals("true", "false", "false")
function rt:memtree-document() {
  let $x := document{()}
  return
    (
	    $x/root() instance of document-node(),
	    $x/ancestor::node() instance of document-node(),
	    $x/parent::node() instance of document-node()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "false", "false")
function rt:memtree-element() {
  let $x := element e1{}
  return
    (
	    $x/root() instance of document-node(),
	    $x/root() instance of element(),
	    $x/ancestor::node() instance of document-node(),
	    $x/ancestor::node() instance of element(),
	    $x/parent::node() instance of document-node(),
	    $x/parent::node() instance of element()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "false", "false")
function rt:memtree-attribute() {
  let $x := attribute a1{ "a1" }
  return
    (
	    $x/root() instance of document-node(),
	    $x/root() instance of attribute(),
	    $x/ancestor::node() instance of document-node(),
	    $x/ancestor::node() instance of attribute(),
	    $x/parent::node() instance of document-node(),
	    $x/parent::node() instance of attribute()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "false", "false")
function rt:memtree-comment() {
  let $x := comment { "c1" }
  return
    (
	    $x/root() instance of document-node(),
	    $x/root() instance of comment(),
	    $x/ancestor::node() instance of document-node(),
	    $x/ancestor::node() instance of comment(),
	    $x/parent::node() instance of document-node(),
	    $x/parent::node() instance of comment()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "false", "false")
function rt:memtree-processing-instruction() {
  let $x := processing-instruction p1 { "p1" }
  return
    (
	    $x/root() instance of document-node(),
	    $x/root() instance of processing-instruction(),
	    $x/ancestor::node() instance of document-node(),
	    $x/ancestor::node() instance of processing-instruction(),
	    $x/parent::node() instance of document-node(),
	    $x/parent::node() instance of processing-instruction()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "false", "false")
function rt:memtree-text() {
  let $x := text { "t1" }
  return
    (
        $x/root() instance of document-node(),
        $x/root() instance of text(),
        $x/ancestor::node() instance of document-node(),
        $x/ancestor::node() instance of text(),
        $x/parent::node() instance of document-node(),
        $x/parent::node() instance of text()
    )
};

declare
    %test:assertEquals("false", "true", "false", "true", "false", "true")
function rt:memtree-element-in-element() {
  let $x := element e1 {
    element e2{}
  }
  return
    (
	    root($x/e2) instance of document-node(),
        root($x/e2) instance of element(),
        $x/e2/ancestor::node() instance of document-node(),
        $x/e2/ancestor::node() instance of element(),
        $x/e2/parent::node() instance of document-node(),
        $x/e2/parent::node() instance of element()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "true", "false", "false", "true", "false")
function rt:memtree-attribute-in-element() {
  let $x := element e1 {
    attribute lang { "en" }
  }
  return
    (
        root($x/@lang) instance of document-node(),
        root($x/@lang) instance of element(),
        root($x/@lang) instance of attribute(),
        $x/@lang/ancestor::node() instance of document-node(),
        $x/@lang/ancestor::node() instance of element(),
        $x/@lang/ancestor::node() instance of attribute(),
        $x/@lang/parent::node() instance of document-node(),
        $x/@lang/parent::node() instance of element(),
        $x/@lang/parent::node() instance of attribute()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "true", "false", "false", "true", "false")
function rt:memtree-comment-in-element() {
  let $x := element e1 {
    comment { "c1" }
  }
  return
    (
        root($x/comment()) instance of document-node(),
        root($x/comment()) instance of element(),
        root($x/comment()) instance of comment(),
        $x/comment()/ancestor::node() instance of document-node(),
        $x/comment()/ancestor::node() instance of element(),
        $x/comment()/ancestor::node() instance of comment(),
        $x/comment()/parent::node() instance of document-node(),
        $x/comment()/parent::node() instance of element(),
        $x/comment()/parent::node() instance of comment()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "true", "false", "false", "true", "false")
function rt:memtree-processing-instruction-in-element() {
  let $x := element e1 {
    processing-instruction p1 { "p1" }
  }
  return
    (
        root($x/processing-instruction()) instance of document-node(),
        root($x/processing-instruction()) instance of element(),
        root($x/processing-instruction()) instance of processing-instruction(),
        $x/processing-instruction()/ancestor::node() instance of document-node(),
        $x/processing-instruction()/ancestor::node() instance of element(),
        $x/processing-instruction()/ancestor::node() instance of processing-instruction(),
        $x/processing-instruction()/parent::node() instance of document-node(),
        $x/processing-instruction()/parent::node() instance of element(),
        $x/processing-instruction()/parent::node() instance of processing-instruction()
    )
};

declare
    %test:assertEquals("false", "true", "false", "false", "true", "false", "false", "true", "false")
function rt:memtree-text-in-element() {
  let $x := element e1 {
    text { "t1" }
  }
  return
    (
        root($x/text()) instance of document-node(),
        root($x/text()) instance of element(),
        root($x/text()) instance of text(),
        $x/text()/ancestor::node() instance of document-node(),
        $x/text()/ancestor::node() instance of element(),
        $x/text()/ancestor::node() instance of text(),
        $x/text()/parent::node() instance of document-node(),
        $x/text()/parent::node() instance of element(),
        $x/text()/parent::node() instance of text()
    )
};