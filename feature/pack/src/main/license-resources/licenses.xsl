<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" encoding="utf-8" standalone="no" media-type="text/html" />
    <xsl:param name="version"/>
    <xsl:param name="product.release.name"/>
    <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz '" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ!'" />

    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
                <link rel="stylesheet" type="text/css" href="licenses.css"/>
            </head>
            <body>
                <h2><xsl:value-of select="$product.release.name"/><xsl:text> </xsl:text><xsl:value-of select="substring-before($version, '-')"/> - Feature Pack</h2>
                <p>The following material has been provided for informational purposes only, and should not be relied upon or construed as a legal opinion or legal advice.</p>
                <!-- Read matching templates -->
                <table>
                    <tr>
                        <th>Package Group</th>
                        <th>Package Artifact</th>
                        <th>Package Version</th>
                        <th>Remote Licenses</th>
                        <th>Local Licenses</th>
                    </tr>
                    <xsl:for-each select="licenseSummary/dependencies/dependency">
                        <xsl:sort select="concat(groupId, '.', artifactId)"/>
                        <tr>
                            <td><xsl:value-of select="groupId"/></td>
                            <td><xsl:value-of select="artifactId"/></td>
                            <td><xsl:value-of select="version"/></td>
                            <td>
                                <xsl:for-each select="licenses/license">
                                    <xsl:choose>
                                        <xsl:when test="name = 'Public Domain'">
                                            <xsl:value-of select="name"/><br/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <a href="{./url}"><xsl:value-of select="name"/></a><br/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:for-each>
                            </td>
                            <td>
                                <xsl:for-each select="licenses/license/file">
                                    <a href="{text()}"><xsl:value-of select="text()"/></a><br/>
                                </xsl:for-each>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
