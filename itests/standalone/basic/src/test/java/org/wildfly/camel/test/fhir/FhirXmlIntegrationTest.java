/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.fhir;

import ca.uhn.fhir.context.FhirContext;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class FhirXmlIntegrationTest {

    private static final String PATIENT_XML = "<Patient xmlns=\"http://hl7.org/fhir\">"
        + "<name><family value=\"Holmes\"/><given value=\"Sherlock\"/></name>"
        + "<address><line value=\"221b Baker St, Marylebone, London NW1 6XE, UK\"/></address>"
        + "</Patient>";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-fhir-xml-tests.jar");
    }

    @Test
    public void testFhirJsonMarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal().fhirXml();
            }
        });

        camelctx.start();
        try {
            Patient patient = createPatient();
            ProducerTemplate template = camelctx.createProducerTemplate();
            InputStream inputStream = template.requestBody("direct:start", patient, InputStream.class);
            IBaseResource result = FhirContext.forDstu3().newXmlParser().parseResource(new InputStreamReader(inputStream));
            Assert.assertTrue("Expected marshaled patient to be equal", patient.equalsDeep((Base)result));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testFhirJsonUnmarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal().fhirXml();
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Patient result = template.requestBody("direct:start", PATIENT_XML, Patient.class);
            Assert.assertTrue("Expected unmarshaled patient to be equal", result.equalsDeep(createPatient()));
        } finally {
            camelctx.stop();
        }
    }

    private Patient createPatient() {
        Patient patient = new Patient();
        patient.addName(new HumanName()
            .addGiven("Sherlock")
            .setFamily("Holmes"))
            .addAddress(new Address().addLine("221b Baker St, Marylebone, London NW1 6XE, UK"));
        return patient;
    }
}
