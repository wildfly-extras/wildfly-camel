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
package org.wildfly.camel.test.common.aws;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.swf.SWFConstants;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClientBuilder;
import com.amazonaws.services.simpleworkflow.model.DomainInfo;
import com.amazonaws.services.simpleworkflow.model.ListDomainsRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;

public class SWFUtils {

    public static final String DOMAIN = "wfcdomain";

    // Attach Policy: SimpleWorkflowFullAccess
    public static AmazonSimpleWorkflowClient createWorkflowClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonSimpleWorkflowClient client = !credentials.isValid() ? null : (AmazonSimpleWorkflowClient) 
                AmazonSimpleWorkflowClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
    }

    public static void addRoutes(CamelContext camelctx) throws Exception {
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                String options = "amazonSWClient=#swfClient&domainName=" + SWFUtils.DOMAIN + "&activityList=swf-alist&workflowList=swf-wlist&version=1.0";
                
                from("aws-swf://activity?" + options + "&eventName=processActivities")
                    .log("FOUND ACTIVITY TASK ${body}")
                    .setBody(constant("1"))
                    .to("mock:worker");
                
                from("aws-swf://workflow?" + options + "&eventName=processWorkflows")
                    .log("FOUND WORKFLOW TASK ${body}").filter(header(SWFConstants.ACTION).isEqualTo(SWFConstants.EXECUTE_ACTION))
                    .to("aws-swf://activity?" + options + "&eventName=processActivities")
                    .setBody(constant("Message two"))
                    .to("aws-swf://activity?" + options + "&eventName=processActivities")
                    .log("SENT ACTIVITY TASK ${body}")
                    .to("mock:decider");

                from("direct:start")
                    .to("aws-swf://workflow?" + options + "&eventName=processWorkflows")
                    .log("SENT WORKFLOW TASK ${body}")
                    .to("mock:starter");
            }
        });
    }

    public static void registerDomain(AmazonSimpleWorkflowClient swfClient) {
        boolean registerDomain = true;
        ListDomainsRequest listreq = new ListDomainsRequest().withRegistrationStatus("REGISTERED");
        for (DomainInfo domain : swfClient.listDomains(listreq).getDomainInfos()) {
            registerDomain &= !DOMAIN.equals(domain.getName());
        }
        if (registerDomain) {
            RegisterDomainRequest domain = new RegisterDomainRequest()
                    .withWorkflowExecutionRetentionPeriodInDays("NONE")
                    .withName(DOMAIN);
            swfClient.registerDomain(domain);
        }
    }

    public static void terminateWorkflowExecution(AmazonSimpleWorkflowClient swfClient, String workflowId) {
        TerminateWorkflowExecutionRequest terminateReq = new TerminateWorkflowExecutionRequest()
                .withWorkflowId(workflowId)
                .withDomain(DOMAIN);
        swfClient.terminateWorkflowExecution(terminateReq);
    }
}
