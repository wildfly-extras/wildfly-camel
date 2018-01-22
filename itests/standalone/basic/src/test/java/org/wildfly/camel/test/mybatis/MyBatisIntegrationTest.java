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
package org.wildfly.camel.test.mybatis;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.mybatis.MyBatisComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.mybatis.subA.Account;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class MyBatisIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-mybatis-tests");
        archive.addClasses(Account.class);
        archive.addAsResource("mybatis/SqlMapConfig.xml", "SqlMapConfig.xml");
        archive.addAsResource("mybatis/Account.xml");
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "com.h2database.h2");
            return builder.openStream();
        });
        return archive;
    }

    @Test
    public void testInsert() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("mybatis:insertAccount?statementType=Insert")
                .to("mock:result");
            }
        });

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);

        Account account = new Account();
        account.setId(444);
        account.setFirstName("Willem");
        account.setLastName("Jiang");
        account.setEmailAddress("Faraway@gmail.com");

        camelctx.start();
        try {
            createTable(camelctx);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", account);
            mock.assertIsSatisfied();

            Integer rows = producer.requestBody("mybatis:count?statementType=SelectOne", null, Integer.class);
            Assert.assertEquals(1, rows.intValue());
        } finally {
            dropTable(camelctx);
            camelctx.stop();
        }
    }

    private void createTable(CamelContext camelctx) throws SQLException, Exception {
        try (Connection connection = createConnection(camelctx)) {
            Statement statement = connection.createStatement();
            statement.execute(createStatement());
            connection.commit();
            statement.close();
        }
    }

    private void dropTable(CamelContext camelctx) throws SQLException, Exception {
        try (Connection connection = createConnection(camelctx)) {
            Statement statement = connection.createStatement();
            statement.execute("drop table ACCOUNT");
            connection.commit();
            statement.close();
        }
    }

    private String createStatement() {
        return "create table ACCOUNT (ACC_ID INTEGER, ACC_FIRST_NAME VARCHAR(255), ACC_LAST_NAME VARCHAR(255), ACC_EMAIL VARCHAR(255))";
    }

    private Connection createConnection(CamelContext camelctx) throws Exception {
        MyBatisComponent component = camelctx.getComponent("mybatis", MyBatisComponent.class);
        return component.getSqlSessionFactory().getConfiguration().getEnvironment().getDataSource().getConnection();
    }
}
