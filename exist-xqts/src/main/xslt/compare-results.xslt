<?xml version="1.0" encoding="UTF-8"?>
<!--

    Elemental
    Copyright (C) 2024, Evolved Binary Ltd

    admin@evolvedbinary.com
    https://www.evolvedbinary.com | https://www.elemental.xyz

    Use of this software is governed by the Business Source License 1.1
    included in the LICENSE file and at www.mariadb.com/bsl11.

    Change Date: 2028-04-27

    On the date above, in accordance with the Business Source License, use
    of this software will be governed by the Apache License, Version 2.0.

    Additional Use Grant: Production use of the Licensed Work for a permitted
    purpose. A Permitted Purpose is any purpose other than a Competing Use.
    A Competing Use means making the Software available to others in a commercial
    product or service that: substitutes for the Software; substitutes for any
    other product or service we offer using the Software that exists as of the
    date we make the Software available; or offers the same or substantially
    similar functionality as the Software.

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:cr="http://exist-db.org/exist-xqts/compare-results"
    exclude-result-prefixes="xs"
    version="2.0">

    <xsl:param name="xqts.previous.junit-data-path" as="xs:string" required="yes"/>
    <xsl:param name="xqts.current.junit-data-path" as="xs:string" required="yes"/>


    <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes" encoding="UTF-8"/>


    <xsl:template name="compare-results" as="document-node(element(cr:comparison))">
        <xsl:variable name="previous-summary" select="cr:summarise-results($xqts.previous.junit-data-path)" as="document-node(element(cr:results))"/>
        <xsl:variable name="current-summary" select="cr:summarise-results($xqts.current.junit-data-path)" as="document-node(element(cr:results))"/>
        <xsl:document>
            <cr:comparison>
                <cr:previous>
                    <xsl:copy select="$previous-summary/cr:results">
                        <xsl:copy-of select="@*"/>
                    </xsl:copy>
                </cr:previous>
                <cr:current>
                    <xsl:copy select="$current-summary/cr:results">
                        <xsl:copy-of select="@*"/>
                    </xsl:copy>
                </cr:current>
                <cr:change>
                    <cr:results>
                        <xsl:for-each select="('tests', 'skipped', 'failures', 'errors', 'time')">
                            <xsl:sequence select="cr:calculate-change($previous-summary/cr:results, $current-summary/cr:results, .)"/>
                        </xsl:for-each>
                    </cr:results>
                    <cr:new>
                        <xsl:for-each select="('pass', 'skipped', 'failures', 'errors')">
                            <xsl:sequence select="cr:new-changes($previous-summary/cr:results, $current-summary/cr:results, .)"/>
                        </xsl:for-each>
                    </cr:new>
                </cr:change>
            </cr:comparison>
        </xsl:document>
    </xsl:template>

    <xsl:function name="cr:summarise-results" as="document-node(element(cr:results))">
        <xsl:param name="junit-data-path" as="xs:string" required="yes"/>
        <xsl:variable name="collection-uri" select="concat($junit-data-path, '?select=*.xml')"/>
        <xsl:variable name="testsuite" select="collection($collection-uri)/testsuite"/>
        <xsl:document>
            <cr:results tests="{sum($testsuite/@tests/xs:integer(.))}" skipped="{sum($testsuite/@skipped/xs:integer(.))}" failures="{sum($testsuite/@failures/xs:integer(.))}" errors="{sum($testsuite/@errors/xs:integer(.))}" time="{sum($testsuite/@time/xs:float(.))}">
                <cr:skipped>
                    <xsl:sequence select="$testsuite/testcase[skipped]"/>
                </cr:skipped>
                <cr:failures>
                    <xsl:sequence select="$testsuite/testcase[failure]"/>
                </cr:failures>
                <cr:errors>
                    <xsl:sequence select="$testsuite/testcase[error]"/>
                </cr:errors>
                <cr:pass>
                    <xsl:sequence select="$testsuite/testcase[empty(skipped)][empty(failure)][empty(error)]"/>
                </cr:pass>
            </cr:results>
        </xsl:document>
    </xsl:function>

    <xsl:function name="cr:calculate-change" as="attribute()+">
        <xsl:param name="previous-results" as="element(cr:results)" required="yes"/>
        <xsl:param name="current-results" as="element(cr:results)" required="yes"/>
        <xsl:param name="attr-name" as="xs:string" required="yes"/>
        
        <xsl:variable name="previous-attr" select="$previous-results/@*[local-name(.) eq $attr-name]"/>
        <xsl:variable name="current-attr" select="$current-results/@*[local-name(.) eq $attr-name]"/>
        
        <xsl:attribute name="{$attr-name}" select="$current-attr - $previous-attr"/>
        <xsl:attribute name="{$attr-name}-pct" select="(($current-attr - $previous-attr) div $previous-attr) * 100"/>
    </xsl:function>

    <xsl:function name="cr:new-changes">
        <xsl:param name="previous-results" as="element(cr:results)" required="yes"/>
        <xsl:param name="current-results" as="element(cr:results)" required="yes"/>
        <xsl:param name="attr-name" as="xs:string" required="yes"/>
        <xsl:variable name="elem-name" as="xs:QName" select="xs:QName(concat('cr:', $attr-name))"/>
        <xsl:variable name="previous-results-names" as="xs:string*" select="$previous-results/element()[node-name(.) eq $elem-name]/testcase/@name/string(.)"/>
        <xsl:element name="cr:{$attr-name}">
            <xsl:apply-templates mode="simple" select="$current-results/element()[node-name(.) eq $elem-name]/testcase[not(@name = $previous-results-names)]"/>
        </xsl:element>
    </xsl:function>

    <xsl:template match="testcase" mode="simple">
        <xsl:copy>
            <xsl:copy-of select="@name"/>
            <xsl:copy-of select="failure|error"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>