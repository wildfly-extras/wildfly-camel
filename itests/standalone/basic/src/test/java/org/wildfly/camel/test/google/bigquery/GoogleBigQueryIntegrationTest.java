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
package org.wildfly.camel.test.google.bigquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse;

import net.bytebuddy.ByteBuddy;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConfiguration;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConstants;
import org.apache.camel.component.google.bigquery.GoogleBigQueryEndpoint;
import org.apache.camel.component.google.bigquery.GoogleBigQueryProducer;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class GoogleBigQueryIntegrationTest {

    private static final String TEST_PROJECT_ID = "testProjectId";
    private static final String TEST_TABLE_ID = "testTableId";
    private static final String TEST_DATASET_ID = "testDatasetId";
    private GoogleBigQueryConfiguration configuration;
    private Bigquery bigquery;
    private GoogleBigQueryEndpoint endpoint;
    private Bigquery.Tabledata tabledata;
    private Bigquery.Tabledata.InsertAll mockInsertall;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-google-bigquery-tests.jar")
            .addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage(), ByteBuddy.class.getPackage());
    }

    @Before
    public void setUp() throws Exception {
        configuration = new GoogleBigQueryConfiguration();

        bigquery = Mockito.mock(Bigquery.class);
        endpoint = Mockito.mock(GoogleBigQueryEndpoint.class);
        tabledata = Mockito.mock(Bigquery.Tabledata.class);
        mockInsertall = Mockito.mock(Bigquery.Tabledata.InsertAll.class);

        Mockito.when(bigquery.tabledata()).thenReturn(tabledata);
        Mockito.when(tabledata.insertAll(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(mockInsertall);

        TableDataInsertAllResponse mockResponse = new TableDataInsertAllResponse();
        Mockito.when(mockInsertall.execute()).thenReturn(mockResponse);
    }

    @Test
    public void sendMessage() throws Exception {
        createProducer().process(createExchangeWithBody(new DefaultCamelContext(), new HashMap<>()));
        ArgumentCaptor<TableDataInsertAllRequest> dataCaptor = ArgumentCaptor.forClass(TableDataInsertAllRequest.class);
        Mockito.verify(tabledata).insertAll(Mockito.eq(TEST_PROJECT_ID), Mockito.eq(TEST_DATASET_ID), Mockito.eq(TEST_TABLE_ID), dataCaptor.capture());
        List<TableDataInsertAllRequest> requests = dataCaptor.getAllValues();
        Assert.assertEquals(1, requests.size());
        Assert.assertEquals(1, requests.get(0).getRows().size());
        Assert.assertNull(requests.get(0).getRows().get(0).getInsertId());
    }

    @Test
    public void sendMessageWithTableId() throws Exception {
        Exchange exchange = createExchangeWithBody(new DefaultCamelContext(), new HashMap<>());
        exchange.getIn().setHeader(GoogleBigQueryConstants.TABLE_ID, "exchange_table_id");
        createProducer().process(exchange);
        ArgumentCaptor<TableDataInsertAllRequest> dataCaptor = ArgumentCaptor.forClass(TableDataInsertAllRequest.class);
        Mockito.verify(tabledata).insertAll(Mockito.eq(TEST_PROJECT_ID), Mockito.eq(TEST_DATASET_ID), Mockito.eq("exchange_table_id"), dataCaptor.capture());
        List<TableDataInsertAllRequest> requests = dataCaptor.getAllValues();
        Assert.assertEquals(1, requests.size());
        Assert.assertEquals(1, requests.get(0).getRows().size());
        Assert.assertNull(requests.get(0).getRows().get(0).getInsertId());
    }

    @Test
    public void useAsInsertIdConfig() throws Exception {
        configuration.setUseAsInsertId("row1");
        Map<String, String> map = new HashMap<>();
        map.put("row1", "value1");
        createProducer().process(createExchangeWithBody(new DefaultCamelContext(), map));
        ArgumentCaptor<TableDataInsertAllRequest> dataCaptor = ArgumentCaptor.forClass(TableDataInsertAllRequest.class);
        Mockito.verify(tabledata).insertAll(Mockito.eq(TEST_PROJECT_ID), Mockito.eq(TEST_DATASET_ID), Mockito.eq(TEST_TABLE_ID), dataCaptor.capture());
        List<TableDataInsertAllRequest> requests = dataCaptor.getAllValues();
        Assert.assertEquals(1, requests.size());
        Assert.assertEquals(1, requests.get(0).getRows().size());
        Assert.assertEquals("value1", requests.get(0).getRows().get(0).getInsertId());
    }

    @Test
    public void listOfMessages() throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(new HashMap<>());
        messages.add(new HashMap<>());
        createProducer().process(createExchangeWithBody(new DefaultCamelContext(), messages));
        ArgumentCaptor<TableDataInsertAllRequest> dataCaptor = ArgumentCaptor.forClass(TableDataInsertAllRequest.class);
        Mockito.verify(tabledata).insertAll(Mockito.eq(TEST_PROJECT_ID), Mockito.eq(TEST_DATASET_ID), Mockito.eq(TEST_TABLE_ID), dataCaptor.capture());
        List<TableDataInsertAllRequest> requests = dataCaptor.getAllValues();
        Assert.assertEquals(1, requests.size());
        Assert.assertEquals(2, requests.get(0).getRows().size());
    }

    private GoogleBigQueryProducer createProducer() throws Exception {
        configuration.setProjectId(TEST_PROJECT_ID);
        configuration.setTableId(TEST_TABLE_ID);
        configuration.setDatasetId(TEST_DATASET_ID);
        return new GoogleBigQueryProducer(bigquery, endpoint, configuration);
    }

    private Exchange createExchangeWithBody(CamelContext camelContext, Object body) {
        Exchange exchange = new DefaultExchange(camelContext);
        Message message = exchange.getIn();
        message.setBody(body);
        return exchange;
    }
}
