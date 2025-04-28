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

(:~
 : Test for instance of operator.
 :
 : @author Adam Retter
 :)
module namespace io = "http://exist-db.org/xquery/test/instance-of";

import module namespace test = "http://exist-db.org/xquery/xqsuite";

declare
    %test:assertEquals("true", "false", "false", "false", "true", "false")
function io:double() {
  xs:double(1.0) instance of xs:double,
  xs:double(1.0) instance of xs:float,               (: false :)
  xs:double(1.0) instance of xs:decimal,             (: false :)
  xs:double(1.0) instance of xs:integer,             (: false :)
  xs:double(1.0) instance of xs:numeric,
  xs:double(-1.0) instance of xs:nonPositiveInteger  (: false :)
};

declare
    %test:assertEquals("false", "true", "false", "false", "true", "false")
function io:float() {
  xs:float(1.0) instance of xs:double,              (: false :)
  xs:float(1.0) instance of xs:float,
  xs:float(1.0) instance of xs:decimal,             (: false :)
  xs:float(1.0) instance of xs:integer,             (: false :)
  xs:float(1.0) instance of xs:numeric,
  xs:float(-1.0) instance of xs:nonPositiveInteger  (: false :)
};

declare
    %test:assertEquals("false", "false", "true", "false", "true", "false")
function io:decimal() {
  xs:decimal(1.0) instance of xs:double,              (: false :)
  xs:decimal(1.0) instance of xs:float,               (: false :)
  xs:decimal(1.0) instance of xs:decimal,
  xs:decimal(1.0) instance of xs:integer,             (: false :)
  xs:decimal(1.0) instance of xs:numeric,
  xs:decimal(-1.0) instance of xs:nonPositiveInteger  (: false :)
};

declare
    %test:assertEquals("false", "false", "true", "true", "true", "false")
function io:integer() {
  xs:integer(1) instance of xs:double,              (: false :)
  xs:integer(1) instance of xs:float,               (: false :)
  xs:integer(1) instance of xs:decimal,
  xs:integer(1) instance of xs:integer,
  xs:integer(1) instance of xs:numeric,
  xs:integer(-1) instance of xs:nonPositiveInteger  (: false :)
};

declare
    %test:assertEquals("false", "false", "true", "true", "true", "true")
function io:nonPositiveInteger() {
  xs:nonPositiveInteger(-1) instance of xs:double,  (: false :)
  xs:nonPositiveInteger(-1) instance of xs:float,   (: false :)
  xs:nonPositiveInteger(-1) instance of xs:decimal,
  xs:nonPositiveInteger(-1) instance of xs:integer,
  xs:nonPositiveInteger(-1) instance of xs:numeric,
  xs:nonPositiveInteger(-1) instance of xs:nonPositiveInteger
};

declare
    %test:assertEquals("false", "false", "true", "false", "true", "false")
function io:numeric-decimal() {
  xs:numeric(1.0) instance of xs:double,              (: false :)
  xs:numeric(1.0) instance of xs:float,               (: false :)
  xs:numeric(1.0) instance of xs:decimal,             (: true - NOTE(AR) for an explanation of why this is true, see: https://xmlcom.slack.com/archives/C011NLXE4DU/p1676291235309039 :)
  xs:numeric(1.0) instance of xs:integer,             (: false :)
  xs:numeric(1.0) instance of xs:numeric,
  xs:numeric(-1.0) instance of xs:nonPositiveInteger  (: false :)
};

declare
    %test:assertEquals("false", "false", "true", "true", "true", "false")
function io:numeric-integer() {
  xs:numeric(1) instance of xs:double,                (: false :)
  xs:numeric(1) instance of xs:float,                 (: false :)
  xs:numeric(1) instance of xs:decimal,
  xs:numeric(1) instance of xs:integer,
  xs:numeric(1) instance of xs:numeric,
  xs:numeric(-1) instance of xs:nonPositiveInteger    (: false :)
};
