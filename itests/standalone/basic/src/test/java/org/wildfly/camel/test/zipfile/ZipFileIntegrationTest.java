/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.zipfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.ZipFileDataFormat;
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
public class ZipFileIntegrationTest {

    private String datadir = System.getProperty("jboss.server.data.dir");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "zipfile-tests");
    }

    @Test
    public void testZipFileMarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .setHeader(Exchange.FILE_NAME, constant("test.txt"))
                        .marshal().zipFile()
                        .to("file://" + datadir + File.separator + "zip-marshal");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", "Hello Kermit");

            ZipFile zip = new ZipFile(datadir + "/zip-marshal" + "/test.txt.zip");

            Assert.assertNotNull(zip);
            Assert.assertTrue(zip.size() > 0);

            ZipEntry entry = zip.getEntry("test.txt");
            String zipFileContent = getZipFileContent(zip.getInputStream(entry));

            Assert.assertEquals("Hello Kermit", zipFileContent);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testZipFileUnmarshal() throws Exception {
        final ZipFileDataFormat zipFileDataFormat = new ZipFileDataFormat();
        zipFileDataFormat.setUsingIterator("true");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file://" + datadir + File.separator + "zip-unmarshal?initialDelay=500&antInclude=*.zip&noop=true")
                    .unmarshal(zipFileDataFormat)
                    .split(bodyAs(Iterator.class))
                    .streaming()
                    .convertBodyTo(String.class)
                    .to("seda:end");
            }
        });

        createZipFile("Hello Kermit");

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();

        camelctx.start();
        try {
            String result = pollingConsumer.receive(3000).getIn().getBody(String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testZipSplitterUnmarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file://" + datadir + File.separator + "zip-unmarshal?initialDelay=500&antInclude=*.zip&noop=true")
                    .split(new ZipSplitter())
                    .streaming()
                    .convertBodyTo(String.class)
                    .to("seda:end");
            }
        });

        createZipFile("Hello Kermit");

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();

        camelctx.start();
        try {
            String result = pollingConsumer.receive(3000).getIn().getBody(String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.close();
        }
    }

    private String getZipFileContent(InputStream inputStream) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    private void createZipFile(String content) throws IOException {
        String basePath = datadir + File.separator + "zip-unmarshal" + File.separator;
        File file = new File(basePath + "test.txt");
        file.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(file);
             FileOutputStream fos = new FileOutputStream(basePath + "test.zip");
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(basePath + "test.txt")) {

            fw.write(content);
            fw.close();

            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);

            int len;
            byte[] buffer = new byte[1024];

            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
        }
    }
}
