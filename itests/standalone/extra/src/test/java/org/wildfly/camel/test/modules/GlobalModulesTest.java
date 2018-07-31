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

package org.wildfly.camel.test.modules;

import java.net.URL;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.modules.subA.MyServlet;

@RunWith(Arquillian.class)
public class GlobalModulesTest {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-global-modules.war");
        archive.addAsWebInfResource("modules/jboss-camel-context.xml", "jboss-camel-context.xml");
        archive.addClasses(HttpRequest.class, MyServlet.class);
        return archive;
    }

    @Test
    public void testClassLoaderAccess() throws Exception {

        Properties props = new Properties();
        ClassLoader loader = getClass().getClassLoader();
        URL propsA = loader.getResource("/psetA.properties");
        URL propsB = loader.getResource("/psetB.properties");
        Assert.assertNotNull("propsA not null", propsA);
        Assert.assertNotNull("propsB not null", propsB);

        System.out.println("Found: " + propsA);
        System.out.println("Found: " + propsB);
        props.load(propsA.openStream());
        props.load(propsB.openStream());

        Assert.assertEquals("valA", props.get("keyA"));
        Assert.assertEquals("valB", props.get("keyB"));
    }

    @Test
    public void testCamelRouteAccess() throws Exception {

        HttpResponse result = HttpRequest.get("http://localhost:8080/camel-global-modules").getResponse();
        Assert.assertEquals("Hello valA valB", result.getBody());
    }
}
