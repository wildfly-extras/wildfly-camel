/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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

import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CamelEnablementHawtioTest {

    private static final String DEPLOYMENT_HAWTIO_WAR = "hawtio.war";

    @Deployment
    public static WebArchive hawtioDeployment() {
        final StringAsset jbossWebAsset = new StringAsset("<jboss-web><context-root>test-hawtio</context-root></jboss-web>");
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_HAWTIO_WAR);
        archive.addAsWebInfResource(jbossWebAsset, "jboss-web.xml");
        return archive;
    }

    @Test(expected = NoClassDefFoundError.class)
    public void testCamelDisabled() {
        new DefaultCamelContext();
    }
}
