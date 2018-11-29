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
package org.wildfly.camel.test.box;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.box.BoxComponent;
import org.apache.camel.component.box.BoxConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.IntrospectionSupport;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

import com.box.sdk.BoxFolder;

@CamelAware
@RunWith(Arquillian.class)
public class BoxIntegrationTest {

    // Enum values correspond to environment variable names
    private enum BoxOption {
        BOX_USERNAME("userName"),
        BOX_PASSWORD("userPassword"),
        BOX_CLIENT_ID("clientId"),
        BOX_CLIENT_SECRET("clientSecret");

        private String configPropertyName;

        BoxOption(String configPropertyName) {
            this.configPropertyName = configPropertyName;
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-box-tests.jar");
    }

    @Test
    public void testBoxComponent() throws Exception {
        Map<String, Object> boxOptions = createBoxOptions();

        // Do nothing if the required credentials are not present
        Assume.assumeTrue(boxOptions.size() == BoxOption.values().length);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").
                to("box://folders/createFolder");
            }
        });

        BoxConfiguration configuration = new BoxConfiguration();
        configuration.setAuthenticationType(BoxConfiguration.STANDARD_AUTHENTICATION);
        IntrospectionSupport.setProperties(configuration, boxOptions);

        BoxComponent component = new BoxComponent(camelctx);
        component.setConfiguration(configuration);
        camelctx.addComponent("box", component);

        BoxFolder folder = null;

        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelBox.parentFolderId", "0");
            headers.put("CamelBox.folderName", "TestFolder");

            ProducerTemplate template = camelctx.createProducerTemplate();
            folder = template.requestBodyAndHeaders("direct:start", null, headers, BoxFolder.class);

            Assert.assertNotNull("Folder was null", folder);
            Assert.assertEquals("Expected folder name to be TestFolder", "TestFolder", folder.getInfo().getName());
        } finally {
            // Clean up
            if (folder != null) {
                folder.delete(true);
            }

            camelctx.stop();
        }
    }

    private Map<String, Object> createBoxOptions() throws Exception {
        final Map<String, Object> options = new HashMap<>();

        for (BoxOption option : BoxOption.values()) {
            String envVar = System.getenv(option.name());
            if (envVar == null || envVar.length() == 0) {
                options.clear();
            } else {
                options.put(option.configPropertyName, envVar);
            }
        }

        return options;
    }
}
