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
package org.wildfly.camel.test.common.utils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DMRUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DMRUtils.class);

    private static Pattern HANDLER_PATTERN = Pattern.compile(".*handler=(.*)");

    public static ModelNode createOpNode(String address, String operation) {
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
                if (args.matches(".*\\{.*\\}.*")) {
                    ModelNode node = new ModelNode();
                    String[] argElements = args.split("(?!=>)=");
                    for (String argSegment : argElements[1].replaceAll("[{}]","").split(",")) {
                        String[] argParams = argSegment.split("=>");
                        node.get(argParams[0]).set(argParams[1]);
                    }

                    op.get(argElements[0].trim()).set(node);
                } else {
                    for (String argSegment : args.replaceAll("[{}\\[\\]]", "").split(",")) {
                        if (argSegment.startsWith("handlers")) {
                            Matcher handlerMatch = HANDLER_PATTERN.matcher(argSegment);
                            if (handlerMatch.find()) {
                                op.get("handlers").add(handlerMatch.group(1));
                            }
                        } else {
                            String[] argElements = argSegment.split("(?!=>)=");
                            String nodeName = argElements[0].trim();
                            if (argElements.length > 2) {
                                op.get(nodeName).set(argSegment.replace(nodeName + "=", ""));
                            } else if (nodeName.equals("handlers")) {
                                op.add(argElements[1].trim());
                            } else {
                                op.get(nodeName).set(argElements[1].trim());
                            }
                        }
                    }
                }
            }
        } else {
            op.get("operation").set(operation);
        }
        return op;
    }

    public static ModelNode createCompositeNode(ModelNode[] steps) {
        ModelNode comp = new ModelNode();
        comp.get("operation").set("composite");
        for (ModelNode step : steps) {
            comp.get("steps").add(step);
        }
        return comp;
    }

    public static BatchNodeBuilder batchNode() {
        return new BatchNodeBuilder();
    }

    public static ExecutionResult executeOperation(final ModelControllerClient client, ModelNode operationNode)
            throws IOException {
        ModelNode result = client.execute(new OperationBuilder(operationNode).build());
        return new ExecutionResult(result);
    }

    public static final class BatchNodeBuilder {
        private ModelNode batchNode;

        BatchNodeBuilder() {
            batchNode = new ModelNode();
            batchNode.get("operation").set("composite");
            batchNode.get("address").setEmptyList();
        }

        public BatchNodeBuilder addStep(String address, String operation) {
            batchNode.get("steps").add(createOpNode(address, operation));
            return this;
        }

        public BatchNodeBuilder addStep(ModelNode operation) {
            batchNode.get("steps").add(operation);
            return this;
        }

        public ExecutionResult execute(final ModelControllerClient client) throws IOException {
            return executeOperation(client, build());
        }

        public ModelNode build() {
            return batchNode;
        }
    }

    /**
     * Encapsulates a result of executing a CLI operation.
     */
    public static class ExecutionResult {
        private final ModelNode wrappedResult;

        ExecutionResult(ModelNode wrappedResult) {
            super();
            this.wrappedResult = wrappedResult;
        }

        /**
         * @return this {@link ExecutionResult}
         * @throws RuntimeException
         *             in case {@code outcome} is not {@code success}
         */
        public ExecutionResult assertSuccess() {
            if (wrappedResult.hasDefined("outcome") && "success".equals(wrappedResult.get("outcome").asString())) {
                if (wrappedResult.hasDefined("result")) {
                    LOG.trace(wrappedResult.get("result").toString());
                }
            } else if (wrappedResult.hasDefined("failure-description")) {
                throw new RuntimeException(wrappedResult.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + wrappedResult.get("outcome"));
            }
            return this;
        }

        /**
         * @return the {@link ModelNode} returned by the operation
         */
        public ModelNode getWrappedResult() {
            return wrappedResult;
        }

        /**
         * @return {@code result} subnode of {@link #wrappedResult} or {@code null} if {@link #wrappedResult} does not
         *         have a {@code result}
         */
        public ModelNode getUnwrappedResult() {
            if (wrappedResult.hasDefined("result")) {
                return wrappedResult.get("result");
            }
            return null;
        }
    }

}
