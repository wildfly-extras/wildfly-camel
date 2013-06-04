/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.jndi;

import java.io.InputStream;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

import org.apache.camel.CamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.camel.CamelContextFactory;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.smoke.subA.HelloBean;

/**
 * Deploys a module which registers a {@link HelloBean} in JNDI, which is later used in a route.
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2013
 */
@RunWith(Arquillian.class)
public class JNDIIntegrationTestCase {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jndi-integration-tests");
        archive.addClasses(HelloBean.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.apache.camel,org.jboss.as.camel");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBeanTransform() throws Exception {
        CamelContext camelctx = new CamelContextFactory().createDefaultCamelContext(getClass().getClassLoader());
        InitialContext context = new InitialContext();
        NamingEnumeration<NameClassPair> list = context.list("/");
        while(list.hasMore()) {
            NameClassPair pair = list.next();
            System.out.println(pair);
        }
        //context.bind("helloBean", new HelloBean());
    }

}
