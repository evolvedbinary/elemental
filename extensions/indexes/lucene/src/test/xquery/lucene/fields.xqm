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

module namespace fields = "http://exist-db.org/xquery/lucene/test/fields";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace util = "http://exist-db.org/xquery/util";

import module namespace ft = "http://exist-db.org/xquery/lucene";

declare variable $fields:COLLECTION_CONF := document {
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index xmlns:xs="http://www.w3.org/2001/XMLSchema">
            <lucene>
                <text qname="date">
                    <field name="date"/>
                    <field name="date-binary" binary="true"/>
                    <field name="date-typed-date" type="xs:date"/>
                    <field name="date-typed-date-binary" type="xs:date" binary="true"/>
                </text>
                <text qname="time">
                    <field name="time"/>
                    <field name="time-binary" binary="true"/>
                    <field name="time-typed-time" type="xs:time"/>
                    <field name="time-typed-time-binary" type="xs:time" binary="true"/>
                </text>
                <text qname="dateTime">
                    <field name="dateTime"/>
                    <field name="dateTime-binary" binary="true"/>
                    <field name="dateTime-typed-dateTime" type="xs:dateTime"/>
                    <field name="dateTime-typed-dateTime-binary" type="xs:dateTime" binary="true"/>
                </text>
                <text qname="dateTimeStamp">
                    <field name="dateTimeStamp"/>
                    <field name="dateTimeStamp-binary" binary="true"/>
                    <field name="dateTimeStamp-typed-dateTimeStamp" type="xs:dateTimeStamp"/>
                    <field name="dateTimeStamp-typed-dateTimeStamp-binary" type="xs:dateTimeStamp" binary="true"/>
                </text>
                <text qname="duration">
                    <field name="duration"/>
                    <field name="duration-binary" binary="true"/>
                    <field name="duration-typed-duration" type="xs:duration"/>
                    <field name="duration-typed-duration-binary" type="xs:duration" binary="true"/>
                </text>
                <text qname="yearMonthDuration">
                    <field name="yearMonthDuration"/>
                    <field name="yearMonthDuration-binary" binary="true"/>
                    <field name="yearMonthDuration-typed-yearMonthDuration" type="xs:yearMonthDuration"/>
                    <field name="yearMonthDuration-typed-yearMonthDuration-binary" type="xs:yearMonthDuration" binary="true"/>
                </text>
                <text qname="dayTimeDuration">
                    <field name="dayTimeDuration"/>
                    <field name="dayTimeDuration-binary" binary="true"/>
                    <field name="dayTimeDuration-typed-dayTimeDuration" type="xs:dayTimeDuration"/>
                    <field name="dayTimeDuration-typed-dayTimeDuration-binary" type="xs:dayTimeDuration" binary="true"/>
                </text>
                <text qname="decimal">
                    <field name="decimal"/>
                    <field name="decimal-binary" binary="true"/>
                    <field name="decimal-typed-decimal" type="xs:decimal"/>
                    <field name="decimal-typed-decimal-binary" type="xs:decimal" binary="true"/>
                </text>
                <text qname="integer">
                    <field name="integer"/>
                    <field name="integer-binary" binary="true"/>
                    <field name="integer-typed-integer" type="xs:integer"/>
                    <field name="integer-typed-integer-binary" type="xs:integer" binary="true"/>
                </text>
                <text qname="nonPositiveInteger">
                    <field name="nonPositiveInteger"/>
                    <field name="nonPositiveInteger-binary" binary="true"/>
                    <field name="nonPositiveInteger-typed-nonPositiveInteger" type="xs:nonPositiveInteger"/>
                    <field name="nonPositiveInteger-typed-nonPositiveInteger-binary" type="xs:nonPositiveInteger" binary="true"/>
                </text>
                <text qname="negativeInteger">
                    <field name="negativeInteger"/>
                    <field name="negativeInteger-binary" binary="true"/>
                    <field name="negativeInteger-typed-negativeInteger" type="xs:negativeInteger"/>
                    <field name="negativeInteger-typed-negativeInteger-binary" type="xs:negativeInteger" binary="true"/>
                </text>
                <text qname="long">
                    <field name="long"/>
                    <field name="long-binary" binary="true"/>
                    <field name="long-typed-long" type="xs:long"/>
                    <field name="long-typed-long-binary" type="xs:long" binary="true"/>
                </text>
                <text qname="int">
                    <field name="int"/>
                    <field name="int-binary" binary="true"/>
                    <field name="int-typed-int" type="xs:int"/>
                    <field name="int-typed-int-binary" type="xs:int" binary="true"/>
                </text>
                <text qname="short">
                    <field name="short"/>
                    <field name="short-binary" binary="true"/>
                    <field name="short-typed-short" type="xs:short"/>
                    <field name="short-typed-short-binary" type="xs:short" binary="true"/>
                </text>
                <text qname="byte">
                    <field name="byte"/>
                    <field name="byte-binary" binary="true"/>
                    <field name="byte-typed-byte" type="xs:byte"/>
                    <field name="byte-typed-byte-binary" type="xs:byte" binary="true"/>
                </text>
                <text qname="nonNegativeInteger">
                    <field name="nonNegativeInteger"/>
                    <field name="nonNegativeInteger-binary" binary="true"/>
                    <field name="nonNegativeInteger-typed-nonNegativeInteger" type="xs:nonNegativeInteger"/>
                    <field name="nonNegativeInteger-typed-nonNegativeInteger-binary" type="xs:nonNegativeInteger" binary="true"/>
                </text>
                <text qname="unsignedLong">
                    <field name="unsignedLong"/>
                    <field name="unsignedLong-binary" binary="true"/>
                    <field name="unsignedLong-typed-unsignedLong" type="xs:unsignedLong"/>
                    <field name="unsignedLong-typed-unsignedLong-binary" type="xs:unsignedLong" binary="true"/>
                </text>
                <text qname="unsignedInt">
                    <field name="unsignedInt"/>
                    <field name="unsignedInt-binary" binary="true"/>
                    <field name="unsignedInt-typed-unsignedInt" type="xs:unsignedInt"/>
                    <field name="unsignedInt-typed-unsignedInt-binary" type="xs:unsignedInt" binary="true"/>
                </text>
                <text qname="unsignedShort">
                    <field name="unsignedShort"/>
                    <field name="unsignedShort-binary" binary="true"/>
                    <field name="unsignedShort-typed-unsignedShort" type="xs:unsignedShort"/>
                    <field name="unsignedShort-typed-unsignedShort-binary" type="xs:unsignedShort" binary="true"/>
                </text>
                <text qname="unsignedByte">
                    <field name="unsignedByte"/>
                    <field name="unsignedByte-binary" binary="true"/>
                    <field name="unsignedByte-typed-unsignedByte" type="xs:unsignedByte"/>
                    <field name="unsignedByte-typed-unsignedByte-binary" type="xs:unsignedByte" binary="true"/>
                </text>
                <text qname="positiveInteger">
                    <field name="positiveInteger"/>
                    <field name="positiveInteger-binary" binary="true"/>
                    <field name="positiveInteger-typed-positiveInteger" type="xs:positiveInteger"/>
                    <field name="positiveInteger-typed-positiveInteger-binary" type="xs:positiveInteger" binary="true"/>
                </text>
                <text qname="float">
                    <field name="float"/>
                    <field name="float-binary" binary="true"/>
                    <field name="float-typed-float" type="xs:float"/>
                    <field name="float-typed-float-binary" type="xs:float" binary="true"/>
                </text>
                <text qname="double">
                    <field name="double"/>
                    <field name="double-binary" binary="true"/>
                    <field name="double-typed-double" type="xs:double"/>
                    <field name="double-typed-double-binary" type="xs:double" binary="true"/>
                </text>
                <text qname="gYearMonth">
                    <field name="gYearMonth"/>
                    <field name="gYearMonth-binary" binary="true"/>
                    <field name="gYearMonth-typed-gYearMonth" type="xs:gYearMonth"/>
                    <field name="gYearMonth-typed-gYearMonth-binary" type="xs:gYearMonth" binary="true"/>
                </text>
                <text qname="gYear">
                    <field name="gYear"/>
                    <field name="gYear-binary" binary="true"/>
                    <field name="gYear-typed-gYear" type="xs:gYear"/>
                    <field name="gYear-typed-gYear-binary" type="xs:gYear" binary="true"/>
                </text>
                <text qname="gMonthDay">
                    <field name="gMonthDay"/>
                    <field name="gMonthDay-binary" binary="true"/>
                    <field name="gMonthDay-typed-gMonthDay" type="xs:gMonthDay"/>
                    <field name="gMonthDay-typed-gMonthDay-binary" type="xs:gMonthDay" binary="true"/>
                </text>
                <text qname="gMonth">
                    <field name="gMonth"/>
                    <field name="gMonth-binary" binary="true"/>
                    <field name="gMonth-typed-gMonth" type="xs:gMonth"/>
                    <field name="gMonth-typed-gMonth-binary" type="xs:gMonth" binary="true"/>
                </text>
                <text qname="gDay">
                    <field name="gDay"/>
                    <field name="gDay-binary" binary="true"/>
                    <field name="gDay-typed-gDay" type="xs:gDay"/>
                    <field name="gDay-typed-gDay-binary" type="xs:gDay" binary="true"/>
                </text>
                <text qname="boolean">
                    <field name="boolean"/>
                    <field name="boolean-binary" binary="true"/>
                    <field name="boolean-typed-boolean" type="xs:boolean"/>
                    <field name="boolean-typed-boolean-binary" type="xs:boolean" binary="true"/>
                </text>
                <text qname="base64Binary">
                    <field name="base64Binary"/>
                    <field name="base64Binary-binary" binary="true"/>
                    <field name="base64Binary-typed-base64Binary" type="xs:base64Binary"/>
                    <field name="base64Binary-typed-base64Binary-binary" type="xs:base64Binary" binary="true"/>
                </text>
                <text qname="hexBinary">
                    <field name="hexBinary"/>
                    <field name="hexBinary-binary" binary="true"/>
                    <field name="hexBinary-typed-hexBinary" type="xs:hexBinary"/>
                    <field name="hexBinary-typed-hexBinary-binary" type="xs:hexBinary" binary="true"/>
                </text>
                <text qname="anyURI">
                    <field name="anyURI"/>
                    <field name="anyURI-binary" binary="true"/>
                    <field name="anyURI-typed-anyURI" type="xs:anyURI"/>
                    <field name="anyURI-typed-anyURI-binary" type="xs:anyURI" binary="true"/>
                </text>
                <text qname="QName">
                    <field name="QName"/>
                    <field name="QName-binary" binary="true"/>
                    <field name="QName-typed-QName" type="xs:QName"/>
                    <field name="QName-typed-QName-binary" type="xs:QName" binary="true"/>
                </text>
                <text qname="NOTATION">
                    <field name="NOTATION"/>
                    <field name="NOTATION-binary" binary="true"/>
                    <field name="NOTATION-typed-NOTATION" type="xs:NOTATION"/>
                    <field name="NOTATION-typed-NOTATION-binary" type="xs:NOTATION" binary="true"/>
                </text>
                <text qname="string">
                    <field name="string"/>
                    <field name="string-binary" binary="true"/>
                    <field name="string-typed-string" type="xs:string"/>
                    <field name="string-typed-string-binary" type="xs:string" binary="true"/>
                </text>
                <text qname="normalizedString">
                    <field name="normalizedString"/>
                    <field name="normalizedString-binary" binary="true"/>
                    <field name="normalizedString-typed-normalizedString" type="xs:normalizedString"/>
                    <field name="normalizedString-typed-normalizedString-binary" type="xs:normalizedString" binary="true"/>
                </text>
                <text qname="token">
                    <field name="token"/>
                    <field name="token-binary" binary="true"/>
                    <field name="token-typed-token" type="xs:token"/>
                    <field name="token-typed-token-binary" type="xs:token" binary="true"/>
                </text>
                <text qname="language">
                    <field name="language"/>
                    <field name="language-binary" binary="true"/>
                    <field name="language-typed-language" type="xs:language"/>
                    <field name="language-typed-language-binary" type="xs:language" binary="true"/>
                </text>
                <text qname="NMTOKEN">
                    <field name="NMTOKEN"/>
                    <field name="NMTOKEN-binary" binary="true"/>
                    <field name="NMTOKEN-typed-NMTOKEN" type="xs:NMTOKEN"/>
                    <field name="NMTOKEN-typed-NMTOKEN-binary" type="xs:NMTOKEN" binary="true"/>
                </text>
                <text qname="Name">
                    <field name="Name"/>
                    <field name="Name-binary" binary="true"/>
                    <field name="Name-typed-Name" type="xs:Name"/>
                    <field name="Name-typed-Name-binary" type="xs:Name" binary="true"/>
                </text>
                <text qname="NCName">
                    <field name="NCName"/>
                    <field name="NCName-binary" binary="true"/>
                    <field name="NCName-typed-NCName" type="xs:NCName"/>
                    <field name="NCName-typed-NCName-binary" type="xs:NCName" binary="true"/>
                </text>
                <text qname="ID">
                    <field name="ID"/>
                    <field name="ID-binary" binary="true"/>
                    <field name="ID-typed-ID" type="xs:ID"/>
                    <field name="ID-typed-ID-binary" type="xs:ID" binary="true"/>
                </text>
                <text qname="IDREF">
                    <field name="IDREF"/>
                    <field name="IDREF-binary" binary="true"/>
                    <field name="IDREF-typed-IDREF" type="xs:IDREF"/>
                    <field name="IDREF-typed-IDREF-binary" type="xs:IDREF" binary="true"/>
                </text>
                <text qname="ENTITY">
                    <field name="ENTITY"/>
                    <field name="ENTITY-binary" binary="true"/>
                    <field name="ENTITY-typed-ENTITY" type="xs:ENTITY"/>
                    <field name="ENTITY-typed-ENTITY-binary" type="xs:ENTITY" binary="true"/>
                </text>
            </lucene>
        </index>
    </collection>
};

