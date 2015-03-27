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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ScriptIntegrationTest {

    private static final String BEANSHELL_SCRIPT = "beanshell-script.bsh";
    private static final String GROOVY_SCRIPT = "groovy-script.grv";
    private static final String JAVA_SCRIPT = "java-script.js";
    private static final String PYTHON_SCRIPT = "python-script.py";
    private static final String RUBY_SCRIPT = "ruby-script.rb";

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "script-tests");
        archive.addAsResource("script/" + BEANSHELL_SCRIPT, BEANSHELL_SCRIPT);
        archive.addAsResource("script/" + GROOVY_SCRIPT, GROOVY_SCRIPT);
        archive.addAsResource("script/" + JAVA_SCRIPT, JAVA_SCRIPT);
        archive.addAsResource("script/" + PYTHON_SCRIPT, PYTHON_SCRIPT);
        archive.addAsResource("script/" + RUBY_SCRIPT, RUBY_SCRIPT);
        return archive;
    }

    @Test
    public void testBeanshell() throws Exception {
        scriptProcessing("beanshell", BEANSHELL_SCRIPT);
    }

    @Test
    public void testPhyton() throws Exception {
        scriptProcessing("python", PYTHON_SCRIPT);
    }

    @Test
    public void testRuby() throws Exception {
        scriptProcessing("ruby", RUBY_SCRIPT);
    }

    @Test
    public void testGroovy() throws Exception {
        scriptProcessing("groovy", GROOVY_SCRIPT);
    }

    @Test
    public void testJavaScrip() throws Exception {
        scriptProcessing("javaScript", JAVA_SCRIPT);
    }

    private void scriptProcessing(final String type, final String resource) throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").process(script(type, scriptSource(resource)));
            }});

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

    private String scriptSource(String resource) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream input = getClass().getResourceAsStream("/" + resource);
        IOUtils.copyStream(input, baos);
        return new String(baos.toByteArray());
    }
}
