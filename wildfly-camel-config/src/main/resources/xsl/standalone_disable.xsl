<?xml version="1.0" encoding="UTF-8"?>
<!--
 - Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors.
 - 
 - Licensed under the Apache License, Version 2.0 (the "License");
 - you may not use this file except in compliance with the License.
 - You may obtain a copy of the License at
 - http://www.apache.org/licenses/LICENSE-2.0
 - Unless required by applicable law or agreed to in writing, software
 - distributed under the License is distributed on an "AS IS" BASIS,
 - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 - See the License for the specific language governing permissions and
 - limitations under the License.
 -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:ns1="urn:jboss:domain:switchyard:1.0"
                xmlns:as="urn:jboss:domain:1.7"
                xmlns:security="urn:jboss:domain:security:1.2"
                xmlns:camel="urn:jboss:domain:camel:1.0"
                exclude-result-prefixes="xs xsl xsi fn xdt as camel security">

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="as:extension[@module='org.wildfly.extension.camel']">
  </xsl:template>

  <xsl:template match="as:property[starts-with(@name,'hawtio.')]">
  </xsl:template>

  <xsl:template match="//*[local-name()='subsystem' and contains(namespace-uri(),'urn:jboss:domain:weld')]">
    <xsl:copy>
      <xsl:apply-templates select="@*[name(.)!='require-bean-descriptor']|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="security:security-domain[@name='hawtio-domain']">
  </xsl:template>

  <xsl:template match="camel:subsystem">
  </xsl:template>

</xsl:stylesheet>
