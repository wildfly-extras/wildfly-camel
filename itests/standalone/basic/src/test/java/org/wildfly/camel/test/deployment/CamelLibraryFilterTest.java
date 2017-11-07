/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.deployment;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CamelLibraryFilterTest {

    private static final String CAMEL_WAR_A = "camel-a.war";
    private static final String CAMEL_WAR_B = "camel-b.war";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-library-filter-tests.jar");
    }

    @Deployment(name = CAMEL_WAR_A, testable = false, managed = false)
    public static WebArchive createWarWithCamelLib() {
        // Add a lib provided by WFC
        File[] libraryDependencies = Maven.configureResolverViaPlugin().
            resolve("org.apache.camel:camel-core").
            withTransitivity().
            asFile();

        return ShrinkWrap.create(WebArchive.class, CAMEL_WAR_A)
            .addAsManifestResource(EmptyAsset.INSTANCE, "spring/camel-context.xml")
            .addAsLibraries(libraryDependencies);
    }

    @Deployment(name = CAMEL_WAR_B, testable = false, managed = false)
    public static WebArchive createWarWithCamelLibAndModuleDependency() {
        // Add a camel lib that is not provided by WFC
        File[] libraryDependencies = Maven.configureResolverViaPlugin().
            resolve("org.apache.camel:camel-blueprint").
            withoutTransitivity().
            asFile();

        return ShrinkWrap.create(WebArchive.class, CAMEL_WAR_B)
            .addAsManifestResource(EmptyAsset.INSTANCE, "spring/camel-context.xml")
            .addAsLibraries(libraryDependencies)
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.apache.camel.core");
                return builder.openStream();
            });
    }

    @Test
    public void testCamelLibraryFilter() throws Exception {
        expectedException.expect(RuntimeException.class);
        deployer.deploy(CAMEL_WAR_A);
    }

    @Test
    public void testCamelLibraryFilterWithModuleDependency() throws Exception {
        expectedException.expect(RuntimeException.class);
        deployer.deploy(CAMEL_WAR_B);
    }

}
