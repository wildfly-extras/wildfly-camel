/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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
package org.wildfly.camel.test.cdi;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.HttpRequest;
import org.wildfly.camel.test.cdi.subA.SimpleServlet;

/**
 * Test the CDI component.
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Oct-2014
 */
@RunWith(Arquillian.class)
public class CDIIntegrationTest {

    static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cdi-integration-tests");
        archive.addClasses(HttpRequest.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.apache.camel,org.jboss.gravia,org.wildfly.camel");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSimpleWar() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
        	String res = HttpRequest.get(getEndpointAddress("/simple?name=Kermit"), 10, TimeUnit.SECONDS);
            Assert.assertEquals("Hello Kermit", res);
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return managementClient.getWebUri() + contextPath;
    }

    @Deployment(name = SIMPLE_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addPackage(SimpleServlet.class.getPackage());
        archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        /*
         * This archive bundles
         * 
         * 	 camel-cdi 
         * 	 deltaspike-core-api 
         * 	 deltaspike-core-impl 
         * 	 deltaspike-cdictrl-api
         * 
         *  [TODO] Replace these embedded jars with (automatic) references to the respective modules
         */
        JavaArchive[] libs = Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.camel:camel-cdi").withTransitivity().as(JavaArchive.class);
        for (JavaArchive lib : libs) {
			String name = lib.getName();
        	if (name.startsWith("camel-cdi") || name.startsWith("deltaspike")) {
        		archive.addAsLibrary(lib);
			}
        }
        //System.out.println(archive.toString(true));
        return archive;
    }
}
