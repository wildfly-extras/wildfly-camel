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
                xmlns:as="urn:jboss:domain:1.7"
                xmlns:security="urn:jboss:domain:security:1.2"
                xmlns:camel="urn:jboss:domain:camel:1.0"
                exclude-result-prefixes="xs xsl xsi fn xdt as camel security">

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" cdata-section-elements="camelContext"/>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="as:extensions[not(as:extension[@module='org.wildfly.extension.camel'])]">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <extension module="org.wildfly.extension.camel"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="//*[local-name()='subsystem' and contains(namespace-uri(),'urn:jboss:domain:weld')]">
    <xsl:copy>
      <xsl:attribute name="require-bean-descriptor">true</xsl:attribute>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- TODO: handle the case where the system-properties already exists -->
  <xsl:template match="as:server[not(as:system-properties)]">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <system-properties>
        <xsl:comment> Hawt.io Security </xsl:comment>
        <property name="hawtio.authenticationEnabled" value="true" />
        <property name="hawtio.offline" value="true" />
        <property name="hawtio.realm" value="hawtio-domain" />
        <property name="hawtio.role" value="admin" />
      </system-properties>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="security:subsystem/security:security-domains[not(security:security-domain[@name='hawtio-domain'])]">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <security-domain name="hawtio-domain" cache-type="default">
          <authentication>
              <login-module code="RealmDirect" flag="required">
                  <module-option name="realm" value="ManagementRealm"/>
              </login-module>
          </authentication>
      </security-domain>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="as:profile[not(camel:subsystem)]">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <subsystem xmlns="urn:jboss:domain:camel:1.0">
        <xsl:comment>You can add static camelContext definitions here.</xsl:comment>
<xsl:comment><![CDATA[
           <camelContext id="system-context-1">
             <![CDATA[
             <route>
               <from uri="direct:start"/>
               <transform>
                 <simple>Hello #{body}</simple>
               </transform>
             </route>
             ]]]]><![CDATA[>
           </camelContext>
]]></xsl:comment>
      </subsystem>
    </xsl:copy>
  </xsl:template>


</xsl:stylesheet>