declare variable $fields:TEST_COLLECTION_NAME := "ft-field-test";
declare variable $fields:TEST_COLLECTION_PATH := "/db/" || $fields:TEST_COLLECTION_NAME;

declare variable $fields:TYPES_DOC_NAME := "types.xml";
declare variable $fields:TYPES_DOC_PATH := $fields:TEST_COLLECTION_PATH || "/" || $fields:TYPES_DOC_NAME;

declare variable $fields:TYPES_DOC := document {
    <types xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <date description="Invalid lexical format">20250706</date>
        <date>2025-07-06-04:55</date>
        <time description="Invalid lexical format">08.50.49.831-04:55</time>
        <time>08:50:49.831-04:55</time>
        <dateTime description="Invalid lexical format">20250706085058265</dateTime>
        <dateTime>2025-07-06T08:50:58.265-04:55</dateTime>
        <dateTimeStamp description="Invalid lexical format">2025-07-06T08:51:11.932</dateTimeStamp>
        <dateTimeStamp>2025-07-06T08:51:11.932-04:55</dateTimeStamp>
        <duration description="Invalid lexical format">P15WT7H</duration>
        <duration>P2025Y07M15DT7H00M00.000S</duration>
        <yearMonthDuration description="Invalid lexical format">PT7H00M00S</yearMonthDuration>
        <yearMonthDuration>P2025Y07M</yearMonthDuration>
        <dayTimeDuration description="Invalid lexical format">P2025Y</dayTimeDuration>
        <dayTimeDuration>P15DT7H000.000S</dayTimeDuration>
        <decimal description="Invalid lexical format">1.23E2</decimal>
        <decimal description="OutOfBounds lt DOUBLE.MIN_VALUE">-19223372036854775808.23</decimal>
        <decimal description="OutOfBounds gt DOUBLE.MAX_VALUE">199769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.000000</decimal>
        <decimal>1.23</decimal>
        <integer description="OutOfBounds lt Long.MIN_VALUE">-9223372036854775809</integer>
        <integer description="OutOfBounds gt Long.MAX_VALUE">9223372036854775808</integer>
        <integer>9876543210</integer>
        <nonPositiveInteger description="OutOfBounds">01</nonPositiveInteger>
        <nonPositiveInteger>0</nonPositiveInteger>
        <nonPositiveInteger>-0</nonPositiveInteger>
        <nonPositiveInteger>-0223372036854775808</nonPositiveInteger>
        <negativeInteger description="OutOfBounds">01</negativeInteger>
        <negativeInteger description="OutOfBounds">0</negativeInteger>
        <negativeInteger description="OutOfBounds">-0</negativeInteger>
        <negativeInteger>-0223372036854775808</negativeInteger>
        <long description="Invalid lexical format">9.876</long>
        <long description="OutOfBounds lt Long.MIN_VALUE">-9223372036854775809</long>
        <long description="OutOfBounds gt Long.MAX_VALUE">9223372036854775808</long>
        <long>9876543210</long>
        <int description="Invalid lexical format">9.876</int>
        <int description="OutOfBounds lt Integer.MIN_VALUE">-92147483648</int>
        <int description="OutOfBounds gt Integer.MAX_VALUE">92147483647</int>
        <int>-98765</int>
        <int>98765</int>
        <short description="Invalid lexical format">9.876</short>
        <short description="OutOfBounds lt Integer.MIN_VALUE">-932768</short>
        <short description="OutOfBounds gt Integer.MAX_VALUE">932767</short>
        <short>-9767</short>
        <short>9767</short>
        <byte description="Invalid lexical format">9.876</byte>
        <byte description="OutOfBounds lt Byte.MIN_VALUE">-987</byte>
        <byte description="OutOfBounds gt Byte.MAX_VALUE">987</byte>
        <byte>-99</byte>
        <byte>99</byte>
        <nonNegativeInteger description="OutOfBounds">-01</nonNegativeInteger>
        <nonNegativeInteger>-0</nonNegativeInteger>
        <nonNegativeInteger>0</nonNegativeInteger>
        <nonNegativeInteger>0223372036854775808</nonNegativeInteger>
        <unsignedLong description="Invalid lexical format">9.876</unsignedLong>
        <unsignedLong description="OutOfBounds">-9</unsignedLong>
        <unsignedLong description="OutOfBounds">918446744073709551616</unsignedLong>
        <unsignedLong>9876543210</unsignedLong>
        <unsignedInt description="Invalid lexical format">9.876</unsignedInt>
        <unsignedInt description="OutOfBounds">-9</unsignedInt>
        <unsignedInt description="OutOfBounds">94294967296</unsignedInt>
        <unsignedInt>98765</unsignedInt>
        <unsignedShort description="Invalid lexical format">9.876</unsignedShort>
        <unsignedShort description="OutOfBounds">-9</unsignedShort>
        <unsignedShort description="OutOfBounds">965536</unsignedShort>
        <unsignedShort>9536</unsignedShort>
        <unsignedByte description="Invalid lexical format">9.876</unsignedByte>
        <unsignedByte description="OutOfBounds">-9</unsignedByte>
        <unsignedByte description="OutOfBounds">9255</unsignedByte>
        <unsignedByte>9</unsignedByte>
        <positiveInteger description="OutOfBounds">-01</positiveInteger>
        <positiveInteger description="OutOfBounds">-0</positiveInteger>
        <positiveInteger description="OutOfBounds">0</positiveInteger>
        <positiveInteger>0223372036854775808</positiveInteger>
        <float description="Invalid lexical format">9.2EE1</float>
        <float description="-INF">-9.4028235E38</float>
        <float description="+INF">9.4028235E38</float>
        <float>9.2E1</float>
        <double description="Invalid lexical format">9.2EE1</double>
        <double description="-INF">-9.7976931348623157E308</double>
        <double description="+INF">9.7976931348623157E308</double>
        <double>9.2E1</double>
        <gYearMonth description="Invalid lexical format">1999-00</gYearMonth>
        <gYearMonth>1978-09-05:00</gYearMonth>
        <gYear description="Invalid lexical format">--1978</gYear>
        <gYear>1978</gYear>
        <gMonthDay description="Invalid lexical format">09-12</gMonthDay>
        <gMonthDay>--09-12</gMonthDay>
        <gMonth description="Invalid lexical format">09</gMonth>
        <gMonth>--09</gMonth>
        <gDay description="Invalid lexical format">12</gDay>
        <gDay>---12</gDay>
        <boolean description="Invalid lexical format">tru</boolean>
        <boolean>true</boolean>
        <base64Binary description="Invalid lexical format">dmFsaWQ</base64Binary>
        <base64Binary>dmFsaWQ=</base64Binary>
        <hexBinary description="Invalid lexical format">76616C696</hexBinary>
        <hexBinary>76616C6964</hexBinary>
        <anyURI description="Invalid lexical format">http://evolvedbinary.com#f1#f2</anyURI>
        <anyURI>http://evolvedbinary.com</anyURI>
        <QName description="Invalid lexical format">xs:in:valid</QName>
        <QName>xs:valid</QName>
        <NOTATION description="Invalid lexical format">xs:in:valid</NOTATION>
        <NOTATION>xs:valid</NOTATION>
        <string>AB C</string>
        <string>ABC</string>
        <normalizedString>ABC	A</normalizedString>
        <normalizedString>ABC</normalizedString>
        <token>AB   C</token>
        <token>ABC</token>
        <language description="Invalid lexical format">englishing</language>
        <language>en-GB</language>
        <NMTOKEN description="Invalid lexical format">%ABC</NMTOKEN>
        <NMTOKEN>ABC</NMTOKEN>
        <Name description="Invalid lexical format">-ABC</Name>
        <Name>ABC</Name>
        <NCName description="Invalid lexical format">A:BC</NCName>
        <NCName>ABC</NCName>
        <ID description="Invalid lexical format">A:BC</ID>
        <ID>ABC</ID>
        <IDREF description="Invalid lexical format">A:BC</IDREF>
        <IDREF>ABC</IDREF>
        <ENTITY description="Invalid lexical format">A:BC</ENTITY>
        <ENTITY>ABC</ENTITY>
    </types>
};

declare
    %test:setUp
function fields:setup() {
  let $conf-collection := xmldb:create-collection("/db/system/config/db", $fields:TEST_COLLECTION_NAME)
  return
    let $_ := xmldb:store($conf-collection, "collection.xconf", $fields:COLLECTION_CONF)
    return
      let $test-collection := xmldb:create-collection("/db", $fields:TEST_COLLECTION_NAME)
      return
        xmldb:store($test-collection, $fields:TYPES_DOC_NAME, $fields:TYPES_DOC)
};

declare
    %test:tearDown
function fields:cleanup() {
  xmldb:remove($fields:TEST_COLLECTION_PATH),
  xmldb:remove("/db/system/config" || $fields:TEST_COLLECTION_PATH)
};

declare
    %private
function fields:assert-field-types($search as xs:string, $type-name as xs:string, $type-check as function(item()*) as xs:boolean) as element()+ {
    for $element in doc($fields:TYPES_DOC_PATH)/types/element()[local-name() eq $type-name][ft:query(., $search)]
    return
        (
            <field name="{$type-name}" instance-of="xs:string" node-id="{util:node-id($element)}">{ft:field($element, $type-name) instance of xs:string}</field>,
            <field name="{$type-name}" instance-of="xs:string" node-id="{util:node-id($element)}">{ft:field($element, $type-name, "xs:string") instance of xs:string}</field>,
            <field name="{$type-name || "-typed-" || $type-name}" instance-of="xs:{$type-name}" node-id="{util:node-id($element)}">{$type-check(ft:field($element, $type-name || "-typed-" || $type-name, "xs:" || $type-name))}</field>,
            <binary-field name="{$type-name}" instance-of="xs:string" node-id="{util:node-id($element)}">{ft:binary-field($element, $type-name || "-binary") instance of xs:string}</binary-field>,
            <binary-field name="{$type-name}" instance-of="xs:string" node-id="{util:node-id($element)}">{ft:binary-field($element, $type-name || "-binary", "xs:string") instance of xs:string}</binary-field>,
            <binary-field name="{$type-name || "-typed-" || $type-name || "-binary"}" instance-of="xs:{$type-name}" node-id="{util:node-id($element)}">{$type-check(ft:binary-field($element, $type-name || "-typed-" || $type-name || "-binary", "xs:" || $type-name))}</binary-field>
        )
};

declare
    %test:assertEquals(
        '<field name="date" instance-of="xs:string" node-id="1.1">true</field>',
        '<field name="date" instance-of="xs:string" node-id="1.1">true</field>',
        '<field name="date-typed-date" instance-of="xs:date" node-id="1.1">false</field>',
        '<binary-field name="date" instance-of="xs:string" node-id="1.1">true</binary-field>',
        '<binary-field name="date" instance-of="xs:string" node-id="1.1">true</binary-field>',
        '<binary-field name="date-typed-date-binary" instance-of="xs:date" node-id="1.1">false</binary-field>',

        '<field name="date" instance-of="xs:string" node-id="1.2">true</field>',
        '<field name="date" instance-of="xs:string" node-id="1.2">true</field>',
        '<field name="date-typed-date" instance-of="xs:date" node-id="1.2">true</field>',
        '<binary-field name="date" instance-of="xs:string" node-id="1.2">true</binary-field>',
        '<binary-field name="date" instance-of="xs:string" node-id="1.2">true</binary-field>',
        '<binary-field name="date-typed-date-binary" instance-of="xs:date" node-id="1.2">true</binary-field>'
    )
