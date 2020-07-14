/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2019 RedHat
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
package org.wildfly.camel.test.undertow;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.undertow.subA.UndertowSecureRoutes1;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class UndertowInvalidArchiveTest {

    private static final String UNDERTOW_DEPLOYMENT_INVALID = "camel-undertow-invalid.jar";
    private static final String UNDERTOW_DEPLOYMENT_VALID = "camel-undertow-valid.war";

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-undertow-invalid-archive-tests.jar")
            .addClass(HttpRequest.class);
    }

    @Deployment(testable = false, managed = false, name = UNDERTOW_DEPLOYMENT_INVALID)
    public static JavaArchive invalidUndertowDeployment() {
        return ShrinkWrap.create(JavaArchive.class, UNDERTOW_DEPLOYMENT_INVALID)
            .addClass(UndertowSecureRoutes1.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(testable = false, managed = false, name = UNDERTOW_DEPLOYMENT_VALID)
    public static WebArchive validUndertowDeployment() {
        return ShrinkWrap.create(WebArchive.class, UNDERTOW_DEPLOYMENT_VALID)
            .addClass(UndertowSecureRoutes1.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testInvalidArchive() throws Exception {
        try {
            deployer.deploy(UNDERTOW_DEPLOYMENT_INVALID);
            Assert.fail("Expected deployment to be unsuccessful");
        } catch (Exception e) {
        	// expeced
        } finally {
            deployer.undeploy(UNDERTOW_DEPLOYMENT_INVALID);
        }


        // Expect a follow-up WAR deployment with the same Undertow handler paths to be accepted
        // We do this check to verify https://github.com/wildfly-extras/wildfly-camel/issues/2841
        try {
            deployer.deploy(UNDERTOW_DEPLOYMENT_VALID);

            HttpResponse response = HttpRequest.get("http://localhost:8080/test").getResponse();
            Assert.assertEquals("GET: /test", response.getBody());
        } finally {
            deployer.undeploy(UNDERTOW_DEPLOYMENT_VALID);
        }
    }
}
