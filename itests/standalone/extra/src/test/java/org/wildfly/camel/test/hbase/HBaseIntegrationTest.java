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
package org.wildfly.camel.test.hbase;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hbase.HBaseAttribute;
import org.apache.camel.component.hbase.HBaseConstants;
import org.apache.camel.component.hbase.HBaseHelper;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ HBaseIntegrationTest.ServerSetup.class })
public class HBaseIntegrationTest {

    private static final String[][] COLUMNS = {
            {"id", "firstName", "lastName"},
            {"day", "month", "year"},
            {"street", "number", "zip"}
    };
    private String[][][] DATA = {
            {{"1", "Kermit", "The Frog"}, {"01", "01", "1960"}, {"Sesame Street", "1", "12345"}},
            {{"2", "Piggy", "Miss"}, {"02", "02", "1970"}, {"Sesame Street", "2", "56789"}},
            {{"3", "Bird", "Bif"}, {"03", "03", "1980"}, {"Sesame Street", "3", "112233"}}
    };
    private static final String[] KEYS = {"1", "2", "3"};

    private static String[] PERSON_SPEC = {"info", "dateOfBirth", "address"};
    private static byte[][] PERSON_DATA = {
        PERSON_SPEC[0].getBytes(),
        PERSON_SPEC[1].getBytes(),
        PERSON_SPEC[2].getBytes()
    };

    private static final String TABLE_PERSON = "person";

    static class ServerSetup implements ServerSetupTask {

        HBaseTestingUtility testUtil = new HBaseTestingUtility();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            if (!EnvironmentUtils.isWindows()) {
                testUtil.startMiniCluster(1);
                testUtil.createTable(HBaseHelper.getHBaseFieldAsBytes(TABLE_PERSON), PERSON_DATA);

                AvailablePortFinder.storeServerData("hbase-zk-port", testUtil.getZkCluster().getClientPortList().get(0));
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // [FIXME #1959] Embedded HBase cluster does not shut down cleanly
            // testUtil.shutdownMiniCluster();
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-hbase-tests.jar")
            .addClasses(AvailablePortFinder.class, EnvironmentUtils.class)
            .addAsResource("hbase/hbase-site.xml", "hbase-site.xml");
    }

    @Test
    public void testHBaseComponent() throws Exception {
        Assume.assumeFalse("[#1975] HBaseIntegrationTest fails on Windows", EnvironmentUtils.isWindows());

        System.setProperty("hbase.zk.clientPort", AvailablePortFinder.readServerData("hbase-zk-port"));

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("hbase://%s", TABLE_PERSON);

                fromF("hbase://%s", TABLE_PERSON)
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(3);

        Map<String, Object> headers = new HashMap<>();
        for (int row = 0; row < KEYS.length; row++) {
            headers.put(HBaseAttribute.HBASE_ROW_ID.asHeader(row + 1), KEYS[row]);
            headers.put(HBaseAttribute.HBASE_FAMILY.asHeader(row + 1), PERSON_SPEC[0]);
            headers.put(HBaseAttribute.HBASE_QUALIFIER.asHeader(row + 1), COLUMNS[0][0]);
            headers.put(HBaseAttribute.HBASE_VALUE.asHeader(row + 1), DATA[row][0][0]);
        }
        headers.put(HBaseConstants.OPERATION, HBaseConstants.PUT);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:start", null, headers);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
            System.clearProperty("hbase.zk.clientPort");
        }
    }
}