function fields:date-type-fields() {
    fields:assert-field-types("2025*", "date", function($field-value) { $field-value instance of xs:date })
};

declare
    %test:assertEquals(
        '<field name="time" instance-of="xs:string" node-id="1.3">true</field>',
        '<field name="time" instance-of="xs:string" node-id="1.3">true</field>',
        '<field name="time-typed-time" instance-of="xs:time" node-id="1.3">false</field>',
        '<binary-field name="time" instance-of="xs:string" node-id="1.3">true</binary-field>',
        '<binary-field name="time" instance-of="xs:string" node-id="1.3">true</binary-field>',
        '<binary-field name="time-typed-time-binary" instance-of="xs:time" node-id="1.3">false</binary-field>',

        '<field name="time" instance-of="xs:string" node-id="1.4">true</field>',
        '<field name="time" instance-of="xs:string" node-id="1.4">true</field>',
        '<field name="time-typed-time" instance-of="xs:time" node-id="1.4">true</field>',
        '<binary-field name="time" instance-of="xs:string" node-id="1.4">true</binary-field>',
        '<binary-field name="time" instance-of="xs:string" node-id="1.4">true</binary-field>',
        '<binary-field name="time-typed-time-binary" instance-of="xs:time" node-id="1.4">true</binary-field>'
    )
function fields:time-type-fields() {
    fields:assert-field-types("08*", "time", function($field-value) { $field-value instance of xs:time })
};

declare
    %test:assertEquals(
        '<field name="dateTime" instance-of="xs:string" node-id="1.5">true</field>',
        '<field name="dateTime" instance-of="xs:string" node-id="1.5">true</field>',
        '<field name="dateTime-typed-dateTime" instance-of="xs:dateTime" node-id="1.5">false</field>',
        '<binary-field name="dateTime" instance-of="xs:string" node-id="1.5">true</binary-field>',
        '<binary-field name="dateTime" instance-of="xs:string" node-id="1.5">true</binary-field>',
        '<binary-field name="dateTime-typed-dateTime-binary" instance-of="xs:dateTime" node-id="1.5">false</binary-field>',

        '<field name="dateTime" instance-of="xs:string" node-id="1.6">true</field>',
        '<field name="dateTime" instance-of="xs:string" node-id="1.6">true</field>',
        '<field name="dateTime-typed-dateTime" instance-of="xs:dateTime" node-id="1.6">true</field>',
        '<binary-field name="dateTime" instance-of="xs:string" node-id="1.6">true</binary-field>',
        '<binary-field name="dateTime" instance-of="xs:string" node-id="1.6">true</binary-field>',
        '<binary-field name="dateTime-typed-dateTime-binary" instance-of="xs:dateTime" node-id="1.6">true</binary-field>'
    )
function fields:dateTime-type-fields() {
    fields:assert-field-types("2025*", "dateTime", function($field-value) { $field-value instance of xs:dateTime })
};

declare
    %test:assertEquals(
        '<field name="dateTimeStamp" instance-of="xs:string" node-id="1.7">true</field>',
        '<field name="dateTimeStamp" instance-of="xs:string" node-id="1.7">true</field>',
        '<field name="dateTimeStamp-typed-dateTimeStamp" instance-of="xs:dateTimeStamp" node-id="1.7">false</field>',
        '<binary-field name="dateTimeStamp" instance-of="xs:string" node-id="1.7">true</binary-field>',
        '<binary-field name="dateTimeStamp" instance-of="xs:string" node-id="1.7">true</binary-field>',
        '<binary-field name="dateTimeStamp-typed-dateTimeStamp-binary" instance-of="xs:dateTimeStamp" node-id="1.7">false</binary-field>',

        '<field name="dateTimeStamp" instance-of="xs:string" node-id="1.8">true</field>',
        '<field name="dateTimeStamp" instance-of="xs:string" node-id="1.8">true</field>',
        '<field name="dateTimeStamp-typed-dateTimeStamp" instance-of="xs:dateTimeStamp" node-id="1.8">true</field>',
        '<binary-field name="dateTimeStamp" instance-of="xs:string" node-id="1.8">true</binary-field>',
        '<binary-field name="dateTimeStamp" instance-of="xs:string" node-id="1.8">true</binary-field>',
        '<binary-field name="dateTimeStamp-typed-dateTimeStamp-binary" instance-of="xs:dateTimeStamp" node-id="1.8">true</binary-field>'
    )
function fields:dateTimeStamp-type-fields() {
    fields:assert-field-types("2025*", "dateTimeStamp", function($field-value) { $field-value instance of xs:dateTimeStamp })
};

declare
    %test:assertEquals(
        '<field name="duration" instance-of="xs:string" node-id="1.9">true</field>',
        '<field name="duration" instance-of="xs:string" node-id="1.9">true</field>',
        '<field name="duration-typed-duration" instance-of="xs:duration" node-id="1.9">false</field>',
        '<binary-field name="duration" instance-of="xs:string" node-id="1.9">true</binary-field>',
        '<binary-field name="duration" instance-of="xs:string" node-id="1.9">true</binary-field>',
        '<binary-field name="duration-typed-duration-binary" instance-of="xs:duration" node-id="1.9">false</binary-field>',

        '<field name="duration" instance-of="xs:string" node-id="1.10">true</field>',
        '<field name="duration" instance-of="xs:string" node-id="1.10">true</field>',
        '<field name="duration-typed-duration" instance-of="xs:duration" node-id="1.10">true</field>',
        '<binary-field name="duration" instance-of="xs:string" node-id="1.10">true</binary-field>',
        '<binary-field name="duration" instance-of="xs:string" node-id="1.10">true</binary-field>',
        '<binary-field name="duration-typed-duration-binary" instance-of="xs:duration" node-id="1.10">true</binary-field>'
    )
function fields:duration-type-fields() {
    fields:assert-field-types("P*", "duration", function($field-value) { $field-value instance of xs:duration })
};

declare
    %test:assertEquals(
        '<field name="yearMonthDuration" instance-of="xs:string" node-id="1.11">true</field>',
        '<field name="yearMonthDuration" instance-of="xs:string" node-id="1.11">true</field>',
        '<field name="yearMonthDuration-typed-yearMonthDuration" instance-of="xs:yearMonthDuration" node-id="1.11">false</field>',
        '<binary-field name="yearMonthDuration" instance-of="xs:string" node-id="1.11">true</binary-field>',
        '<binary-field name="yearMonthDuration" instance-of="xs:string" node-id="1.11">true</binary-field>',
        '<binary-field name="yearMonthDuration-typed-yearMonthDuration-binary" instance-of="xs:yearMonthDuration" node-id="1.11">false</binary-field>',

        '<field name="yearMonthDuration" instance-of="xs:string" node-id="1.12">true</field>',
        '<field name="yearMonthDuration" instance-of="xs:string" node-id="1.12">true</field>',
        '<field name="yearMonthDuration-typed-yearMonthDuration" instance-of="xs:yearMonthDuration" node-id="1.12">true</field>',
        '<binary-field name="yearMonthDuration" instance-of="xs:string" node-id="1.12">true</binary-field>',
        '<binary-field name="yearMonthDuration" instance-of="xs:string" node-id="1.12">true</binary-field>',
        '<binary-field name="yearMonthDuration-typed-yearMonthDuration-binary" instance-of="xs:yearMonthDuration" node-id="1.12">true</binary-field>'
    )
function fields:yearMonthDuration-type-fields() {
    fields:assert-field-types("P*", "yearMonthDuration", function($field-value) { $field-value instance of xs:yearMonthDuration })
};

declare
    %test:assertEquals(
        '<field name="dayTimeDuration" instance-of="xs:string" node-id="1.13">true</field>',
        '<field name="dayTimeDuration" instance-of="xs:string" node-id="1.13">true</field>',
        '<field name="dayTimeDuration-typed-dayTimeDuration" instance-of="xs:dayTimeDuration" node-id="1.13">false</field>',
        '<binary-field name="dayTimeDuration" instance-of="xs:string" node-id="1.13">true</binary-field>',
        '<binary-field name="dayTimeDuration" instance-of="xs:string" node-id="1.13">true</binary-field>',
        '<binary-field name="dayTimeDuration-typed-dayTimeDuration-binary" instance-of="xs:dayTimeDuration" node-id="1.13">false</binary-field>',

        '<field name="dayTimeDuration" instance-of="xs:string" node-id="1.14">true</field>',
        '<field name="dayTimeDuration" instance-of="xs:string" node-id="1.14">true</field>',
        '<field name="dayTimeDuration-typed-dayTimeDuration" instance-of="xs:dayTimeDuration" node-id="1.14">true</field>',
        '<binary-field name="dayTimeDuration" instance-of="xs:string" node-id="1.14">true</binary-field>',
        '<binary-field name="dayTimeDuration" instance-of="xs:string" node-id="1.14">true</binary-field>',
        '<binary-field name="dayTimeDuration-typed-dayTimeDuration-binary" instance-of="xs:dayTimeDuration" node-id="1.14">true</binary-field>'
    )
function fields:dayTimeDuration-type-fields() {
    fields:assert-field-types("P*", "dayTimeDuration", function($field-value) { $field-value instance of xs:dayTimeDuration })
};

declare
    %test:assertEquals(
        '<field name="decimal" instance-of="xs:string" node-id="1.15">true</field>',
        '<field name="decimal" instance-of="xs:string" node-id="1.15">true</field>',
        '<field name="decimal-typed-decimal" instance-of="xs:decimal" node-id="1.15">false</field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.15">true</binary-field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.15">true</binary-field>',
        '<binary-field name="decimal-typed-decimal-binary" instance-of="xs:decimal" node-id="1.15">false</binary-field>',

        '<field name="decimal" instance-of="xs:string" node-id="1.16">true</field>',
        '<field name="decimal" instance-of="xs:string" node-id="1.16">true</field>',
        '<field name="decimal-typed-decimal" instance-of="xs:decimal" node-id="1.16">false</field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.16">true</binary-field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.16">true</binary-field>',
        '<binary-field name="decimal-typed-decimal-binary" instance-of="xs:decimal" node-id="1.16">true</binary-field>',

        '<field name="decimal" instance-of="xs:string" node-id="1.17">true</field>',
        '<field name="decimal" instance-of="xs:string" node-id="1.17">true</field>',
        '<field name="decimal-typed-decimal" instance-of="xs:decimal" node-id="1.17">false</field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.17">true</binary-field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.17">true</binary-field>',
        '<binary-field name="decimal-typed-decimal-binary" instance-of="xs:decimal" node-id="1.17">true</binary-field>',

        '<field name="decimal" instance-of="xs:string" node-id="1.18">true</field>',
        '<field name="decimal" instance-of="xs:string" node-id="1.18">true</field>',
        '<field name="decimal-typed-decimal" instance-of="xs:decimal" node-id="1.18">true</field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.18">true</binary-field>',
        '<binary-field name="decimal" instance-of="xs:string" node-id="1.18">true</binary-field>',
        '<binary-field name="decimal-typed-decimal-binary" instance-of="xs:decimal" node-id="1.18">true</binary-field>'
    )
function fields:decimal-type-fields() {
    fields:assert-field-types("1*", "decimal", function($field-value) { $field-value instance of xs:decimal })
};

declare
    %test:assertEquals(
        '<field name="integer" instance-of="xs:string" node-id="1.19">true</field>',
        '<field name="integer" instance-of="xs:string" node-id="1.19">true</field>',
        '<field name="integer-typed-integer" instance-of="xs:integer" node-id="1.19">false</field>',
        '<binary-field name="integer" instance-of="xs:string" node-id="1.19">true</binary-field>',
        '<binary-field name="integer" instance-of="xs:string" node-id="1.19">true</binary-field>',
        '<binary-field name="integer-typed-integer-binary" instance-of="xs:integer" node-id="1.19">true</binary-field>',

        '<field name="integer" instance-of="xs:string" node-id="1.20">true</field>',
        '<field name="integer" instance-of="xs:string" node-id="1.20">true</field>',
        '<field name="integer-typed-integer" instance-of="xs:integer" node-id="1.20">false</field>',
        '<binary-field name="integer" instance-of="xs:string" node-id="1.20">true</binary-field>',
        '<binary-field name="integer" instance-of="xs:string" node-id="1.20">true</binary-field>',
        '<binary-field name="integer-typed-integer-binary" instance-of="xs:integer" node-id="1.20">true</binary-field>',

        '<field name="integer" instance-of="xs:string" node-id="1.21">true</field>',
        '<field name="integer" instance-of="xs:string" node-id="1.21">true</field>',
        '<field name="integer-typed-integer" instance-of="xs:integer" node-id="1.21">true</field>',
        '<binary-field name="integer" instance-of="xs:string" node-id="1.21">true</binary-field>',
        '<binary-field name="integer" instance-of="xs:string" node-id="1.21">true</binary-field>',
        '<binary-field name="integer-typed-integer-binary" instance-of="xs:integer" node-id="1.21">true</binary-field>'
    )
