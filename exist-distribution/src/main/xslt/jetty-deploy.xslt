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

    NOTE: Parts of this file contain code from 'The eXist-db Authors'.
          The original license header is included below.

    =====================================================================

    eXist-db Open Source Native XML Database
    Copyright (C) 2001 The eXist-db Authors

    info@exist-db.org
    http://www.exist-db.org

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">
    <xsl:output method="xml" omit-xml-declaration="no" doctype-public="-//Jetty//Configure//EN" doctype-system="https://www.eclipse.org/jetty/configure_10_0.dtd" indent="yes"/>
    <xsl:template match="Set[@name eq 'monitoredDirName']">
        <xsl:copy><xsl:copy-of select="@*"/><xsl:copy-of select="Property[@name eq 'jetty.base']"/>/etc/jetty/<xsl:copy-of select="Property[@name eq 'jetty.deploy.monitoredDir']"/></xsl:copy>
    </xsl:template>
    <xsl:template match="Set[@name eq 'defaultsDescriptor']">
        <xsl:copy><xsl:copy-of select="@*"/><xsl:copy-of select="Property[@name eq 'jetty.home']"/>/etc/jetty/webdefault.xml</xsl:copy>
    </xsl:template>
    <xsl:template match="Set[@name eq 'war' and SystemProperty/Default/Property[@name eq 'jetty.home']]">
        <xsl:copy><xsl:copy-of select="@*"/><xsl:copy-of select="SystemProperty/Default/Property[@name eq 'jetty.home']"/>/etc/<xsl:value-of select="tokenize(SystemProperty/Default/text(),'/')[last() - 1]"/></xsl:copy>
    </xsl:template>
    <xsl:template match="Property[@name = ('jetty.sslContext.keyStorePath', 'jetty.sslContext.trustStorePath')]">
        <xsl:copy><xsl:copy-of select="@*[local-name(.) ne 'default']"/><xsl:attribute name="default" select="'etc/jetty/elemental-server.p12'"/></xsl:copy>
    </xsl:template>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>