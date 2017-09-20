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
