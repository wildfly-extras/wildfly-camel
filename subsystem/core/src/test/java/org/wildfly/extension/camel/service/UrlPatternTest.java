package org.wildfly.extension.camel.service;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.camel.service.CamelEndpointDeployerService.UrlPattern;

public class UrlPatternTest {

    static void assertPath(String urlPattern, String path, String expectedRelativePattern) {
        final UrlPattern pattern = new UrlPattern(urlPattern);
        Assert.assertEquals(expectedRelativePattern, pattern.relativize(path));
    }

    @Test
    public void test() {
        assertPath("/*", "/", "/*");
        assertPath("/foo/*", "/foo", "/*");
        assertPath("/foo", "/foo", "");
        assertPath("/foo/bar", "/foo/bar", "");
        assertPath("/foo/*", "/foo/bar", "");
        assertPath("/foo/*/", "/foo/bar", "/");
        assertPath("/*", "/foo", "");
        assertPath("///", "///", "");
        assertPath("///*", "///foo", "");
        assertPath("///*", "///foo/bar", null);
        assertPath("///foo/*", "///foo", "/*");
        assertPath("///foo/*", "///foo/bar", "");
    }
}
