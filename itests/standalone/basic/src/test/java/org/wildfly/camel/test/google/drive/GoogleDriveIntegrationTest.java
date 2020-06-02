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

package org.wildfly.camel.test.google.drive;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.drive.GoogleDriveComponent;
import org.apache.camel.component.google.drive.GoogleDriveConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.google.GoogleApiEnv;
import org.wildfly.extension.camel.CamelAware;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpContent;
import com.google.api.services.drive.model.File;

/**
 * Read {@code service-access.md} in the itests directory to learn how to set up credentials used by this class.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
public class GoogleDriveIntegrationTest {
    private static final Logger LOG = Logger.getLogger(GoogleDriveIntegrationTest.class);

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-google-drive-tests.jar")
            .addClass(GoogleApiEnv.class);
    }

	@Test
    @SuppressWarnings("serial")
    public void testGoogleDriveComponent() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            GoogleDriveConfiguration configuration = new GoogleDriveConfiguration();
			GoogleApiEnv.configure(configuration, getClass(), LOG);

            GoogleDriveComponent component = camelctx.getComponent("google-drive", GoogleDriveComponent.class);
            component.setConfiguration(configuration);
            
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    final String pathPrefix = "drive-files";
                    // test route for copy
                    from("direct://COPY")
                        .to("google-drive://" + pathPrefix + "/copy");

                    // test route for delete
                    from("direct://DELETE")
                        .to("google-drive://" + pathPrefix + "/delete?inBody=fileId");

                    // test route for get
                    from("direct://GET")
                        .to("google-drive://" + pathPrefix + "/get?inBody=fileId");

                    // test route for insert
                    from("direct://INSERT")
                        .to("google-drive://" + pathPrefix + "/insert?inBody=content");

                    // test route for insert
                    from("direct://INSERT_1")
                        .to("google-drive://" + pathPrefix + "/insert");

                    // test route for list
                    from("direct://LIST")
                        .to("google-drive://" + pathPrefix + "/list");

                    // test route for patch
                    from("direct://PATCH")
                        .to("google-drive://" + pathPrefix + "/patch");

                    // test route for touch
                    from("direct://TOUCH")
                        .to("google-drive://" + pathPrefix + "/touch?inBody=fileId");

                    // test route for trash
                    from("direct://TRASH")
                        .to("google-drive://" + pathPrefix + "/trash?inBody=fileId");

                    // test route for untrash
                    from("direct://UNTRASH")
                        .to("google-drive://" + pathPrefix + "/untrash?inBody=fileId");

                    // test route for update
                    from("direct://UPDATE")
                        .to("google-drive://" + pathPrefix + "/update");

                    // test route for update
                    from("direct://UPDATE_1")
                        .to("google-drive://" + pathPrefix + "/update");

                    // test route for watch
                    from("direct://WATCH")
                        .to("google-drive://" + pathPrefix + "/watch");

                }
            });
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();

            /* Create */
            final File originalFile = uploadTestFile(template, "files");
            /* Ensure it is there */
            Assert.assertNotNull(template.requestBody("direct://GET", originalFile.getId(), File.class));

            /* Copy */
            final File toFile = new File();
            toFile.setTitle(originalFile.getTitle() + "_copy");
            final File copyFile = template.requestBodyAndHeaders("direct://COPY", null, new HashMap<String, Object>(){{
                put("CamelGoogleDrive.fileId", originalFile.getId());
                // parameter type is com.google.api.services.drive.model.File
                put("CamelGoogleDrive.content", toFile);
                }}, File.class);

            Assert.assertNotNull("copy result", copyFile);
            Assert.assertEquals(toFile.getTitle(), copyFile.getTitle());
            /* Ensure the copy is there */
            Assert.assertNotNull(template.requestBody("direct://GET", copyFile.getId(), File.class));

            /* Delete */
            template.sendBody("direct://DELETE", originalFile.getId());
            /* Ensure it was deleted */
            try {
                template.requestBody("direct://GET", originalFile.getId(), File.class);
                Assert.fail("template.requestBody(\"direct://GET\", originalFile.getId(), File.class) should have thrown an exception");
            } catch (Exception expected) {
            }

            /* Delete */
            template.sendBody("direct://DELETE", copyFile.getId());
            /* Ensure it was deleted */
            try {
                template.requestBody("direct://GET", copyFile.getId(), File.class);
                Assert.fail("template.requestBody(\"direct://GET\", copyFile.getId(), File.class) should have thrown an exception");
            } catch (Exception expected) {
            	// ignore
            }
        }
    }

    private static File uploadTestFile(ProducerTemplate template, String testName) {
        File fileMetadata = new File();
        fileMetadata.setTitle(GoogleDriveIntegrationTest.class.getName()+"."+testName+"-"+ UUID.randomUUID().toString());
        final String content = "Camel rocks!\n" //
                + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()) + "\n" //
                + "user: " + System.getProperty("user.name");
        HttpContent mediaContent = new ByteArrayContent("text/plain", content.getBytes(StandardCharsets.UTF_8));

        final Map<String, Object> headers = new HashMap<>();
        // parameter type is com.google.api.services.drive.model.File
        headers.put("CamelGoogleDrive.content", fileMetadata);
        // parameter type is com.google.api.client.http.AbstractInputStreamContent
        headers.put("CamelGoogleDrive.mediaContent", mediaContent);

        return template.requestBodyAndHeaders("google-drive://drive-files/insert", null, headers, File.class);
    }
}
