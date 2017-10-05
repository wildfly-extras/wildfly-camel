/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.shiro;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.shiro.security.ShiroSecurityConstants;
import org.apache.camel.component.shiro.security.ShiroSecurityPolicy;
import org.apache.camel.component.shiro.security.ShiroSecurityToken;
import org.apache.camel.component.shiro.security.ShiroSecurityTokenInjector;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ShiroIntegrationTest {

    private byte[] passPhrase = {
        (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0B,
        (byte) 0x0C, (byte) 0x0D, (byte) 0x0E, (byte) 0x0F,
        (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13,
        (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17};    
    
    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-shiro-tests");
        archive.addAsResource("shiro/securityconfig.ini", "securityconfig.ini");
        return archive;
    }

    @Test
    public void testShiroAuthentication() throws Exception {
        
        ShiroSecurityPolicy securityPolicy = new ShiroSecurityPolicy("classpath:securityconfig.ini", passPhrase);
        
        ShiroSecurityToken goodToken = new ShiroSecurityToken("ringo", "starr");
        MySecurityTokenInjector goodInjector = new MySecurityTokenInjector(goodToken, passPhrase);
        
        ShiroSecurityToken badToken = new ShiroSecurityToken("ringo", "stirr");
        MySecurityTokenInjector badInjector = new MySecurityTokenInjector(badToken, passPhrase);
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            @SuppressWarnings("unchecked")
            public void configure() throws Exception {
                
                onException(IncorrectCredentialsException.class, AuthenticationException.class).
                    to("mock:authenticationException");

                from("direct:secureEndpoint").
                    policy(securityPolicy).
                    to("log:incoming payload").
                    to("mock:success");
            }
        });

        MockEndpoint successEndpoint = camelctx.getEndpoint("mock:success", MockEndpoint.class);
        MockEndpoint failureEndpoint = camelctx.getEndpoint("mock:authenticationException", MockEndpoint.class);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            successEndpoint.expectedMessageCount(1);
            failureEndpoint.expectedMessageCount(0);
            template.send("direct:secureEndpoint", goodInjector);
            
            successEndpoint.assertIsSatisfied();
            failureEndpoint.assertIsSatisfied();
            successEndpoint.reset();
            failureEndpoint.reset();
            
            successEndpoint.expectedMessageCount(0);
            failureEndpoint.expectedMessageCount(1);
            template.send("direct:secureEndpoint", badInjector);
            
            successEndpoint.assertIsSatisfied();
            failureEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    private static class MySecurityTokenInjector extends ShiroSecurityTokenInjector {

        MySecurityTokenInjector(ShiroSecurityToken shiroSecurityToken, byte[] bytes) {
            super(shiroSecurityToken, bytes);
        }
        
        public void process(Exchange exchange) throws Exception {
            exchange.getIn().setHeader(ShiroSecurityConstants.SHIRO_SECURITY_TOKEN, encrypt());
            exchange.getIn().setBody("Beatle Mania");
        }
    }
    
}
