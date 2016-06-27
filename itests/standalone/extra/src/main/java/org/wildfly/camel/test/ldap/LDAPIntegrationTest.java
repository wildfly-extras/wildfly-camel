/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wildfly.camel.test.ldap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.directory.api.ldap.util.JndiUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.ldap.DirectoryServiceBuilder.SetupResult;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ LDAPIntegrationTest.LDAPServerSetupTask.class })
public class LDAPIntegrationTest {

    @CreateLdapServer(transports = { @CreateTransport(protocol = "LDAP") })
    @ApplyLdifFiles("ldap/LdapRouteTest.ldif")
    static class LDAPServerSetupTask implements ServerSetupTask {

        private SetupResult setupResult;

        @Override
        public void setup(final ManagementClient managementClient, String containerId) throws Exception {
            setupResult = DirectoryServiceBuilder.setupDirectoryService(LDAPServerSetupTask.class);
            int port = setupResult.getLdapServer().getPort();
            Path filePath = Paths.get(System.getProperty("jboss.home"), "standalone", "data", "ldap-port");
            try (PrintWriter fw = new PrintWriter(new FileWriter(filePath.toFile()))) {
                fw.println("" + port);
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
            if (setupResult != null) {
                setupResult.getLdapServer().stop();
                DirectoryServiceBuilder.shutdownDirectoryService(setupResult.getDirectoryService());
            }
        }
    }

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        File[] libs = Maven.configureResolverViaPlugin().resolve(
                "org.apache.directory.api:api-ldap-codec-core", 
                "org.apache.directory.api:api-ldap-extras-util",
                "org.apache.directory.api:api-ldap-codec-standalone")
                .withTransitivity().asFile();

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-ldap-tests.war");
        archive.addAsLibraries(libs);
        return archive;
    }

    int ldapPort;
    
    @Before
    public void before() throws IOException {
        File ldapPortFile = new File (System.getProperty("jboss.server.data.dir"), "ldap-port");
        try (BufferedReader fw = new BufferedReader(new FileReader(ldapPortFile))) {
            ldapPort = Integer.parseInt(fw.readLine());
        }
    }
    
    @Test
    public void testLdapRouteStandard() throws Exception {

        SimpleRegistry reg = new SimpleRegistry();
        reg.put("localhost:" + ldapPort, getWiredContext(ldapPort));
        
        CamelContext camelctx = new DefaultCamelContext(reg);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ldap:localhost:" + ldapPort + "?base=ou=system");
            }
        });
        
        camelctx.start();
        try {

            Endpoint endpoint = camelctx.getEndpoint("direct:start");
            Exchange exchange = endpoint.createExchange();
            // then we set the LDAP filter on the in body
            exchange.getIn().setBody("(!(ou=test1))");

            // now we send the exchange to the endpoint, and receives the response from Camel
            Exchange out = camelctx.createProducerTemplate().send(endpoint, exchange);
            Collection<SearchResult> searchResults = defaultLdapModuleOutAssertions(out);

            Assert.assertFalse(contains("uid=test1,ou=test,ou=system", searchResults));
            Assert.assertTrue(contains("uid=test2,ou=test,ou=system", searchResults));
            Assert.assertTrue(contains("uid=testNoOU,ou=test,ou=system", searchResults));
            Assert.assertTrue(contains("uid=tcruise,ou=actors,ou=system", searchResults));
        } finally {
            camelctx.stop();
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<SearchResult> defaultLdapModuleOutAssertions(Exchange out) {
        Assert.assertNotNull(out);
        Assert.assertNotNull(out.getOut());
        Collection<SearchResult> data = out.getOut().getBody(Collection.class);
        Assert.assertNotNull("out body could not be converted to a Collection - was: " + out.getOut().getBody(), data);
        return data;
    }

    private boolean contains(String dn, Collection<SearchResult> results) {
        for (SearchResult result : results) {
            if (result.getNameInNamespace().equals(dn)) {
                return true;
            }
        }
        return false;
    }

    private LdapContext getWiredContext(int port) throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + InetAddress.getLocalHost().getHostName() + ":" + port);
        env.put(Context.SECURITY_PRINCIPAL, ServerDNConstants.ADMIN_SYSTEM_DN);
        env.put(Context.SECURITY_CREDENTIALS, "secret");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        LdapApiService ldapApiService = new StandaloneLdapApiService();
        return new InitialLdapContext(env, JndiUtils.toJndiControls(ldapApiService));
    }
}