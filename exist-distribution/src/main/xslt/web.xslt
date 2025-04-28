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
    xmlns="https://jakarta.ee/xml/ns/jakartaee"
    xmlns:jee="https://jakarta.ee/xml/ns/jakartaee"
    version="2.0">
    
    <xsl:output method="xml" indent="yes" omit-xml-declaration="no"/>
    
    <xsl:template match="jee:servlet[jee:servlet-name eq 'JMXServlet']" exclude-result-prefixes="jee">
        <xsl:copy-of select="."/>
        
        <xsl:comment>
            Milton provides the WebDAV interface
        </xsl:comment>
        <servlet>
            <servlet-name>milton</servlet-name>
            <servlet-class>org.exist.webdav.MiltonWebDAVServlet</servlet-class>

            <init-param>
                <param-name>resource.factory.class</param-name>
                <param-value>org.exist.webdav.ExistResourceFactory</param-value>
            </init-param>
            
            <xsl:comment>
                Some WebDAV clients send a "Expect: 100-continue" header before 
                uploading body data. Servlet containers (like tomcat and jetty) handle 
                the header in a wrong way, making a client not work OK.
                Set value to TRUE to restore old behavior (FALSE is the new default 
                value, hardcoded in MiltonWebDAVServlet).       
            </xsl:comment>
            <xsl:text disable-output-escaping="yes">
      &lt;!-- </xsl:text>
            <init-param>
                <param-name>enable.expect.continue</param-name>
                <param-value>false</param-value>
            </init-param>
            <xsl:text disable-output-escaping="yes">
      --&gt;    
    </xsl:text>
            <xsl:comment>
                Uncomment to enable debugging
            </xsl:comment>
            <xsl:text disable-output-escaping="yes">
      &lt;!-- </xsl:text>
            <init-param>
                <param-name>filter_0</param-name>
                <param-value>com.bradmcevoy.http.DebugFilter</param-value>
            </init-param>
            <xsl:text disable-output-escaping="yes">
      --&gt;
    </xsl:text>
        </servlet>
    </xsl:template>
    
    <xsl:template match="jee:servlet[jee:servlet-name eq 'XSLTServlet']" exclude-result-prefixes="jee">
        <xsl:copy-of select="."/>
        
        <xsl:comment>
        EXQuery - RESTXQ
    </xsl:comment>
        <servlet>
            <servlet-name>RestXqServlet</servlet-name>
            <servlet-class>org.exist.extensions.exquery.restxq.impl.RestXqServlet</servlet-class>
        </servlet>
    </xsl:template>
    
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>