/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.influxdb;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.influxdb.InfluxDbConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class InfluxDBIntegrationTest {

    private static final String CONTAINER_INFLUX_DB = "influxdb";
    private static final String INFLUX_DB_BIND_NAME = "wfcInfluxDB";
    private static final String INFLUX_DB_NAME = "myTestTimeSeries";

    @ArquillianResource
    private CubeController cubeController;

    @ArquillianResource
    private InitialContext context;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-influxdb-tests.jar")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_INFLUX_DB);
        cubeController.start(CONTAINER_INFLUX_DB);

        InfluxDB influxDB = InfluxDBFactory.connect("http://" + TestUtils.getDockerHost() + ":8086", "admin", "admin");
        influxDB.createDatabase(INFLUX_DB_NAME);
        context.bind(INFLUX_DB_BIND_NAME, influxDB);
    }

    @After
    public void tearDown() throws Exception {
        try {
            context.unbind(INFLUX_DB_BIND_NAME);
        } catch (NamingException e) {
            // Ignore
        }
        cubeController.stop(CONTAINER_INFLUX_DB);
        cubeController.destroy(CONTAINER_INFLUX_DB);
    }

    @Test
    public void testInfluxDBProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("influxdb:%s?databaseName=%s&retentionPolicy=autogen", INFLUX_DB_BIND_NAME, INFLUX_DB_NAME);
            }
        });

        Map<String, Object> map = new HashMap<>();
        map.put(InfluxDbConstants.MEASUREMENT_NAME, "MyTestMeasurement");
        map.put("CPU", 1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", map, String.class);
            Assert.assertEquals("{CPU=1, camelInfluxDB.MeasurementName=MyTestMeasurement}", result);
        } finally {
            camelctx.stop();
        }
    }
}
