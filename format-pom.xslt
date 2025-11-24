<?xml version="1.0" encoding="UTF-8"?>
<!--

    Elemental
    Copyright (C) 2024, Evolved Binary Ltd

    admin@evolvedbinary.com
    https://www.evolvedbinary.com | https://www.elemental.xyz

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; version 2.1.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

-->
<!--
    Formats a pom.xml file and makes sure
    that any license include(s)/exclude(s) are
    in the correct order.
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:pom="http://maven.apache.org/POM/4.0.0"
  xmlns:my="my-functions"
  exclude-result-prefixes="xs pom my"
  version="2.0">

  <xsl:output method="xml" indent="no" omit-xml-declaration="no" encoding="UTF-8" cdata-section-elements="pom:preamble pom:separator"/>
  
  <xsl:template match="comment()[parent::document-node()][1]">
    <xsl:text>&#xA;</xsl:text>
    <xsl:copy/>
    <xsl:text>&#xA;</xsl:text>
  </xsl:template>
  
  <xsl:template match="pom:includes[parent::pom:licenseSet]">
    <xsl:copy>
      <xsl:text>&#xA;</xsl:text>

      <xsl:for-each select="comment()[not(preceding-sibling::pom:include)]">
        <xsl:text>                                </xsl:text>
        <xsl:copy/>
        <xsl:text>&#xA;</xsl:text>
      </xsl:for-each>
      
      <xsl:call-template name="process-includes-excludes-children">
        <xsl:with-param name="current-child" select="child::node()[. instance of element(pom:include) or . instance of processing-instruction(license-section)][1]"/>
        <xsl:with-param name="accumulator" select="()"/>
      </xsl:call-template>

      <xsl:for-each select="comment()[not(following-sibling::pom:include)]">
        <xsl:text>                                </xsl:text>
        <xsl:copy/>
        <xsl:text>&#xA;</xsl:text>
      </xsl:for-each>

      <xsl:text>                            </xsl:text>
      
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="pom:excludes[parent::pom:licenseSet]">
    <xsl:copy>
      <xsl:text>&#xA;</xsl:text>
      
      <xsl:for-each select="comment()[not(preceding-sibling::pom:exclude)]">
        <xsl:text>                                </xsl:text>
        <xsl:copy/>
        <xsl:text>&#xA;</xsl:text>
      </xsl:for-each>
      
      <xsl:call-template name="process-includes-excludes-children">
        <xsl:with-param name="current-child" select="child::node()[. instance of element(pom:exclude) or . instance of processing-instruction(license-section)][1]"/>
        <xsl:with-param name="accumulator" select="()"/>
      </xsl:call-template>
      
      <xsl:for-each select="comment()[not(following-sibling::pom:exclude)]">
        <xsl:text>                                </xsl:text>
        <xsl:copy/>
        <xsl:text>&#xA;</xsl:text>
      </xsl:for-each>
      
      <xsl:text>                            </xsl:text>
      
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="process-includes-excludes-children">
    <xsl:param name="current-child" as="node()?"/>
    <xsl:param name="accumulator" as="node()*"/>

    <xsl:choose>
      <xsl:when test="empty($current-child)">
        <!-- NOTE(AR) no more children to process -->
        <xsl:if test="exists($accumulator)">
          <!-- SECTION-3 -->
            <xsl:call-template name="sort-includes-excludes">
              <xsl:with-param name="includes-excludes" select="$accumulator"/>
            </xsl:call-template>
          <!-- /SECTION-3 -->
        </xsl:if>
      </xsl:when>
      
      <xsl:when test="$current-child instance of processing-instruction(license-section)">
        <xsl:choose>
          <xsl:when test="starts-with(string($current-child), 'start ') or string($current-child) eq 'end'">
            <!-- NOTE(AR) entered or exited a license-section PI -->
            <xsl:if test="exists($accumulator)">
              <!-- SECTION-1 -->
                <xsl:call-template name="sort-includes-excludes">
                  <xsl:with-param name="includes-excludes" select="$accumulator"/>
                </xsl:call-template>
              <!-- /SECTION-1 -->
            </xsl:if>
            
            <!-- xsl:copy-of select="$current-child/preceding-sibling::text()[1]"/ -->
            <xsl:if test="starts-with(string($current-child), 'start ')">
              <xsl:text>&#xA;</xsl:text>
            </xsl:if>
            
            <xsl:text>                                </xsl:text>
            <xsl:copy-of select="$current-child"/>
            <xsl:text>&#xA;</xsl:text>
            
            <!-- NOTE(AR) Recursive call for next child -->
            <xsl:variable name="next-child" as="node()?" select="$current-child/following-sibling::node()[1]"/>
            <xsl:call-template name="process-includes-excludes-children">
              <xsl:with-param name="current-child" select="$next-child"/>
              <xsl:with-param name="accumulator" select="()"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:message terminate="yes">Unrecognised license-section PI</xsl:message>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>

      <xsl:otherwise>
        <!-- NOTE(AR) Recursive call for next child, but note that Text and Comment nodes will not be appended to the accumulator -->
        <xsl:variable name="item" as="node()?" select="if ($current-child instance of text() or $current-child instance of comment()) then () else $current-child"/>
        <xsl:variable name="next-child" as="node()?" select="$current-child/following-sibling::node()[1]"/>
        <xsl:call-template name="process-includes-excludes-children">
          <xsl:with-param name="current-child" select="$next-child"/>
          <xsl:with-param name="accumulator" select="($accumulator, $item)"/>
        </xsl:call-template>
      </xsl:otherwise>
      
    </xsl:choose>
  </xsl:template>
  
  <xsl:template name="sort-includes-excludes">
    <xsl:param name="includes-excludes" as="element()+"/>

    <xsl:for-each-group select="$includes-excludes" group-by="my:package-group-keys(my:key-for-sort(.))">
      <xsl:sort select="my:package-group-keys(my:key-for-sort(.))"/>
      
      <xsl:for-each select="current-group()">
        <xsl:sort select="my:key-for-sort(.)"/>
        
        <xsl:text>                                </xsl:text>
        <xsl:copy>
          
          <!-- NOTE(AR) Useful for debugging sort order issues -->
          <!--
          <xsl:attribute name="sort-key" select="my:key-for-sort(.)"/>
          -->
          
          <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
        <xsl:text>&#xA;</xsl:text>
      </xsl:for-each>
      
    </xsl:for-each-group>
  </xsl:template>

  <xsl:function name="my:key-for-sort" as="xs:string">
    <xsl:param name="str" as="xs:string"/>
    <xsl:variable name="str1" select="replace($str, 'src/((test)|(main))/', 'src/ZZ/')"/>
    <xsl:variable name="str2" select="replace($str1, 'src/ZZ/java/xquery/', 'src/ZZ/xquery/')"/>
    <xsl:variable name="str3" select="replace($str2, '/resources-filtered/', '/resources/')"/>
    <xsl:variable name="str4" select="replace($str3, '/((java)|(antlr)|(resources)|(xquery)|(xjb)|(xsd)|(xslt))/org/exist/', '/ZZ/org/exist/')"/>
    <xsl:variable name="str5" select="replace($str4, '/((java)|(antlr)|(resources)|(xquery)|(xjb)|(xsd)|(xslt))/org/expath/', '/ZZ/org/expath/')"/>
    <xsl:variable name="str6" select="replace($str5, '/((java)|(antlr)|(resources)|(xquery)|(xjb)|(xsd)|(xslt))/xyz/elemental/', '/ZZ/xyz/elemental/')"/>
    <xsl:variable name="str7" select="lower-case($str6)"/>
    <xsl:sequence select="$str7"/>
  </xsl:function>
  
  <xsl:function name="my:package-group-keys" as="xs:string*">
    <xsl:param name="str" as="xs:string"/>
    <xsl:variable name="str1" select="replace($str, '^(?:(.+)/)?[^/]+$', '$1')"/>
    <xsl:variable name="str2" select="tokenize($str1)"/>
    <xsl:choose>
      <xsl:when test="empty($str2)">
        <!-- NOTE(AR) if there is no group key, place it in group '0' -->
        <xsl:sequence select="'0'"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="$str2"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

</xsl:stylesheet>