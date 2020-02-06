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

package org.wildfly.camel.test.classloading;

import java.net.URL;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.modules.subA.CamelServlet;

@RunWith(Arquillian.class)
public class PropertiesOnWarClasspathTest {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-warprops.war");
        archive.addAsWebInfResource("modules/jboss-camel-context.xml", "jboss-camel-context.xml");
        archive.addAsWebInfResource("modules/psetA.properties", "psetAA.properties");
        archive.addAsWebInfResource("modules/psetB.properties", "psetBA.properties");
        archive.addClasses(HttpRequest.class, CamelServlet.class);
        System.out.println(archive.toString(true));
        return archive;
    }

    @Test
    @Ignore("[#2944] Cannot load properties from servlet classpath")
    public void testClassLoaderAccess() throws Exception {

        Properties props = new Properties();
        ClassLoader loader = getClass().getClassLoader();
        URL propsA = loader.getResource("/WEB-INF/psetAA.properties");
        URL propsB = loader.getResource("/WEB-INF/psetBA.properties");
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

        // Note, jboss-camel-context.xml does not actually load the properties from the deployment
        // Instead it loads psetA, psetB from global modules

        HttpResponse result = HttpRequest.get("http://localhost:8080/camel-warprops/camel").getResponse();
        Assert.assertEquals("Hello valA valB", result.getBody());
    }
}
