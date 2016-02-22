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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.classloading.subA.FakeActivationSpec;
import org.wildfly.camel.test.classloading.subA.FakeResourceAdapter;

@RunWith(Arquillian.class)
public class CamelEnablementResourceAdapterTest {

    private static final String DEPLOYMENT_RESADAPTOR_RAR = "myadapter.rar";

    @Deployment
    public static ResourceAdapterArchive rarDeployment() {
        return ShrinkWrap.create(ResourceAdapterArchive.class, DEPLOYMENT_RESADAPTOR_RAR)
            .addAsManifestResource("classloading/resource-adapter.xml", "ra.xml")
            .addAsLibrary(ShrinkWrap.create(JavaArchive.class)
                .addClasses(FakeResourceAdapter.class, FakeActivationSpec.class, CamelEnablementResourceAdapterTest.class)
            );
    }

    @Test(expected = NoClassDefFoundError.class)
    public void testCamelDisabled() {
        new DefaultCamelContext();
    }
}
