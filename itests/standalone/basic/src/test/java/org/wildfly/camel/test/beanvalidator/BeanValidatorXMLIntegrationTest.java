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
package org.wildfly.camel.test.beanvalidator;


import java.util.Set;

import javax.validation.ConstraintViolation;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.validator.BeanValidationException;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.beanvalidator.subA.CarWithoutAnnotations;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class BeanValidatorXMLIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bean-validator-xml-tests");
        archive.addPackage(CarWithoutAnnotations.class.getPackage());
        archive.addAsManifestResource("beanvalidator/validation.xml", "validation.xml");
        archive.addAsResource("beanvalidator/constraints-car.xml", "constraints-car.xml");
        return archive;
    }

    @Test
    public void testBeanValidationSuccess() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("bean-validator://validate");
            }
        });

        camelctx.start();

        try {
            CarWithoutAnnotations car = new CarWithoutAnnotations("BMW", "DD-AB-123");

            ProducerTemplate template = camelctx.createProducerTemplate();
            CarWithoutAnnotations result = template.requestBody("direct:start", car, CarWithoutAnnotations.class);

            Assert.assertSame(car, result);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testBeanValidationFailure() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("bean-validator://validate");
            }
        });

        camelctx.start();

        try {
            CarWithoutAnnotations CarWithoutAnnotations = new CarWithoutAnnotations("BMW", null);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBody("direct:start", CarWithoutAnnotations);

            Assert.fail("Expected BeanValidationException to be thrown");
        } catch(CamelExecutionException e) {
            Assert.assertTrue(e.getExchange().getException() instanceof BeanValidationException);

            BeanValidationException bve = (BeanValidationException) e.getExchange().getException();
            Set<ConstraintViolation<Object>> constraintViolations = bve.getConstraintViolations();

            Assert.assertEquals(1, constraintViolations.size());
            ConstraintViolation<Object> constraintViolation = constraintViolations.iterator().next();
            Assert.assertEquals("licensePlate", constraintViolation.getPropertyPath().toString());
            Assert.assertEquals(null, constraintViolation.getInvalidValue());
            Assert.assertEquals("must not be null", constraintViolation.getMessage());
        } finally {
            camelctx.close();
        }
    }

}
