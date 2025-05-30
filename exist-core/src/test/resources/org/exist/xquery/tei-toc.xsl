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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:math="http://www.w3.org/2005/xpath-functions/math" xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" exclude-result-prefixes="xs math xd tei" version="3.0">

    <xsl:param name="heading" as="xs:boolean" select="true()"/>
    <xsl:param name="documentID" as="xs:string" select="/tei:TEI/@xml:id"/>

    <xsl:output indent="true"/>

    <xsl:mode on-no-match="shallow-skip" use-accumulators="#all"/>
    <xsl:mode name="html" on-no-match="text-only-copy"/>

    <xsl:accumulator name="document-nos" initial-value="()" as="xs:string*">
        <xsl:accumulator-rule match="tei:div[@type eq 'document']" select="($value, @n)" phase="end"/>
    </xsl:accumulator>

    <xsl:accumulator name="document-ids" initial-value="()" as="xs:string*">
        <xsl:accumulator-rule match="tei:div[tei:div/@type = 'document']" select="()"/>
        <xsl:accumulator-rule match="tei:div[@type eq 'document']" select="($value, @xml:id)" phase="end"/>
    </xsl:accumulator>

    <xsl:template match="tei:TEI">
        <div class="toc">
            <div class="toc__header">
                <h4 class="title">Contents</h4>
            </div>
            <nav aria-label="Side navigation,,," class="toc__chapters">
                <ul class="chapters js-smoothscroll">
                    <xsl:apply-templates select="tei:text"/>
                </ul>
            </nav>
        </div>
    </xsl:template>

    <xsl:template match="tei:div[@xml:id][not(@type = ('document'))]">
        <xsl:variable name="accDocs" as="xs:string*" select="accumulator-after('document-nos')"/>
        <xsl:variable name="prevDocs" as="xs:string*" select="accumulator-before('document-nos')"/>
        <xsl:variable name="docs" as="xs:string*" select="$accDocs[not(. = $prevDocs)]"/>
        <xsl:variable name="prevDocIDs" as="xs:string*" select="accumulator-before('document-ids')"/>
        <xsl:variable name="docIDs" as="xs:string*" select="accumulator-after('document-ids')[not(. = $prevDocIDs)]"/>
        <li data-tei-id="{@xml:id}">
            <xsl:if test="exists($docIDs) and tei:div[@type='document']">
                <xsl:attribute name="data-tei-documents" select="string-join($docIDs, ' ')"/>
            </xsl:if>

            <a href="/{$documentID}/{@xml:id}">
                <xsl:apply-templates mode="html" select="tei:head"/>
            </a>
            <xsl:value-of select="(' (Document' || 's'[count($docs) gt 1] || ' ' || $docs[1] || ' - '[count($docs) gt 1] || $docs[last()][count($docs) gt 1] || ')')[exists($docs)]"/>

            <xsl:where-populated>
                <ul class="chapters__nested">
                    <xsl:apply-templates/>
                </ul>
            </xsl:where-populated>
        </li>
    </xsl:template>

    <xsl:template match="tei:div[@xml:id eq 'toc']" priority="2"/>

    <xsl:template match="tei:head/tei:note" mode="html"/>

    <xsl:template match="tei:lb" mode="html">
        <br/>
    </xsl:template>

</xsl:stylesheet>