function fields:integer-type-fields() {
    fields:assert-field-types("9*", "integer", function($field-value) { $field-value instance of xs:integer })
};

declare
    %test:assertEquals(
        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.22">true</field>',
        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.22">true</field>',
        '<field name="nonPositiveInteger-typed-nonPositiveInteger" instance-of="xs:nonPositiveInteger" node-id="1.22">false</field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.22">true</binary-field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.22">true</binary-field>',
        '<binary-field name="nonPositiveInteger-typed-nonPositiveInteger-binary" instance-of="xs:nonPositiveInteger" node-id="1.22">false</binary-field>',

        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.23">true</field>',
        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.23">true</field>',
        '<field name="nonPositiveInteger-typed-nonPositiveInteger" instance-of="xs:nonPositiveInteger" node-id="1.23">true</field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.23">true</binary-field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.23">true</binary-field>',
        '<binary-field name="nonPositiveInteger-typed-nonPositiveInteger-binary" instance-of="xs:nonPositiveInteger" node-id="1.23">true</binary-field>',

        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.24">true</field>',
        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.24">true</field>',
        '<field name="nonPositiveInteger-typed-nonPositiveInteger" instance-of="xs:nonPositiveInteger" node-id="1.24">true</field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.24">true</binary-field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.24">true</binary-field>',
        '<binary-field name="nonPositiveInteger-typed-nonPositiveInteger-binary" instance-of="xs:nonPositiveInteger" node-id="1.24">true</binary-field>',

        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.25">true</field>',
        '<field name="nonPositiveInteger" instance-of="xs:string" node-id="1.25">true</field>',
        '<field name="nonPositiveInteger-typed-nonPositiveInteger" instance-of="xs:nonPositiveInteger" node-id="1.25">true</field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.25">true</binary-field>',
        '<binary-field name="nonPositiveInteger" instance-of="xs:string" node-id="1.25">true</binary-field>',
        '<binary-field name="nonPositiveInteger-typed-nonPositiveInteger-binary" instance-of="xs:nonPositiveInteger" node-id="1.25">true</binary-field>'
    )
function fields:nonPositiveInteger-type-fields() {
    fields:assert-field-types("0*", "nonPositiveInteger", function($field-value) { $field-value instance of xs:nonPositiveInteger })
};

declare
    %test:assertEquals(
        '<field name="negativeInteger" instance-of="xs:string" node-id="1.26">true</field>',
        '<field name="negativeInteger" instance-of="xs:string" node-id="1.26">true</field>',
        '<field name="negativeInteger-typed-negativeInteger" instance-of="xs:negativeInteger" node-id="1.26">false</field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.26">true</binary-field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.26">true</binary-field>',
        '<binary-field name="negativeInteger-typed-negativeInteger-binary" instance-of="xs:negativeInteger" node-id="1.26">false</binary-field>',

        '<field name="negativeInteger" instance-of="xs:string" node-id="1.27">true</field>',
        '<field name="negativeInteger" instance-of="xs:string" node-id="1.27">true</field>',
        '<field name="negativeInteger-typed-negativeInteger" instance-of="xs:negativeInteger" node-id="1.27">false</field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.27">true</binary-field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.27">true</binary-field>',
        '<binary-field name="negativeInteger-typed-negativeInteger-binary" instance-of="xs:negativeInteger" node-id="1.27">false</binary-field>',

        '<field name="negativeInteger" instance-of="xs:string" node-id="1.28">true</field>',
        '<field name="negativeInteger" instance-of="xs:string" node-id="1.28">true</field>',
        '<field name="negativeInteger-typed-negativeInteger" instance-of="xs:negativeInteger" node-id="1.28">false</field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.28">true</binary-field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.28">true</binary-field>',
        '<binary-field name="negativeInteger-typed-negativeInteger-binary" instance-of="xs:negativeInteger" node-id="1.28">false</binary-field>',

        '<field name="negativeInteger" instance-of="xs:string" node-id="1.29">true</field>',
        '<field name="negativeInteger" instance-of="xs:string" node-id="1.29">true</field>',
        '<field name="negativeInteger-typed-negativeInteger" instance-of="xs:negativeInteger" node-id="1.29">true</field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.29">true</binary-field>',
        '<binary-field name="negativeInteger" instance-of="xs:string" node-id="1.29">true</binary-field>',
        '<binary-field name="negativeInteger-typed-negativeInteger-binary" instance-of="xs:negativeInteger" node-id="1.29">true</binary-field>'
    )
function fields:negativeInteger-type-fields() {
    fields:assert-field-types("0*", "negativeInteger", function($field-value) { $field-value instance of xs:negativeInteger })
};

declare
    %test:assertEquals(
        '<field name="long" instance-of="xs:string" node-id="1.30">true</field>',
        '<field name="long" instance-of="xs:string" node-id="1.30">true</field>',
        '<field name="long-typed-long" instance-of="xs:long" node-id="1.30">false</field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.30">true</binary-field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.30">true</binary-field>',
        '<binary-field name="long-typed-long-binary" instance-of="xs:long" node-id="1.30">false</binary-field>',

        '<field name="long" instance-of="xs:string" node-id="1.31">true</field>',
        '<field name="long" instance-of="xs:string" node-id="1.31">true</field>',
        '<field name="long-typed-long" instance-of="xs:long" node-id="1.31">false</field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.31">true</binary-field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.31">true</binary-field>',
        '<binary-field name="long-typed-long-binary" instance-of="xs:long" node-id="1.31">false</binary-field>',

        '<field name="long" instance-of="xs:string" node-id="1.32">true</field>',
        '<field name="long" instance-of="xs:string" node-id="1.32">true</field>',
        '<field name="long-typed-long" instance-of="xs:long" node-id="1.32">false</field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.32">true</binary-field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.32">true</binary-field>',
        '<binary-field name="long-typed-long-binary" instance-of="xs:long" node-id="1.32">false</binary-field>',

        '<field name="long" instance-of="xs:string" node-id="1.33">true</field>',
        '<field name="long" instance-of="xs:string" node-id="1.33">true</field>',
        '<field name="long-typed-long" instance-of="xs:long" node-id="1.33">true</field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.33">true</binary-field>',
        '<binary-field name="long" instance-of="xs:string" node-id="1.33">true</binary-field>',
        '<binary-field name="long-typed-long-binary" instance-of="xs:long" node-id="1.33">true</binary-field>'
    )
function fields:long-type-fields() {
    fields:assert-field-types("9*", "long", function($field-value) { $field-value instance of xs:long })
};

declare
    %test:assertEquals(
        '<field name="int" instance-of="xs:string" node-id="1.34">true</field>',
        '<field name="int" instance-of="xs:string" node-id="1.34">true</field>',
        '<field name="int-typed-int" instance-of="xs:int" node-id="1.34">false</field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.34">true</binary-field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.34">true</binary-field>',
        '<binary-field name="int-typed-int-binary" instance-of="xs:int" node-id="1.34">false</binary-field>',

        '<field name="int" instance-of="xs:string" node-id="1.35">true</field>',
        '<field name="int" instance-of="xs:string" node-id="1.35">true</field>',
        '<field name="int-typed-int" instance-of="xs:int" node-id="1.35">false</field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.35">true</binary-field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.35">true</binary-field>',
        '<binary-field name="int-typed-int-binary" instance-of="xs:int" node-id="1.35">false</binary-field>',

        '<field name="int" instance-of="xs:string" node-id="1.36">true</field>',
        '<field name="int" instance-of="xs:string" node-id="1.36">true</field>',
        '<field name="int-typed-int" instance-of="xs:int" node-id="1.36">false</field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.36">true</binary-field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.36">true</binary-field>',
        '<binary-field name="int-typed-int-binary" instance-of="xs:int" node-id="1.36">false</binary-field>',

        '<field name="int" instance-of="xs:string" node-id="1.37">true</field>',
        '<field name="int" instance-of="xs:string" node-id="1.37">true</field>',
        '<field name="int-typed-int" instance-of="xs:int" node-id="1.37">true</field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.37">true</binary-field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.37">true</binary-field>',
        '<binary-field name="int-typed-int-binary" instance-of="xs:int" node-id="1.37">true</binary-field>',

        '<field name="int" instance-of="xs:string" node-id="1.38">true</field>',
        '<field name="int" instance-of="xs:string" node-id="1.38">true</field>',
        '<field name="int-typed-int" instance-of="xs:int" node-id="1.38">true</field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.38">true</binary-field>',
        '<binary-field name="int" instance-of="xs:string" node-id="1.38">true</binary-field>',
        '<binary-field name="int-typed-int-binary" instance-of="xs:int" node-id="1.38">true</binary-field>'
    )
function fields:int-type-fields() {
    fields:assert-field-types("9*", "int", function($field-value) { $field-value instance of xs:int })
};

declare
    %test:assertEquals(
        '<field name="short" instance-of="xs:string" node-id="1.39">true</field>',
        '<field name="short" instance-of="xs:string" node-id="1.39">true</field>',
        '<field name="short-typed-short" instance-of="xs:short" node-id="1.39">false</field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.39">true</binary-field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.39">true</binary-field>',
        '<binary-field name="short-typed-short-binary" instance-of="xs:short" node-id="1.39">false</binary-field>',

        '<field name="short" instance-of="xs:string" node-id="1.40">true</field>',
        '<field name="short" instance-of="xs:string" node-id="1.40">true</field>',
        '<field name="short-typed-short" instance-of="xs:short" node-id="1.40">false</field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.40">true</binary-field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.40">true</binary-field>',
        '<binary-field name="short-typed-short-binary" instance-of="xs:short" node-id="1.40">false</binary-field>',

        '<field name="short" instance-of="xs:string" node-id="1.41">true</field>',
        '<field name="short" instance-of="xs:string" node-id="1.41">true</field>',
        '<field name="short-typed-short" instance-of="xs:short" node-id="1.41">false</field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.41">true</binary-field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.41">true</binary-field>',
        '<binary-field name="short-typed-short-binary" instance-of="xs:short" node-id="1.41">false</binary-field>',

        '<field name="short" instance-of="xs:string" node-id="1.42">true</field>',
        '<field name="short" instance-of="xs:string" node-id="1.42">true</field>',
        '<field name="short-typed-short" instance-of="xs:short" node-id="1.42">true</field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.42">true</binary-field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.42">true</binary-field>',
        '<binary-field name="short-typed-short-binary" instance-of="xs:short" node-id="1.42">true</binary-field>',

        '<field name="short" instance-of="xs:string" node-id="1.43">true</field>',
        '<field name="short" instance-of="xs:string" node-id="1.43">true</field>',
        '<field name="short-typed-short" instance-of="xs:short" node-id="1.43">true</field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.43">true</binary-field>',
        '<binary-field name="short" instance-of="xs:string" node-id="1.43">true</binary-field>',
        '<binary-field name="short-typed-short-binary" instance-of="xs:short" node-id="1.43">true</binary-field>'
    )
function fields:short-type-fields() {
    fields:assert-field-types("9*", "short", function($field-value) { $field-value instance of xs:short })
};

declare
    %test:assertEquals(
        '<field name="byte" instance-of="xs:string" node-id="1.44">true</field>',
        '<field name="byte" instance-of="xs:string" node-id="1.44">true</field>',
        '<field name="byte-typed-byte" instance-of="xs:byte" node-id="1.44">false</field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.44">true</binary-field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.44">true</binary-field>',
        '<binary-field name="byte-typed-byte-binary" instance-of="xs:byte" node-id="1.44">false</binary-field>',

        '<field name="byte" instance-of="xs:string" node-id="1.45">true</field>',
        '<field name="byte" instance-of="xs:string" node-id="1.45">true</field>',
        '<field name="byte-typed-byte" instance-of="xs:byte" node-id="1.45">false</field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.45">true</binary-field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.45">true</binary-field>',
        '<binary-field name="byte-typed-byte-binary" instance-of="xs:byte" node-id="1.45">false</binary-field>',

        '<field name="byte" instance-of="xs:string" node-id="1.46">true</field>',
        '<field name="byte" instance-of="xs:string" node-id="1.46">true</field>',
        '<field name="byte-typed-byte" instance-of="xs:byte" node-id="1.46">false</field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.46">true</binary-field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.46">true</binary-field>',
        '<binary-field name="byte-typed-byte-binary" instance-of="xs:byte" node-id="1.46">false</binary-field>',

        '<field name="byte" instance-of="xs:string" node-id="1.47">true</field>',
        '<field name="byte" instance-of="xs:string" node-id="1.47">true</field>',
        '<field name="byte-typed-byte" instance-of="xs:byte" node-id="1.47">true</field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.47">true</binary-field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.47">true</binary-field>',
        '<binary-field name="byte-typed-byte-binary" instance-of="xs:byte" node-id="1.47">true</binary-field>',

        '<field name="byte" instance-of="xs:string" node-id="1.48">true</field>',
        '<field name="byte" instance-of="xs:string" node-id="1.48">true</field>',
        '<field name="byte-typed-byte" instance-of="xs:byte" node-id="1.48">true</field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.48">true</binary-field>',
        '<binary-field name="byte" instance-of="xs:string" node-id="1.48">true</binary-field>',
        '<binary-field name="byte-typed-byte-binary" instance-of="xs:byte" node-id="1.48">true</binary-field>'
    )
