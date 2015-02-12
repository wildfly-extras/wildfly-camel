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

package org.wildfly.camel.test.script;

import static org.apache.camel.builder.script.ScriptBuilder.script;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class BeanShellIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "script-tests");
        return archive;
    }

    @Test
    public void testSendMatchingMessage() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(getRouteBuilder());
        camelctx.start();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBodyAndHeader("direct:start", "mybody", "foo", "bar", String.class);
        Assert.assertEquals("mybody", result);
    }

    @Test
    public void testSendNonMatchingMessage() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(getRouteBuilder());
        camelctx.start();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBodyAndHeader("direct:start", "mybody", "foo", "bad", String.class);
        Assert.assertEquals("mybody unmatched", result);
    }

	private RouteBuilder getRouteBuilder() {
		return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").choice()
                .when(script("beanshell", "request.getHeaders().get(\"foo\").equals(\"bar\")")).to("mock:result")
                .otherwise().transform(body().append(" unmatched")).to("mock:unmatched");
            }
        };
	}
}
