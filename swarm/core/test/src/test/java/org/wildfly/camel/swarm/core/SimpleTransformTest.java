/**
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.swarm.core;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.swarm.core.subA.RouteBuilderA;
import org.wildfly.swarm.ContainerFactory;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.container.JARArchive;


/**
 * @author thomas.diesler@jboss.com
 * @since 09-Feb-2016
 */
@RunWith(Arquillian.class)
public class SimpleTransformTest implements ContainerFactory {

    @Deployment(testable = false)
    public static JARArchive swarmDeployment() {
        JARArchive archive = ShrinkWrap.create(JARArchive.class);
        archive.addAsResource("spring/simple-camel-context.xml");
        archive.addClasses(RouteBuilderA.class);
        return archive;
    }

    @Override
    public Container newContainer(String... args) throws Exception {
        return new Container().fraction(new CamelCoreFraction());
    }

    @Test
    public void testSimpleTransform() throws Exception {

        Path path = Paths.get(System.getProperty("java.io.tmpdir"), RouteBuilderA.class.getName());
        final String fileUrl = "file://" + path + "?fileName=fileA&doneFileName=fileA.done";

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(fileUrl).convertBodyTo(String.class).to("seda:end");
            }
        });

        camelctx.start();
        try {
            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            String result = consumer.receiveBody("seda:end", String.class);
            Assert.assertEquals("Hello 1", result);
        } finally {
            camelctx.stop();
        }
    }

    public JARArchive camelDeployment() {
        JARArchive archive = ShrinkWrap.create(JARArchive.class);
        archive.addAsResource("spring/simple-camel-context.xml");
        return archive;
    }
}