function fields:byte-type-fields() {
    fields:assert-field-types("9*", "byte", function($field-value) { $field-value instance of xs:byte })
};

declare
    %test:assertEquals(
        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.49">true</field>',
        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.49">true</field>',
        '<field name="nonNegativeInteger-typed-nonNegativeInteger" instance-of="xs:nonNegativeInteger" node-id="1.49">false</field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.49">true</binary-field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.49">true</binary-field>',
        '<binary-field name="nonNegativeInteger-typed-nonNegativeInteger-binary" instance-of="xs:nonNegativeInteger" node-id="1.49">false</binary-field>',

        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.50">true</field>',
        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.50">true</field>',
        '<field name="nonNegativeInteger-typed-nonNegativeInteger" instance-of="xs:nonNegativeInteger" node-id="1.50">true</field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.50">true</binary-field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.50">true</binary-field>',
        '<binary-field name="nonNegativeInteger-typed-nonNegativeInteger-binary" instance-of="xs:nonNegativeInteger" node-id="1.50">true</binary-field>',

        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.51">true</field>',
        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.51">true</field>',
        '<field name="nonNegativeInteger-typed-nonNegativeInteger" instance-of="xs:nonNegativeInteger" node-id="1.51">true</field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.51">true</binary-field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.51">true</binary-field>',
        '<binary-field name="nonNegativeInteger-typed-nonNegativeInteger-binary" instance-of="xs:nonNegativeInteger" node-id="1.51">true</binary-field>',

        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.52">true</field>',
        '<field name="nonNegativeInteger" instance-of="xs:string" node-id="1.52">true</field>',
        '<field name="nonNegativeInteger-typed-nonNegativeInteger" instance-of="xs:nonNegativeInteger" node-id="1.52">true</field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.52">true</binary-field>',
        '<binary-field name="nonNegativeInteger" instance-of="xs:string" node-id="1.52">true</binary-field>',
        '<binary-field name="nonNegativeInteger-typed-nonNegativeInteger-binary" instance-of="xs:nonNegativeInteger" node-id="1.52">true</binary-field>'
    )
function fields:nonNegativeInteger-type-fields() {
    fields:assert-field-types("0*", "nonNegativeInteger", function($field-value) { $field-value instance of xs:nonNegativeInteger })
};

declare
    %test:assertEquals(
        '<field name="unsignedLong" instance-of="xs:string" node-id="1.53">true</field>',
        '<field name="unsignedLong" instance-of="xs:string" node-id="1.53">true</field>',
        '<field name="unsignedLong-typed-unsignedLong" instance-of="xs:unsignedLong" node-id="1.53">false</field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.53">true</binary-field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.53">true</binary-field>',
        '<binary-field name="unsignedLong-typed-unsignedLong-binary" instance-of="xs:unsignedLong" node-id="1.53">false</binary-field>',

        '<field name="unsignedLong" instance-of="xs:string" node-id="1.54">true</field>',
        '<field name="unsignedLong" instance-of="xs:string" node-id="1.54">true</field>',
        '<field name="unsignedLong-typed-unsignedLong" instance-of="xs:unsignedLong" node-id="1.54">false</field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.54">true</binary-field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.54">true</binary-field>',
        '<binary-field name="unsignedLong-typed-unsignedLong-binary" instance-of="xs:unsignedLong" node-id="1.54">false</binary-field>',

        '<field name="unsignedLong" instance-of="xs:string" node-id="1.55">true</field>',
        '<field name="unsignedLong" instance-of="xs:string" node-id="1.55">true</field>',
        '<field name="unsignedLong-typed-unsignedLong" instance-of="xs:unsignedLong" node-id="1.55">false</field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.55">true</binary-field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.55">true</binary-field>',
        '<binary-field name="unsignedLong-typed-unsignedLong-binary" instance-of="xs:unsignedLong" node-id="1.55">false</binary-field>',

        '<field name="unsignedLong" instance-of="xs:string" node-id="1.56">true</field>',
        '<field name="unsignedLong" instance-of="xs:string" node-id="1.56">true</field>',
        '<field name="unsignedLong-typed-unsignedLong" instance-of="xs:unsignedLong" node-id="1.56">true</field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.56">true</binary-field>',
        '<binary-field name="unsignedLong" instance-of="xs:string" node-id="1.56">true</binary-field>',
        '<binary-field name="unsignedLong-typed-unsignedLong-binary" instance-of="xs:unsignedLong" node-id="1.56">true</binary-field>'
    )
function fields:unsignedLong-type-fields() {
    fields:assert-field-types("9*", "unsignedLong", function($field-value) { $field-value instance of xs:unsignedLong })
};

declare
    %test:assertEquals(
        '<field name="unsignedInt" instance-of="xs:string" node-id="1.57">true</field>',
        '<field name="unsignedInt" instance-of="xs:string" node-id="1.57">true</field>',
        '<field name="unsignedInt-typed-unsignedInt" instance-of="xs:unsignedInt" node-id="1.57">false</field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.57">true</binary-field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.57">true</binary-field>',
        '<binary-field name="unsignedInt-typed-unsignedInt-binary" instance-of="xs:unsignedInt" node-id="1.57">false</binary-field>',

        '<field name="unsignedInt" instance-of="xs:string" node-id="1.58">true</field>',
        '<field name="unsignedInt" instance-of="xs:string" node-id="1.58">true</field>',
        '<field name="unsignedInt-typed-unsignedInt" instance-of="xs:unsignedInt" node-id="1.58">false</field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.58">true</binary-field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.58">true</binary-field>',
        '<binary-field name="unsignedInt-typed-unsignedInt-binary" instance-of="xs:unsignedInt" node-id="1.58">false</binary-field>',

        '<field name="unsignedInt" instance-of="xs:string" node-id="1.59">true</field>',
        '<field name="unsignedInt" instance-of="xs:string" node-id="1.59">true</field>',
        '<field name="unsignedInt-typed-unsignedInt" instance-of="xs:unsignedInt" node-id="1.59">false</field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.59">true</binary-field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.59">true</binary-field>',
        '<binary-field name="unsignedInt-typed-unsignedInt-binary" instance-of="xs:unsignedInt" node-id="1.59">false</binary-field>',

        '<field name="unsignedInt" instance-of="xs:string" node-id="1.60">true</field>',
        '<field name="unsignedInt" instance-of="xs:string" node-id="1.60">true</field>',
        '<field name="unsignedInt-typed-unsignedInt" instance-of="xs:unsignedInt" node-id="1.60">true</field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.60">true</binary-field>',
        '<binary-field name="unsignedInt" instance-of="xs:string" node-id="1.60">true</binary-field>',
        '<binary-field name="unsignedInt-typed-unsignedInt-binary" instance-of="xs:unsignedInt" node-id="1.60">true</binary-field>'
    )
function fields:unsignedInt-type-fields() {
    fields:assert-field-types("9*", "unsignedInt", function($field-value) { $field-value instance of xs:unsignedInt })
};

declare
    %test:assertEquals(
        '<field name="unsignedShort" instance-of="xs:string" node-id="1.61">true</field>',
        '<field name="unsignedShort" instance-of="xs:string" node-id="1.61">true</field>',
        '<field name="unsignedShort-typed-unsignedShort" instance-of="xs:unsignedShort" node-id="1.61">false</field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.61">true</binary-field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.61">true</binary-field>',
        '<binary-field name="unsignedShort-typed-unsignedShort-binary" instance-of="xs:unsignedShort" node-id="1.61">false</binary-field>',

        '<field name="unsignedShort" instance-of="xs:string" node-id="1.62">true</field>',
        '<field name="unsignedShort" instance-of="xs:string" node-id="1.62">true</field>',
        '<field name="unsignedShort-typed-unsignedShort" instance-of="xs:unsignedShort" node-id="1.62">false</field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.62">true</binary-field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.62">true</binary-field>',
        '<binary-field name="unsignedShort-typed-unsignedShort-binary" instance-of="xs:unsignedShort" node-id="1.62">false</binary-field>',

        '<field name="unsignedShort" instance-of="xs:string" node-id="1.63">true</field>',
        '<field name="unsignedShort" instance-of="xs:string" node-id="1.63">true</field>',
        '<field name="unsignedShort-typed-unsignedShort" instance-of="xs:unsignedShort" node-id="1.63">false</field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.63">true</binary-field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.63">true</binary-field>',
        '<binary-field name="unsignedShort-typed-unsignedShort-binary" instance-of="xs:unsignedShort" node-id="1.63">false</binary-field>',

        '<field name="unsignedShort" instance-of="xs:string" node-id="1.64">true</field>',
        '<field name="unsignedShort" instance-of="xs:string" node-id="1.64">true</field>',
        '<field name="unsignedShort-typed-unsignedShort" instance-of="xs:unsignedShort" node-id="1.64">true</field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.64">true</binary-field>',
        '<binary-field name="unsignedShort" instance-of="xs:string" node-id="1.64">true</binary-field>',
        '<binary-field name="unsignedShort-typed-unsignedShort-binary" instance-of="xs:unsignedShort" node-id="1.64">true</binary-field>'
    )
function fields:unsignedShort-type-fields() {
    fields:assert-field-types("9*", "unsignedShort", function($field-value) { $field-value instance of xs:unsignedShort })
};

declare
    %test:assertEquals(
        '<field name="unsignedByte" instance-of="xs:string" node-id="1.65">true</field>',
        '<field name="unsignedByte" instance-of="xs:string" node-id="1.65">true</field>',
        '<field name="unsignedByte-typed-unsignedByte" instance-of="xs:unsignedByte" node-id="1.65">false</field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.65">true</binary-field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.65">true</binary-field>',
        '<binary-field name="unsignedByte-typed-unsignedByte-binary" instance-of="xs:unsignedByte" node-id="1.65">false</binary-field>',

        '<field name="unsignedByte" instance-of="xs:string" node-id="1.66">true</field>',
        '<field name="unsignedByte" instance-of="xs:string" node-id="1.66">true</field>',
        '<field name="unsignedByte-typed-unsignedByte" instance-of="xs:unsignedByte" node-id="1.66">false</field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.66">true</binary-field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.66">true</binary-field>',
        '<binary-field name="unsignedByte-typed-unsignedByte-binary" instance-of="xs:unsignedByte" node-id="1.66">false</binary-field>',

        '<field name="unsignedByte" instance-of="xs:string" node-id="1.67">true</field>',
        '<field name="unsignedByte" instance-of="xs:string" node-id="1.67">true</field>',
        '<field name="unsignedByte-typed-unsignedByte" instance-of="xs:unsignedByte" node-id="1.67">false</field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.67">true</binary-field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.67">true</binary-field>',
        '<binary-field name="unsignedByte-typed-unsignedByte-binary" instance-of="xs:unsignedByte" node-id="1.67">false</binary-field>',

        '<field name="unsignedByte" instance-of="xs:string" node-id="1.68">true</field>',
        '<field name="unsignedByte" instance-of="xs:string" node-id="1.68">true</field>',
        '<field name="unsignedByte-typed-unsignedByte" instance-of="xs:unsignedByte" node-id="1.68">true</field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.68">true</binary-field>',
        '<binary-field name="unsignedByte" instance-of="xs:string" node-id="1.68">true</binary-field>',
        '<binary-field name="unsignedByte-typed-unsignedByte-binary" instance-of="xs:unsignedByte" node-id="1.68">true</binary-field>'
    )
function fields:unsignedByte-type-fields() {
    fields:assert-field-types("9*", "unsignedByte", function($field-value) { $field-value instance of xs:unsignedByte })
};

