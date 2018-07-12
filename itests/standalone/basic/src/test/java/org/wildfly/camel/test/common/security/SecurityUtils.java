/*
* #%L
* Wildfly Camel :: Testsuite
* %%
* Copyright (C) 2013 - 2018 RedHat
* %%
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* #L%
*/
package org.wildfly.camel.test.common.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.common.utils.FileUtils;
import org.wildfly.camel.test.common.utils.WildFlyCli;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SecurityUtils {
    private static final String APPLICATION_KEYSTORE_PASSWORD = "password";

    private static final String CLIENT_CERT_KEYSTORE_PASSWORD = "123456";

    private static final String CLIENT_CRT = "client.crt";

    private static final String CLIENT_KEYSTORE = "client.keystore";
    private static final String CLIENT_TRUSTSTORE = "client.truststore";
    private static final String JBOSS_WEB_XML_TEMPLATE = "<jboss-web><security-domain>%s</security-domain></jboss-web>";
    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String SERVER_CRT = "server.crt";
    private static final String SERVER_KEYSTORE = "server.keystore";
    private static final String SERVER_TRUSTSTORE = "server.truststore";
    public static final String SPRING_CONSUMER_ENDPOINT_ADDRESS = "https://localhost:8443/webservices/greeting-secure-spring";
    private static final String UNTRUSTED_CRT = "untrusted.crt";

    private static final String UNTRUSTED_KEYSTORE = "untrusted.keystore";
    private static final String WEB_XML_SECURITY_CONSTRAINT_TEMPLATE = "<security-constraint>"
            + "<display-name>SecurityConstraint%d</display-name>"
            + "<web-resource-collection>"
            + "<web-resource-name>All Resources</web-resource-name>"
            + "<url-pattern>%s</url-pattern>"
            + "</web-resource-collection>"
            + "<auth-constraint>"
            + "<role-name>%s</role-name>"
            + "</auth-constraint>"
            + "</security-constraint>"
    ;

    private static final String WEB_XML_TEMPLATE = "<web-app>"
            + "%s" // security constraints
            + "<security-role>"
            + "<role-name>testRole</role-name>"
            + "<role-name>testRoleSub</role-name>"
            + "</security-role>"
            + "<login-config>"
            + "<auth-method>%s</auth-method>"
            + "</login-config>"
            + "</web-app>"
    ;

    public static void addSpringXml(WebArchive archive) {
        final StringBuilder sb = new StringBuilder();
        try {
            FileUtils.copy(
                    SecurityUtils.class.getClassLoader().getResource("cxf/secure/spring/cxfws-camel-context.xml"), sb);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final String xml = sb.toString().replace("${SPRING_CONSUMER_ENDPOINT_ADDRESS}",
                SPRING_CONSUMER_ENDPOINT_ADDRESS);
        archive.addAsWebInfResource(new StringAsset(xml), "cxfws-camel-context.xml");
    }

    private static void copy(String fileName, Path targetDirectory) throws IOException {
        FileUtils.copy(SecurityUtils.class.getClassLoader().getResource("security/keys/" + fileName),
                targetDirectory.resolve(fileName));
    }

    /**
     * Copies server and clients keystores and truststores from this package to the given
     * {@code $wildflyHome/standalone/configuration}. Server truststore has accepted certificate from client keystore
     * and vice-versa
     *
     * @param wildflyHome
     * @throws java.io.IOException copying of keystores fails
     * @throws IllegalArgumentException workingFolder is null or it's not a directory
     */
    public static void copyKeyMaterial(final Path wildflyHome) throws IOException, IllegalArgumentException {
        final Path targetDirectory = wildflyHome.resolve("standalone/configuration");
        if (targetDirectory == null || !Files.isDirectory(targetDirectory)) {
            throw new IllegalArgumentException("Provide an existing folder as the method parameter.");
        }
        copy(SERVER_KEYSTORE, targetDirectory);
        copy(SERVER_TRUSTSTORE, targetDirectory);
        copy(SERVER_CRT, targetDirectory);
        copy(CLIENT_KEYSTORE, targetDirectory);
        copy(CLIENT_TRUSTSTORE, targetDirectory);
        copy(CLIENT_CRT, targetDirectory);
        copy(UNTRUSTED_KEYSTORE, targetDirectory);
        copy(UNTRUSTED_CRT, targetDirectory);
    }

    public static SSLConnectionSocketFactory createBasicSocketFactory(final Path wildflyHome)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
            IOException, UnrecoverableKeyException {
        final Path truststoreFile = wildflyHome.resolve("standalone/configuration/application.keystore");
        return createSocketFactory(truststoreFile, null, APPLICATION_KEYSTORE_PASSWORD);

    }

    static SSLConnectionSocketFactory createSocketFactory(Path truststoreFile, Path keystoreFile, String password)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
            IOException, UnrecoverableKeyException {
        final char[] pwd = password.toCharArray();
        SSLContextBuilder sslcontextBuilder = SSLContexts.custom()//
                .loadTrustMaterial(truststoreFile.toFile(), pwd, TrustSelfSignedStrategy.INSTANCE)//
        ;
        if (keystoreFile != null) {
            sslcontextBuilder.loadKeyMaterial(keystoreFile.toFile(), pwd, pwd);
        }

        return new SSLConnectionSocketFactory(sslcontextBuilder.build(), new HostnameVerifier() {
            @Override
            public boolean verify(final String s, final SSLSession sslSession) {
                return true;
            }
        });
    }

    public static SSLConnectionSocketFactory createTrustedClientCertSocketFactory(final Path wildflyHome)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
            IOException, UnrecoverableKeyException {
        final Path truststoreFile = wildflyHome.resolve("standalone/configuration/client.truststore");
        final Path keystoreFile = wildflyHome.resolve("standalone/configuration/client.keystore");
        return createSocketFactory(truststoreFile, keystoreFile, CLIENT_CERT_KEYSTORE_PASSWORD);

    }

    public static SSLConnectionSocketFactory createUntrustedClientCertSocketFactory(final Path wildflyHome)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
            IOException, UnrecoverableKeyException {
        final Path truststoreFile = wildflyHome.resolve("standalone/configuration/client.truststore");
        final Path keystoreFile = wildflyHome.resolve("standalone/configuration/untrusted.keystore");
        return createSocketFactory(truststoreFile, keystoreFile, CLIENT_CERT_KEYSTORE_PASSWORD);

    }

    public static void enhanceArchive(WebArchive archive, String securityDomain, String authMethod,
            Map<String, String> uriRolesMap) {
        final String securityConstraints = uriRolesMap.entrySet().stream()
                .map(en -> String.format(WEB_XML_SECURITY_CONSTRAINT_TEMPLATE, (int) (Math.random() * 1000),
                        en.getKey(), en.getValue()))
                .collect(Collectors.joining());
        final String webXml = String.format(WEB_XML_TEMPLATE, securityConstraints, authMethod);

        archive.addClasses(WildFlyCli.class, SecurityUtils.class, EnvironmentUtils.class)
                .addAsWebInfResource(new StringAsset(String.format(JBOSS_WEB_XML_TEMPLATE, securityDomain)),
                        "jboss-web.xml")//
                .addAsWebInfResource(new StringAsset(webXml), "web.xml");
    }

    private SecurityUtils() {
    }

}
