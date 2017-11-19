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
package org.wildfly.camel.test.flink;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.naming.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.flink.DataSetCallback;
import org.apache.camel.component.flink.FlinkConstants;
import org.apache.camel.component.flink.Flinks;
import org.apache.camel.component.flink.VoidDataSetCallback;
import org.apache.camel.component.flink.VoidDataStreamCallback;
import org.apache.camel.component.flink.annotations.AnnotatedDataSetCallback;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

@CamelAware
@RunWith(Arquillian.class)
public class FlinkIntegrationTest {

    final String flinkDataSetUri = "flink:dataSet?dataSet=#myDataSet";
    final String flinkDataStreamUri = "flink:dataStream?dataStream=#myDataStream";
    final long numberOfLinesInTestFile = 19;
    
    CamelContext camelctx;
    
    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-flink-tests.jar");
        archive.addAsResource("flink/flinkds.txt");
        return archive;
    }

    @Before
    public void before() throws Exception {
        String datadir = System.getProperty("jboss.server.data.dir");
        InputStream in = getClass().getClassLoader().getResourceAsStream("flink/flinkds.txt");
        try (OutputStream out = new FileOutputStream(datadir + "/flinkds.txt")) {
            IOUtils.copyStream(in, out);
        }
        camelctx = createCamelContext();
        camelctx.start();
    }

    @After
    public void after() throws Exception {
        if (camelctx != null)
            camelctx.stop();
    }
    
    @Test
    public void shouldExecuteDataSetCallback() {
        ProducerTemplate template = camelctx.createProducerTemplate();
        long linesCount = template.requestBodyAndHeader(flinkDataSetUri, null, FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, new DataSetCallback() {
            @Override
            public Object onDataSet(DataSet ds, Object... payloads) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    return ds.count();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        }, Long.class);

        Assert.assertEquals(numberOfLinesInTestFile, linesCount);
    }

    @Test
    public void shouldExecuteDataSetCallbackWithSinglePayload() {
        ProducerTemplate template = camelctx.createProducerTemplate();
        long linesCount = template.requestBodyAndHeader(flinkDataSetUri, 10, FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, new DataSetCallback() {
            @Override
            public Object onDataSet(DataSet ds, Object... payloads) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    return ds.count() * (int) payloads[0];
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        }, Long.class);

        Assert.assertEquals(numberOfLinesInTestFile * 10, linesCount);
    }

    @Test
    public void shouldExecuteDataSetCallbackWithPayloads() {
        ProducerTemplate template = camelctx.createProducerTemplate();
        long linesCount = template.requestBodyAndHeader(flinkDataSetUri, Arrays.<Integer>asList(10, 10), FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, new DataSetCallback() {
            @Override
            public Object onDataSet(DataSet ds, Object... payloads) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    return ds.count() * (int) payloads[0] * (int) payloads[1];
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        }, Long.class);

        Assert.assertEquals(numberOfLinesInTestFile * 100, linesCount);
    }

    @Test
    public void shouldUseTransformationFromRegistry() {
        ProducerTemplate template = camelctx.createProducerTemplate();
        Long linesCount = template.requestBody(flinkDataSetUri + "&dataSetCallback=#countLinesContaining", null, Long.class);
        Assert.assertTrue(linesCount > 0);
    }

    @Test
    public void shouldExecuteVoidCallback() throws IOException {
        final File output = File.createTempFile("camel", "flink");
        output.delete();

        ProducerTemplate template = camelctx.createProducerTemplate();
        template.sendBodyAndHeader(flinkDataSetUri, null, FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, new VoidDataSetCallback() {
            @Override
            public void doOnDataSet(DataSet ds, Object... payloads) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    ds.writeAsText(output.getAbsolutePath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        });

        Assert.assertTrue(output.length() >= 0);
    }

    @Test
    public void shouldExecuteAnnotatedCallback() {
        DataSetCallback dataSetCallback = new AnnotatedDataSetCallback(new Object() {
            @org.apache.camel.component.flink.annotations.DataSetCallback
            Long countLines(DataSet<String> textFile) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    return textFile.count();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        });

        ProducerTemplate template = camelctx.createProducerTemplate();
        long pomLinesCount = template.requestBodyAndHeader(flinkDataSetUri, null, FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, dataSetCallback, Long.class);

        Assert.assertEquals(numberOfLinesInTestFile, pomLinesCount);
    }

    @Test
    public void shouldExecuteAnnotatedVoidCallback() throws IOException {
        final File output = File.createTempFile("camel", "flink");
        output.delete();

        DataSetCallback dataSetCallback = new AnnotatedDataSetCallback(new Object() {
            @org.apache.camel.component.flink.annotations.DataSetCallback
            void countLines(DataSet<String> textFile) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    textFile.writeAsText(output.getAbsolutePath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        });

        ProducerTemplate template = camelctx.createProducerTemplate();
        template.sendBodyAndHeader(flinkDataSetUri, null, FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, dataSetCallback);

        Assert.assertTrue(output.length() >= 0);
    }

    @Test
    public void shouldExecuteAnnotatedCallbackWithParameters() {
        DataSetCallback dataSetCallback = new AnnotatedDataSetCallback(new Object() {
            @org.apache.camel.component.flink.annotations.DataSetCallback
            Long countLines(DataSet<String> textFile, int first, int second) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    return textFile.count() * first * second;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        });

        ProducerTemplate template = camelctx.createProducerTemplate();
        long pomLinesCount = template.requestBodyAndHeader(flinkDataSetUri, Arrays.<Integer>asList(10, 10), FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, dataSetCallback, Long.class);

        Assert.assertEquals(numberOfLinesInTestFile * 100, pomLinesCount);
    }

    @Test
    public void shouldExecuteVoidDataStreamCallback() throws IOException {
        final File output = File.createTempFile("camel", "flink");
        output.delete();

        ProducerTemplate template = camelctx.createProducerTemplate();
        template.sendBodyAndHeader(flinkDataStreamUri, null, FlinkConstants.FLINK_DATASTREAM_CALLBACK_HEADER, new VoidDataStreamCallback() {
            @Override
            public void doOnDataStream(DataStream ds, Object... payloads) throws Exception {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    ds.writeAsText(output.getAbsolutePath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        });

        Assert.assertTrue(output.length() >= 0);
    }

    private CamelContext createCamelContext() throws Exception {
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        Context jndictx = camelctx.getNamingContext();
        String datadir = System.getProperty("jboss.server.data.dir");
        ExecutionEnvironment executionEnvironment = Flinks.createExecutionEnvironment();
        StreamExecutionEnvironment streamExecutionEnvironment = Flinks.createStreamExecutionEnvironment();
        jndictx.rebind("myDataSet", executionEnvironment.readTextFile(datadir + "/flinkds.txt"));
        jndictx.rebind("myDataStream", streamExecutionEnvironment.readTextFile(datadir + "/flinkds.txt"));
        jndictx.rebind("countLinesContaining", new DataSetCallback() {
            @Override
            public Object onDataSet(DataSet ds, Object... payloads) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(DataSet.class.getClassLoader());
                    return ds.count();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        });
        return camelctx;
    }
    
}