declare
    %test:assertEquals(
        '<field name="positiveInteger" instance-of="xs:string" node-id="1.69">true</field>',
        '<field name="positiveInteger" instance-of="xs:string" node-id="1.69">true</field>',
        '<field name="positiveInteger-typed-positiveInteger" instance-of="xs:positiveInteger" node-id="1.69">false</field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.69">true</binary-field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.69">true</binary-field>',
        '<binary-field name="positiveInteger-typed-positiveInteger-binary" instance-of="xs:positiveInteger" node-id="1.69">false</binary-field>',

        '<field name="positiveInteger" instance-of="xs:string" node-id="1.70">true</field>',
        '<field name="positiveInteger" instance-of="xs:string" node-id="1.70">true</field>',
        '<field name="positiveInteger-typed-positiveInteger" instance-of="xs:positiveInteger" node-id="1.70">false</field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.70">true</binary-field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.70">true</binary-field>',
        '<binary-field name="positiveInteger-typed-positiveInteger-binary" instance-of="xs:positiveInteger" node-id="1.70">false</binary-field>',

        '<field name="positiveInteger" instance-of="xs:string" node-id="1.71">true</field>',
        '<field name="positiveInteger" instance-of="xs:string" node-id="1.71">true</field>',
        '<field name="positiveInteger-typed-positiveInteger" instance-of="xs:positiveInteger" node-id="1.71">false</field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.71">true</binary-field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.71">true</binary-field>',
        '<binary-field name="positiveInteger-typed-positiveInteger-binary" instance-of="xs:positiveInteger" node-id="1.71">false</binary-field>',

        '<field name="positiveInteger" instance-of="xs:string" node-id="1.72">true</field>',
        '<field name="positiveInteger" instance-of="xs:string" node-id="1.72">true</field>',
        '<field name="positiveInteger-typed-positiveInteger" instance-of="xs:positiveInteger" node-id="1.72">true</field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.72">true</binary-field>',
        '<binary-field name="positiveInteger" instance-of="xs:string" node-id="1.72">true</binary-field>',
        '<binary-field name="positiveInteger-typed-positiveInteger-binary" instance-of="xs:positiveInteger" node-id="1.72">true</binary-field>'
    )
function fields:positiveInteger-type-fields() {
    fields:assert-field-types("0*", "positiveInteger", function($field-value) { $field-value instance of xs:positiveInteger })
};

declare
    %test:assertEquals(
        '<field name="float" instance-of="xs:string" node-id="1.73">true</field>',
        '<field name="float" instance-of="xs:string" node-id="1.73">true</field>',
        '<field name="float-typed-float" instance-of="xs:float" node-id="1.73">false</field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.73">true</binary-field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.73">true</binary-field>',
        '<binary-field name="float-typed-float-binary" instance-of="xs:float" node-id="1.73">false</binary-field>',

        '<field name="float" instance-of="xs:string" node-id="1.74">true</field>',
        '<field name="float" instance-of="xs:string" node-id="1.74">true</field>',
        '<field name="float-typed-float" instance-of="xs:float" node-id="1.74">true</field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.74">true</binary-field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.74">true</binary-field>',
        '<binary-field name="float-typed-float-binary" instance-of="xs:float" node-id="1.74">true</binary-field>',

        '<field name="float" instance-of="xs:string" node-id="1.75">true</field>',
        '<field name="float" instance-of="xs:string" node-id="1.75">true</field>',
        '<field name="float-typed-float" instance-of="xs:float" node-id="1.75">true</field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.75">true</binary-field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.75">true</binary-field>',
        '<binary-field name="float-typed-float-binary" instance-of="xs:float" node-id="1.75">true</binary-field>',

        '<field name="float" instance-of="xs:string" node-id="1.76">true</field>',
        '<field name="float" instance-of="xs:string" node-id="1.76">true</field>',
        '<field name="float-typed-float" instance-of="xs:float" node-id="1.76">true</field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.76">true</binary-field>',
        '<binary-field name="float" instance-of="xs:string" node-id="1.76">true</binary-field>',
        '<binary-field name="float-typed-float-binary" instance-of="xs:float" node-id="1.76">true</binary-field>'
    )
function fields:float-type-fields() {
    fields:assert-field-types("9*", "float", function($field-value) { $field-value instance of xs:float })
};

declare
    %test:assertEquals(
        '<field name="double" instance-of="xs:string" node-id="1.77">true</field>',
        '<field name="double" instance-of="xs:string" node-id="1.77">true</field>',
        '<field name="double-typed-double" instance-of="xs:double" node-id="1.77">false</field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.77">true</binary-field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.77">true</binary-field>',
        '<binary-field name="double-typed-double-binary" instance-of="xs:double" node-id="1.77">false</binary-field>',

        '<field name="double" instance-of="xs:string" node-id="1.78">true</field>',
        '<field name="double" instance-of="xs:string" node-id="1.78">true</field>',
        '<field name="double-typed-double" instance-of="xs:double" node-id="1.78">true</field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.78">true</binary-field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.78">true</binary-field>',
        '<binary-field name="double-typed-double-binary" instance-of="xs:double" node-id="1.78">true</binary-field>',

        '<field name="double" instance-of="xs:string" node-id="1.79">true</field>',
        '<field name="double" instance-of="xs:string" node-id="1.79">true</field>',
        '<field name="double-typed-double" instance-of="xs:double" node-id="1.79">true</field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.79">true</binary-field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.79">true</binary-field>',
        '<binary-field name="double-typed-double-binary" instance-of="xs:double" node-id="1.79">true</binary-field>',

        '<field name="double" instance-of="xs:string" node-id="1.80">true</field>',
        '<field name="double" instance-of="xs:string" node-id="1.80">true</field>',
        '<field name="double-typed-double" instance-of="xs:double" node-id="1.80">true</field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.80">true</binary-field>',
        '<binary-field name="double" instance-of="xs:string" node-id="1.80">true</binary-field>',
        '<binary-field name="double-typed-double-binary" instance-of="xs:double" node-id="1.80">true</binary-field>'
    )
function fields:double-type-fields() {
    fields:assert-field-types("9*", "double", function($field-value) { $field-value instance of xs:double })
};

declare
    %test:assertEquals(
        '<field name="gYearMonth" instance-of="xs:string" node-id="1.81">true</field>',
        '<field name="gYearMonth" instance-of="xs:string" node-id="1.81">true</field>',
        '<field name="gYearMonth-typed-gYearMonth" instance-of="xs:gYearMonth" node-id="1.81">false</field>',
        '<binary-field name="gYearMonth" instance-of="xs:string" node-id="1.81">true</binary-field>',
        '<binary-field name="gYearMonth" instance-of="xs:string" node-id="1.81">true</binary-field>',
        '<binary-field name="gYearMonth-typed-gYearMonth-binary" instance-of="xs:gYearMonth" node-id="1.81">false</binary-field>',

        '<field name="gYearMonth" instance-of="xs:string" node-id="1.82">true</field>',
        '<field name="gYearMonth" instance-of="xs:string" node-id="1.82">true</field>',
        '<field name="gYearMonth-typed-gYearMonth" instance-of="xs:gYearMonth" node-id="1.82">true</field>',
        '<binary-field name="gYearMonth" instance-of="xs:string" node-id="1.82">true</binary-field>',
        '<binary-field name="gYearMonth" instance-of="xs:string" node-id="1.82">true</binary-field>',
        '<binary-field name="gYearMonth-typed-gYearMonth-binary" instance-of="xs:gYearMonth" node-id="1.82">true</binary-field>'
    )
function fields:gYearMonth-type-fields() {
    fields:assert-field-types("1*", "gYearMonth", function($field-value) { $field-value instance of xs:gYearMonth })
};

declare
    %test:assertEquals(
        '<field name="gYear" instance-of="xs:string" node-id="1.83">true</field>',
        '<field name="gYear" instance-of="xs:string" node-id="1.83">true</field>',
        '<field name="gYear-typed-gYear" instance-of="xs:gYear" node-id="1.83">false</field>',
        '<binary-field name="gYear" instance-of="xs:string" node-id="1.83">true</binary-field>',
        '<binary-field name="gYear" instance-of="xs:string" node-id="1.83">true</binary-field>',
        '<binary-field name="gYear-typed-gYear-binary" instance-of="xs:gYear" node-id="1.83">false</binary-field>',

        '<field name="gYear" instance-of="xs:string" node-id="1.84">true</field>',
        '<field name="gYear" instance-of="xs:string" node-id="1.84">true</field>',
        '<field name="gYear-typed-gYear" instance-of="xs:gYear" node-id="1.84">true</field>',
        '<binary-field name="gYear" instance-of="xs:string" node-id="1.84">true</binary-field>',
        '<binary-field name="gYear" instance-of="xs:string" node-id="1.84">true</binary-field>',
        '<binary-field name="gYear-typed-gYear-binary" instance-of="xs:gYear" node-id="1.84">true</binary-field>'
    )
function fields:gYear-type-fields() {
    fields:assert-field-types("1*", "gYear", function($field-value) { $field-value instance of xs:gYear })
};

declare
    %test:assertEquals(
        '<field name="gMonthDay" instance-of="xs:string" node-id="1.85">true</field>',
        '<field name="gMonthDay" instance-of="xs:string" node-id="1.85">true</field>',
        '<field name="gMonthDay-typed-gMonthDay" instance-of="xs:gMonthDay" node-id="1.85">false</field>',
        '<binary-field name="gMonthDay" instance-of="xs:string" node-id="1.85">true</binary-field>',
        '<binary-field name="gMonthDay" instance-of="xs:string" node-id="1.85">true</binary-field>',
        '<binary-field name="gMonthDay-typed-gMonthDay-binary" instance-of="xs:gMonthDay" node-id="1.85">false</binary-field>',

        '<field name="gMonthDay" instance-of="xs:string" node-id="1.86">true</field>',
        '<field name="gMonthDay" instance-of="xs:string" node-id="1.86">true</field>',
        '<field name="gMonthDay-typed-gMonthDay" instance-of="xs:gMonthDay" node-id="1.86">true</field>',
        '<binary-field name="gMonthDay" instance-of="xs:string" node-id="1.86">true</binary-field>',
        '<binary-field name="gMonthDay" instance-of="xs:string" node-id="1.86">true</binary-field>',
        '<binary-field name="gMonthDay-typed-gMonthDay-binary" instance-of="xs:gMonthDay" node-id="1.86">true</binary-field>'
    )
function fields:gMonthDay-type-fields() {
    fields:assert-field-types("1*", "gMonthDay", function($field-value) { $field-value instance of xs:gMonthDay })
};

declare
    %test:assertEquals(
        '<field name="gMonth" instance-of="xs:string" node-id="1.87">true</field>',
        '<field name="gMonth" instance-of="xs:string" node-id="1.87">true</field>',
        '<field name="gMonth-typed-gMonth" instance-of="xs:gMonth" node-id="1.87">false</field>',
        '<binary-field name="gMonth" instance-of="xs:string" node-id="1.87">true</binary-field>',
        '<binary-field name="gMonth" instance-of="xs:string" node-id="1.87">true</binary-field>',
        '<binary-field name="gMonth-typed-gMonth-binary" instance-of="xs:gMonth" node-id="1.87">false</binary-field>',

        '<field name="gMonth" instance-of="xs:string" node-id="1.88">true</field>',
        '<field name="gMonth" instance-of="xs:string" node-id="1.88">true</field>',
        '<field name="gMonth-typed-gMonth" instance-of="xs:gMonth" node-id="1.88">true</field>',
        '<binary-field name="gMonth" instance-of="xs:string" node-id="1.88">true</binary-field>',
        '<binary-field name="gMonth" instance-of="xs:string" node-id="1.88">true</binary-field>',
        '<binary-field name="gMonth-typed-gMonth-binary" instance-of="xs:gMonth" node-id="1.88">true</binary-field>'
    )
function fields:gMonth-type-fields() {
    fields:assert-field-types("09*", "gMonth", function($field-value) { $field-value instance of xs:gMonth })
};

declare
    %test:assertEquals(
        '<field name="gDay" instance-of="xs:string" node-id="1.89">true</field>',
        '<field name="gDay" instance-of="xs:string" node-id="1.89">true</field>',
        '<field name="gDay-typed-gDay" instance-of="xs:gDay" node-id="1.89">false</field>',
        '<binary-field name="gDay" instance-of="xs:string" node-id="1.89">true</binary-field>',
        '<binary-field name="gDay" instance-of="xs:string" node-id="1.89">true</binary-field>',
        '<binary-field name="gDay-typed-gDay-binary" instance-of="xs:gDay" node-id="1.89">false</binary-field>',

        '<field name="gDay" instance-of="xs:string" node-id="1.90">true</field>',
        '<field name="gDay" instance-of="xs:string" node-id="1.90">true</field>',
        '<field name="gDay-typed-gDay" instance-of="xs:gDay" node-id="1.90">true</field>',
        '<binary-field name="gDay" instance-of="xs:string" node-id="1.90">true</binary-field>',
        '<binary-field name="gDay" instance-of="xs:string" node-id="1.90">true</binary-field>',
        '<binary-field name="gDay-typed-gDay-binary" instance-of="xs:gDay" node-id="1.90">true</binary-field>'
    )
