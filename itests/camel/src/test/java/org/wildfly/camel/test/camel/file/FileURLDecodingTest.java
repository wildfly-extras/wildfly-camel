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

package org.wildfly.camel.test.camel.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Test;

public class FileURLDecodingTest {

    final String TARGET_DIR = "target/files";

    @Test
    public void testSimpleFile() throws Exception {
        assertTargetFile("data.txt", "data.txt");
    }

    @Test
    public void testFilePlus() throws Exception {
        assertTargetFile("data+.txt", "data .txt");
    }

    @Test
    public void testFileSpace() throws Exception {
        assertTargetFile("data%20.txt", "data .txt");
    }

    @Test
    public void testFileRawPlus() throws Exception {
        assertTargetFile("RAW(data+.txt)", "data+.txt");
    }

    private void assertTargetFile(final String encoded, final String expected) throws Exception, FileNotFoundException, IOException {
        File expectedFile = Paths.get(TARGET_DIR, expected).toFile();
        expectedFile.delete();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("file:" + TARGET_DIR + "?fileName=" + encoded);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Kermit", result);
        } finally {
            camelctx.stop();
        }

        BufferedReader br = new BufferedReader(new FileReader(expectedFile));
        Assert.assertEquals("Kermit", br.readLine());
        br.close();
    }

}
