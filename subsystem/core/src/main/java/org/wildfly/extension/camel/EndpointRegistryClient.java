/*
 * #%L
 * Wildfly Camel :: Subsystem
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

package org.wildfly.extension.camel;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * The client for registered endpoints.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Jan-2016
 */
public class EndpointRegistryClient {

    private final ModelControllerClient controllerClient;

    public EndpointRegistryClient(ModelControllerClient controllerClient) {
        this.controllerClient = controllerClient;
    }

    public List<URL> getRegisteredEndpoints(String pathFilter) throws IOException {
        List<URL> result = new ArrayList<>();
        ModelNode resnode = controllerClient.execute(createOpNode("subsystem=camel", "read-attribute(name=endpoints)"));
        IllegalStateAssertion.assertEquals("success", resnode.get("outcome").asString(), "Cannot obtain endpoint URLs");
        for (ModelNode item : resnode.get("result").asList()) {
            if (pathFilter == null || item.asString().endsWith(pathFilter)) {
                result.add(new URL(item.asString()));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }

        Matcher matcher = Pattern.compile(".*\\((.*?)\\)").matcher(operation);
        if (matcher.matches()) {
            matcher.reset();
            op.get("operation").set(operation.substring(0, operation.indexOf('(')));
            while (matcher.find()) {
                String args = matcher.group(1);
                for (String argSegment : args.split(",")) {
                    String[] argElements = argSegment.split("=");
                    op.get(argElements[0].trim()).set(argElements[1].trim());
                }
            }
        } else {
            op.get("operation").set(operation);
        }
        return op;
    }
}