function fields:gDay-type-fields() {
    fields:assert-field-types("1*", "gDay", function($field-value) { $field-value instance of xs:gDay })
};

declare
    %test:assertEquals(
        '<field name="boolean" instance-of="xs:string" node-id="1.91">true</field>',
        '<field name="boolean" instance-of="xs:string" node-id="1.91">true</field>',
        '<field name="boolean-typed-boolean" instance-of="xs:boolean" node-id="1.91">false</field>',
        '<binary-field name="boolean" instance-of="xs:string" node-id="1.91">true</binary-field>',
        '<binary-field name="boolean" instance-of="xs:string" node-id="1.91">true</binary-field>',
        '<binary-field name="boolean-typed-boolean-binary" instance-of="xs:boolean" node-id="1.91">false</binary-field>',

        '<field name="boolean" instance-of="xs:string" node-id="1.92">true</field>',
        '<field name="boolean" instance-of="xs:string" node-id="1.92">true</field>',
        '<field name="boolean-typed-boolean" instance-of="xs:boolean" node-id="1.92">true</field>',
        '<binary-field name="boolean" instance-of="xs:string" node-id="1.92">true</binary-field>',
        '<binary-field name="boolean" instance-of="xs:string" node-id="1.92">true</binary-field>',
        '<binary-field name="boolean-typed-boolean-binary" instance-of="xs:boolean" node-id="1.92">true</binary-field>'
    )
function fields:boolean-type-fields() {
    fields:assert-field-types("t*", "boolean", function($field-value) { $field-value instance of xs:boolean })
};

declare
    %test:assertEquals(
        '<field name="base64Binary" instance-of="xs:string" node-id="1.93">true</field>',
        '<field name="base64Binary" instance-of="xs:string" node-id="1.93">true</field>',
        '<field name="base64Binary-typed-base64Binary" instance-of="xs:base64Binary" node-id="1.93">false</field>',
        '<binary-field name="base64Binary" instance-of="xs:string" node-id="1.93">true</binary-field>',
        '<binary-field name="base64Binary" instance-of="xs:string" node-id="1.93">true</binary-field>',
        '<binary-field name="base64Binary-typed-base64Binary-binary" instance-of="xs:base64Binary" node-id="1.93">false</binary-field>',

        '<field name="base64Binary" instance-of="xs:string" node-id="1.94">true</field>',
        '<field name="base64Binary" instance-of="xs:string" node-id="1.94">true</field>',
        '<field name="base64Binary-typed-base64Binary" instance-of="xs:base64Binary" node-id="1.94">true</field>',
        '<binary-field name="base64Binary" instance-of="xs:string" node-id="1.94">true</binary-field>',
        '<binary-field name="base64Binary" instance-of="xs:string" node-id="1.94">true</binary-field>',
        '<binary-field name="base64Binary-typed-base64Binary-binary" instance-of="xs:base64Binary" node-id="1.94">true</binary-field>'
    )
function fields:base64Binary-type-fields() {
    fields:assert-field-types("d*", "base64Binary", function($field-value) { $field-value instance of xs:base64Binary })
};

declare
    %test:assertEquals(
        '<field name="hexBinary" instance-of="xs:string" node-id="1.95">true</field>',
        '<field name="hexBinary" instance-of="xs:string" node-id="1.95">true</field>',
        '<field name="hexBinary-typed-hexBinary" instance-of="xs:hexBinary" node-id="1.95">false</field>',
        '<binary-field name="hexBinary" instance-of="xs:string" node-id="1.95">true</binary-field>',
        '<binary-field name="hexBinary" instance-of="xs:string" node-id="1.95">true</binary-field>',
        '<binary-field name="hexBinary-typed-hexBinary-binary" instance-of="xs:hexBinary" node-id="1.95">false</binary-field>',

        '<field name="hexBinary" instance-of="xs:string" node-id="1.96">true</field>',
        '<field name="hexBinary" instance-of="xs:string" node-id="1.96">true</field>',
        '<field name="hexBinary-typed-hexBinary" instance-of="xs:hexBinary" node-id="1.96">true</field>',
        '<binary-field name="hexBinary" instance-of="xs:string" node-id="1.96">true</binary-field>',
        '<binary-field name="hexBinary" instance-of="xs:string" node-id="1.96">true</binary-field>',
        '<binary-field name="hexBinary-typed-hexBinary-binary" instance-of="xs:hexBinary" node-id="1.96">true</binary-field>'
    )
function fields:hexBinary-type-fields() {
    fields:assert-field-types("7*", "hexBinary", function($field-value) { $field-value instance of xs:hexBinary })
};

declare
    %test:assertEquals(
        '<field name="anyURI" instance-of="xs:string" node-id="1.97">true</field>',
        '<field name="anyURI" instance-of="xs:string" node-id="1.97">true</field>',
        '<field name="anyURI-typed-anyURI" instance-of="xs:anyURI" node-id="1.97">false</field>',
        '<binary-field name="anyURI" instance-of="xs:string" node-id="1.97">true</binary-field>',
        '<binary-field name="anyURI" instance-of="xs:string" node-id="1.97">true</binary-field>',
        '<binary-field name="anyURI-typed-anyURI-binary" instance-of="xs:anyURI" node-id="1.97">false</binary-field>',

        '<field name="anyURI" instance-of="xs:string" node-id="1.98">true</field>',
        '<field name="anyURI" instance-of="xs:string" node-id="1.98">true</field>',
        '<field name="anyURI-typed-anyURI" instance-of="xs:anyURI" node-id="1.98">true</field>',
        '<binary-field name="anyURI" instance-of="xs:string" node-id="1.98">true</binary-field>',
        '<binary-field name="anyURI" instance-of="xs:string" node-id="1.98">true</binary-field>',
        '<binary-field name="anyURI-typed-anyURI-binary" instance-of="xs:anyURI" node-id="1.98">true</binary-field>'
    )
function fields:anyURI-type-fields() {
    fields:assert-field-types("h*", "anyURI", function($field-value) { $field-value instance of xs:anyURI })
};

declare
    %test:assertEquals(
        '<field name="QName" instance-of="xs:string" node-id="1.99">true</field>',
        '<field name="QName" instance-of="xs:string" node-id="1.99">true</field>',
        '<field name="QName-typed-QName" instance-of="xs:QName" node-id="1.99">false</field>',
        '<binary-field name="QName" instance-of="xs:string" node-id="1.99">true</binary-field>',
        '<binary-field name="QName" instance-of="xs:string" node-id="1.99">true</binary-field>',
        '<binary-field name="QName-typed-QName-binary" instance-of="xs:QName" node-id="1.99">false</binary-field>',

        '<field name="QName" instance-of="xs:string" node-id="1.100">true</field>',
        '<field name="QName" instance-of="xs:string" node-id="1.100">true</field>',
        '<field name="QName-typed-QName" instance-of="xs:QName" node-id="1.100">true</field>',
        '<binary-field name="QName" instance-of="xs:string" node-id="1.100">true</binary-field>',
        '<binary-field name="QName" instance-of="xs:string" node-id="1.100">true</binary-field>',
        '<binary-field name="QName-typed-QName-binary" instance-of="xs:QName" node-id="1.100">true</binary-field>'
    )
function fields:QName-type-fields() {
    fields:assert-field-types("x*", "QName", function($field-value) { $field-value instance of xs:QName })
};

declare
    %test:assertEquals(
        '<field name="NOTATION" instance-of="xs:string" node-id="1.101">true</field>',
        '<field name="NOTATION" instance-of="xs:string" node-id="1.101">true</field>',
        '<field name="NOTATION-typed-NOTATION" instance-of="xs:NOTATION" node-id="1.101">false</field>',
        '<binary-field name="NOTATION" instance-of="xs:string" node-id="1.101">true</binary-field>',
        '<binary-field name="NOTATION" instance-of="xs:string" node-id="1.101">true</binary-field>',
        '<binary-field name="NOTATION-typed-NOTATION-binary" instance-of="xs:NOTATION" node-id="1.101">false</binary-field>',

        '<field name="NOTATION" instance-of="xs:string" node-id="1.102">true</field>',
        '<field name="NOTATION" instance-of="xs:string" node-id="1.102">true</field>',
        '<field name="NOTATION-typed-NOTATION" instance-of="xs:NOTATION" node-id="1.102">false</field>',
        '<binary-field name="NOTATION" instance-of="xs:string" node-id="1.102">true</binary-field>',
        '<binary-field name="NOTATION" instance-of="xs:string" node-id="1.102">true</binary-field>',
        '<binary-field name="NOTATION-typed-NOTATION-binary" instance-of="xs:NOTATION" node-id="1.102">false</binary-field>'
    )
function fields:NOTATION-type-fields() {
    fields:assert-field-types("x*", "NOTATION", function($field-value) { $field-value instance of xs:NOTATION })
};

declare
    %test:assertEquals(
        '<field name="string" instance-of="xs:string" node-id="1.103">true</field>',
        '<field name="string" instance-of="xs:string" node-id="1.103">true</field>',
        '<field name="string-typed-string" instance-of="xs:string" node-id="1.103">true</field>',
        '<binary-field name="string" instance-of="xs:string" node-id="1.103">true</binary-field>',
        '<binary-field name="string" instance-of="xs:string" node-id="1.103">true</binary-field>',
        '<binary-field name="string-typed-string-binary" instance-of="xs:string" node-id="1.103">true</binary-field>',

        '<field name="string" instance-of="xs:string" node-id="1.104">true</field>',
        '<field name="string" instance-of="xs:string" node-id="1.104">true</field>',
        '<field name="string-typed-string" instance-of="xs:string" node-id="1.104">true</field>',
        '<binary-field name="string" instance-of="xs:string" node-id="1.104">true</binary-field>',
        '<binary-field name="string" instance-of="xs:string" node-id="1.104">true</binary-field>',
        '<binary-field name="string-typed-string-binary" instance-of="xs:string" node-id="1.104">true</binary-field>'
    )
function fields:string-type-fields() {
    fields:assert-field-types("A*", "string", function($field-value) { $field-value instance of xs:string })
};

declare
    %test:assertEquals(
        '<field name="normalizedString" instance-of="xs:string" node-id="1.105">true</field>',
        '<field name="normalizedString" instance-of="xs:string" node-id="1.105">true</field>',
        '<field name="normalizedString-typed-normalizedString" instance-of="xs:normalizedString" node-id="1.105">true</field>',
        '<binary-field name="normalizedString" instance-of="xs:string" node-id="1.105">true</binary-field>',
        '<binary-field name="normalizedString" instance-of="xs:string" node-id="1.105">true</binary-field>',
        '<binary-field name="normalizedString-typed-normalizedString-binary" instance-of="xs:normalizedString" node-id="1.105">true</binary-field>',

        '<field name="normalizedString" instance-of="xs:string" node-id="1.106">true</field>',
        '<field name="normalizedString" instance-of="xs:string" node-id="1.106">true</field>',
        '<field name="normalizedString-typed-normalizedString" instance-of="xs:normalizedString" node-id="1.106">true</field>',
        '<binary-field name="normalizedString" instance-of="xs:string" node-id="1.106">true</binary-field>',
        '<binary-field name="normalizedString" instance-of="xs:string" node-id="1.106">true</binary-field>',
        '<binary-field name="normalizedString-typed-normalizedString-binary" instance-of="xs:normalizedString" node-id="1.106">true</binary-field>'
    )
function fields:normalizedString-type-fields() {
    fields:assert-field-types("A*", "normalizedString", function($field-value) { $field-value instance of xs:normalizedString })
};

declare
    %test:assertEquals(
        '<field name="token" instance-of="xs:string" node-id="1.107">true</field>',
        '<field name="token" instance-of="xs:string" node-id="1.107">true</field>',
        '<field name="token-typed-token" instance-of="xs:token" node-id="1.107">true</field>',
        '<binary-field name="token" instance-of="xs:string" node-id="1.107">true</binary-field>',
        '<binary-field name="token" instance-of="xs:string" node-id="1.107">true</binary-field>',
        '<binary-field name="token-typed-token-binary" instance-of="xs:token" node-id="1.107">true</binary-field>',

        '<field name="token" instance-of="xs:string" node-id="1.108">true</field>',
        '<field name="token" instance-of="xs:string" node-id="1.108">true</field>',
        '<field name="token-typed-token" instance-of="xs:token" node-id="1.108">true</field>',
        '<binary-field name="token" instance-of="xs:string" node-id="1.108">true</binary-field>',
        '<binary-field name="token" instance-of="xs:string" node-id="1.108">true</binary-field>',
        '<binary-field name="token-typed-token-binary" instance-of="xs:token" node-id="1.108">true</binary-field>'
    )
