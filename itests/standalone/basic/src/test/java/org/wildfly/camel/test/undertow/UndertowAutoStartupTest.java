/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.undertow.subA.ApplicationScopeRouteBuilder;

@RunAsClient
@RunWith(Arquillian.class)
public class UndertowAutoStartupTest {

	protected final Logger LOG = LoggerFactory.getLogger(UndertowAutoStartupTest.class);
	
    private static final String CONTEXT_NAME = "camel-undertow";
    private static final String DEPLOYMENT_NAME = CONTEXT_NAME + ".war";

    @Deployment(testable = false)
    public static WebArchive warDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
    	archive.addClasses(ApplicationScopeRouteBuilder.class);
    	archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
		return archive;
    }

    @Test
    public void testUndertowEndpoint() throws Exception {
    	
        String httpUrl = "http://127.0.0.1:8080/hello?name=Kermit";
		HttpResponse res = HttpRequest.get(httpUrl).getResponse();
        Assert.assertEquals(200, res.getStatusCode());
        Assert.assertEquals("Hello Kermit", res.getBody());
    }
}
