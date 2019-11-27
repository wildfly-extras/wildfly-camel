/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.plain.ldap;

import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.SimpleRegistry;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.plain.ldap.DirectoryServiceBuilder.SetupResult;

@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP")})
@ApplyLdifFiles("ldap/LdapRouteTest.ldif")
public class LdapRouteTest  {

    private static DirectoryService directoryService;
    private static LdapServer ldapServer;
    private static Registry registry;

    @BeforeClass
    public static void beforeClass() throws Exception {

        SetupResult setupResult = DirectoryServiceBuilder.setupDirectoryService(LdapRouteTest.class);
        directoryService = setupResult.getDirectoryService();
        ldapServer = setupResult.getLdapServer();

        // You can assign port number in the @CreateTransport annotation
        int port = ldapServer.getPort();

        LdapContext ctx = getWiredContext(ldapServer);

        registry = new SimpleRegistry();
        registry.bind("localhost:" + port, ctx);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        DirectoryServiceBuilder.shutdownDirectoryService(directoryService);
    }

    @Test
    public void testLdapRouteStandard() throws Exception {

        try (CamelContext camel = new DefaultCamelContext(registry)) {

            camel.addRoutes(new RouteBuilder() {
                public void configure() throws Exception {
                    from("direct:start").to("ldap:localhost:" + ldapServer.getPort() + "?base=ou=system");
                }
            });
            camel.start();

            Endpoint endpoint = camel.getEndpoint("direct:start");
            Exchange exchange = endpoint.createExchange();
            exchange.getIn().setBody("(!(ou=test1))");

            // now we send the exchange to the endpoint, and receives the response from Camel
            ProducerTemplate template = camel.createProducerTemplate();
            Exchange out = template.send(endpoint, exchange);
            Collection<SearchResult> searchResults = defaultLdapModuleOutAssertions(out);

            assertFalse(contains("uid=test1,ou=test,ou=system", searchResults));
            assertTrue(contains("uid=test2,ou=test,ou=system", searchResults));
            assertTrue(contains("uid=testNoOU,ou=test,ou=system", searchResults));
            assertTrue(contains("uid=tcruise,ou=actors,ou=system", searchResults));
        }
    }


    @SuppressWarnings("unchecked")
    private Collection<SearchResult> defaultLdapModuleOutAssertions(Exchange out) {
        assertNotNull(out);
        assertNotNull(out.getMessage());
        Collection<SearchResult> data = out.getMessage().getBody(Collection.class);
        assertNotNull("out body could not be converted to a Collection - was: " + out.getMessage().getBody(), data);
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
}
