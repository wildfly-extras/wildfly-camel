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
package org.wildfly.camel.test.hdfs;

import java.io.File;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class HdfsIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-hdfs-tests.jar");
		return archive;
    }

    @Test
    public void testHdfsConsumer() throws Exception {

    	File datadir = getHadoopDataDirectory();
		recursiveDelete(datadir);
		
        Path dataPath = new Path(datadir.getAbsolutePath());
        FileSystem fs = FileSystem.get(dataPath.toUri(), new Configuration());
        FSDataOutputStream out = fs.create(dataPath);
        for (int i = 0; i < 1024; ++i) {
            out.write(("PIPPO" + i).getBytes("UTF-8"));
            out.flush();
        }
        out.close();

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("hdfs:localhost/" + dataPath.toUri() + "?fileSystemType=LOCAL&chunkSize=4096&initialDelay=0")
                    	.to("mock:result");
                }
            });

            MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            resultEndpoint.expectedMessageCount(2);
            
            camelctx.start();

            resultEndpoint.assertIsSatisfied();
        }
    }
    
	private File getHadoopDataDirectory() {
		File dataDir = Paths.get(System.getProperty("jboss.server.data.dir"), "standalone", "data", "hadoop").toFile();
		return dataDir;
	}

    private boolean recursiveDelete(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                recursiveDelete(file);
            }
        }
        return directory.delete();
    }
}