function fields:token-type-fields() {
    fields:assert-field-types("A*", "token", function($field-value) { $field-value instance of xs:token })
};

declare
    %test:assertEquals(
        '<field name="language" instance-of="xs:string" node-id="1.109">true</field>',
        '<field name="language" instance-of="xs:string" node-id="1.109">true</field>',
        '<field name="language-typed-language" instance-of="xs:language" node-id="1.109">false</field>',
        '<binary-field name="language" instance-of="xs:string" node-id="1.109">true</binary-field>',
        '<binary-field name="language" instance-of="xs:string" node-id="1.109">true</binary-field>',
        '<binary-field name="language-typed-language-binary" instance-of="xs:language" node-id="1.109">false</binary-field>',

        '<field name="language" instance-of="xs:string" node-id="1.110">true</field>',
        '<field name="language" instance-of="xs:string" node-id="1.110">true</field>',
        '<field name="language-typed-language" instance-of="xs:language" node-id="1.110">true</field>',
        '<binary-field name="language" instance-of="xs:string" node-id="1.110">true</binary-field>',
        '<binary-field name="language" instance-of="xs:string" node-id="1.110">true</binary-field>',
        '<binary-field name="language-typed-language-binary" instance-of="xs:language" node-id="1.110">true</binary-field>'
    )
function fields:language-type-fields() {
    fields:assert-field-types("en*", "language", function($field-value) { $field-value instance of xs:language })
};

declare
    %test:assertEquals(
        '<field name="NMTOKEN" instance-of="xs:string" node-id="1.111">true</field>',
        '<field name="NMTOKEN" instance-of="xs:string" node-id="1.111">true</field>',
        '<field name="NMTOKEN-typed-NMTOKEN" instance-of="xs:NMTOKEN" node-id="1.111">false</field>',
        '<binary-field name="NMTOKEN" instance-of="xs:string" node-id="1.111">true</binary-field>',
        '<binary-field name="NMTOKEN" instance-of="xs:string" node-id="1.111">true</binary-field>',
        '<binary-field name="NMTOKEN-typed-NMTOKEN-binary" instance-of="xs:NMTOKEN" node-id="1.111">false</binary-field>',

        '<field name="NMTOKEN" instance-of="xs:string" node-id="1.112">true</field>',
        '<field name="NMTOKEN" instance-of="xs:string" node-id="1.112">true</field>',
        '<field name="NMTOKEN-typed-NMTOKEN" instance-of="xs:NMTOKEN" node-id="1.112">true</field>',
        '<binary-field name="NMTOKEN" instance-of="xs:string" node-id="1.112">true</binary-field>',
        '<binary-field name="NMTOKEN" instance-of="xs:string" node-id="1.112">true</binary-field>',
        '<binary-field name="NMTOKEN-typed-NMTOKEN-binary" instance-of="xs:NMTOKEN" node-id="1.112">true</binary-field>'
    )
function fields:NMTOKEN-type-fields() {
    fields:assert-field-types("A*", "NMTOKEN", function($field-value) { $field-value instance of xs:NMTOKEN })
};

declare
    %test:assertEquals(
        '<field name="Name" instance-of="xs:string" node-id="1.113">true</field>',
        '<field name="Name" instance-of="xs:string" node-id="1.113">true</field>',
        '<field name="Name-typed-Name" instance-of="xs:Name" node-id="1.113">false</field>',
        '<binary-field name="Name" instance-of="xs:string" node-id="1.113">true</binary-field>',
        '<binary-field name="Name" instance-of="xs:string" node-id="1.113">true</binary-field>',
        '<binary-field name="Name-typed-Name-binary" instance-of="xs:Name" node-id="1.113">false</binary-field>',

        '<field name="Name" instance-of="xs:string" node-id="1.114">true</field>',
        '<field name="Name" instance-of="xs:string" node-id="1.114">true</field>',
        '<field name="Name-typed-Name" instance-of="xs:Name" node-id="1.114">true</field>',
        '<binary-field name="Name" instance-of="xs:string" node-id="1.114">true</binary-field>',
        '<binary-field name="Name" instance-of="xs:string" node-id="1.114">true</binary-field>',
        '<binary-field name="Name-typed-Name-binary" instance-of="xs:Name" node-id="1.114">true</binary-field>'
    )
function fields:Name-type-fields() {
    fields:assert-field-types("A*", "Name", function($field-value) { $field-value instance of xs:Name })
};

declare
    %test:assertEquals(
        '<field name="NCName" instance-of="xs:string" node-id="1.115">true</field>',
        '<field name="NCName" instance-of="xs:string" node-id="1.115">true</field>',
        '<field name="NCName-typed-NCName" instance-of="xs:NCName" node-id="1.115">false</field>',
        '<binary-field name="NCName" instance-of="xs:string" node-id="1.115">true</binary-field>',
        '<binary-field name="NCName" instance-of="xs:string" node-id="1.115">true</binary-field>',
        '<binary-field name="NCName-typed-NCName-binary" instance-of="xs:NCName" node-id="1.115">false</binary-field>',

        '<field name="NCName" instance-of="xs:string" node-id="1.116">true</field>',
        '<field name="NCName" instance-of="xs:string" node-id="1.116">true</field>',
        '<field name="NCName-typed-NCName" instance-of="xs:NCName" node-id="1.116">true</field>',
        '<binary-field name="NCName" instance-of="xs:string" node-id="1.116">true</binary-field>',
        '<binary-field name="NCName" instance-of="xs:string" node-id="1.116">true</binary-field>',
        '<binary-field name="NCName-typed-NCName-binary" instance-of="xs:NCName" node-id="1.116">true</binary-field>'
    )
function fields:NCName-type-fields() {
    fields:assert-field-types("A*", "NCName", function($field-value) { $field-value instance of xs:NCName })
};

declare
    %test:assertEquals(
        '<field name="ID" instance-of="xs:string" node-id="1.117">true</field>',
        '<field name="ID" instance-of="xs:string" node-id="1.117">true</field>',
        '<field name="ID-typed-ID" instance-of="xs:ID" node-id="1.117">false</field>',
        '<binary-field name="ID" instance-of="xs:string" node-id="1.117">true</binary-field>',
        '<binary-field name="ID" instance-of="xs:string" node-id="1.117">true</binary-field>',
        '<binary-field name="ID-typed-ID-binary" instance-of="xs:ID" node-id="1.117">false</binary-field>',

        '<field name="ID" instance-of="xs:string" node-id="1.118">true</field>',
        '<field name="ID" instance-of="xs:string" node-id="1.118">true</field>',
        '<field name="ID-typed-ID" instance-of="xs:ID" node-id="1.118">true</field>',
        '<binary-field name="ID" instance-of="xs:string" node-id="1.118">true</binary-field>',
        '<binary-field name="ID" instance-of="xs:string" node-id="1.118">true</binary-field>',
        '<binary-field name="ID-typed-ID-binary" instance-of="xs:ID" node-id="1.118">true</binary-field>'
    )
function fields:ID-type-fields() {
    fields:assert-field-types("A*", "ID", function($field-value) { $field-value instance of xs:ID })
};

declare
    %test:assertEquals(
        '<field name="IDREF" instance-of="xs:string" node-id="1.119">true</field>',
        '<field name="IDREF" instance-of="xs:string" node-id="1.119">true</field>',
        '<field name="IDREF-typed-IDREF" instance-of="xs:IDREF" node-id="1.119">false</field>',
        '<binary-field name="IDREF" instance-of="xs:string" node-id="1.119">true</binary-field>',
        '<binary-field name="IDREF" instance-of="xs:string" node-id="1.119">true</binary-field>',
        '<binary-field name="IDREF-typed-IDREF-binary" instance-of="xs:IDREF" node-id="1.119">false</binary-field>',

        '<field name="IDREF" instance-of="xs:string" node-id="1.120">true</field>',
        '<field name="IDREF" instance-of="xs:string" node-id="1.120">true</field>',
        '<field name="IDREF-typed-IDREF" instance-of="xs:IDREF" node-id="1.120">true</field>',
        '<binary-field name="IDREF" instance-of="xs:string" node-id="1.120">true</binary-field>',
        '<binary-field name="IDREF" instance-of="xs:string" node-id="1.120">true</binary-field>',
        '<binary-field name="IDREF-typed-IDREF-binary" instance-of="xs:IDREF" node-id="1.120">true</binary-field>'
    )
function fields:IDREF-type-fields() {
    fields:assert-field-types("A*", "IDREF", function($field-value) { $field-value instance of xs:IDREF })
};

declare
    %test:assertEquals(
        '<field name="ENTITY" instance-of="xs:string" node-id="1.121">true</field>',
        '<field name="ENTITY" instance-of="xs:string" node-id="1.121">true</field>',
        '<field name="ENTITY-typed-ENTITY" instance-of="xs:ENTITY" node-id="1.121">false</field>',
        '<binary-field name="ENTITY" instance-of="xs:string" node-id="1.121">true</binary-field>',
        '<binary-field name="ENTITY" instance-of="xs:string" node-id="1.121">true</binary-field>',
        '<binary-field name="ENTITY-typed-ENTITY-binary" instance-of="xs:ENTITY" node-id="1.121">false</binary-field>',

        '<field name="ENTITY" instance-of="xs:string" node-id="1.122">true</field>',
        '<field name="ENTITY" instance-of="xs:string" node-id="1.122">true</field>',
        '<field name="ENTITY-typed-ENTITY" instance-of="xs:ENTITY" node-id="1.122">true</field>',
        '<binary-field name="ENTITY" instance-of="xs:string" node-id="1.122">true</binary-field>',
        '<binary-field name="ENTITY" instance-of="xs:string" node-id="1.122">true</binary-field>',
        '<binary-field name="ENTITY-typed-ENTITY-binary" instance-of="xs:ENTITY" node-id="1.122">true</binary-field>'
    )
function fields:ENTITY-type-fields() {
    fields:assert-field-types("A*", "ENTITY", function($field-value) { $field-value instance of xs:ENTITY })
};

declare
    %test:assertEquals("2025-07-06-04:55", "2025-07-06-04:55")
function fields:date-typed-field-preserves-timezone() {
    let $date-elem := doc($fields:TYPES_DOC_PATH)/types/element()[local-name() eq "date"][empty(@description)][ft:query(., "2025*")]
    return
        (
            ft:field($date-elem, "date-typed-date", "xs:date"),
            ft:binary-field($date-elem, "date-typed-date-binary", "xs:date")
        )
};

declare
    %test:assertEquals("08:50:49.831-04:55", "08:50:49.831-04:55")
function fields:time-typed-field-preserves-timezone() {
    let $time-elem := doc($fields:TYPES_DOC_PATH)/types/element()[local-name() eq "time"][empty(@description)][ft:query(., "08*")]
    return
        (
            ft:field($time-elem, "time-typed-time", "xs:time"),
            ft:binary-field($time-elem, "time-typed-time-binary", "xs:time")
        )
};

declare
    %test:assertEquals("2025-07-06T08:50:58.265-04:55", "2025-07-06T08:50:58.265-04:55")
function fields:dateTime-typed-field-preserves-timezone() {
    let $dateTime-elem := doc($fields:TYPES_DOC_PATH)/types/element()[local-name() eq "dateTime"][empty(@description)][ft:query(., "2025*")]
    return
        (
            ft:field($dateTime-elem, "dateTime-typed-dateTime", "xs:dateTime"),
            ft:binary-field($dateTime-elem, "dateTime-typed-dateTime-binary", "xs:dateTime")
        )
};

declare
    %test:assertEquals("2025-07-06T08:51:11.932-04:55", "2025-07-06T08:51:11.932-04:55")
function fields:dateTimeStamp-typed-field-preserves-timezone() {
    let $dateTimeStamp-elem := doc($fields:TYPES_DOC_PATH)/types/element()[local-name() eq "dateTimeStamp"][empty(@description)][ft:query(., "2025*")]
    return
        (
            ft:field($dateTimeStamp-elem, "dateTimeStamp-typed-dateTimeStamp", "xs:dateTimeStamp"),
            ft:binary-field($dateTimeStamp-elem, "dateTimeStamp-typed-dateTimeStamp-binary", "xs:dateTimeStamp")
        )
};
