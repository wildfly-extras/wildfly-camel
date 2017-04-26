<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ns="http://org/wildfly/test/jaxb/model/Customer">
    <xsl:output omit-xml-declaration="yes"/>
    <xsl:template match="/ns:customer">
        <xsl:value-of select="ns:firstName"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="ns:lastName"/>
    </xsl:template>
</xsl:transform>