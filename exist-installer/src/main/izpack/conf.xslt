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
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        version="2.0">
    <xsl:param name="dataDir" as="xs:string" required="yes"/>
    <xsl:output omit-xml-declaration="no" indent="yes" encoding="UTF-8" method="xml"/>
    <xsl:template match="db-connection[@files]">
        <xsl:copy>
            <xsl:copy-of select="@*[local-name() ne 'files']"/><xsl:attribute name="files" select="$dataDir"/>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="recovery[@journal-dir]">
        <xsl:copy>
            <xsl:copy-of select="@*[local-name() ne 'journal-dir']"/><xsl:attribute name="journal-dir" select="$dataDir"/>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>