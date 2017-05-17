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

package org.wildfly.camel.test.plain.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws.ec2.EC2Constants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.EC2Utils;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

public class EC2IntegrationTest {

    private static AmazonEC2Client ec2Client;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        ec2Client = EC2Utils.createEC2Client();
    }
    
    @Test
    public void testCreateInstance() throws Exception {

        Assume.assumeNotNull("AWS client not null", ec2Client);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("ec2Client", ec2Client);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        EC2Utils.addRoutes(camelctx);

        // Select the subnet
        Subnet subnet = null;
        for (Subnet aux : ec2Client.describeSubnets().getSubnets()) {
            System.out.println();
            if (aux.getState().equals("available") && aux.getAvailabilityZone().equals("eu-west-1a")) {
                subnet = aux;
            }
        }
        Assert.assertNotNull("Subnet not null", subnet);
        
        camelctx.start();
        try {

            // Create and run an instance
            Map<String, Object> headers = new HashMap<>();
            headers.put(EC2Constants.IMAGE_ID, "ami-02ace471");
            headers.put(EC2Constants.INSTANCE_TYPE, InstanceType.T2Micro);
            headers.put(EC2Constants.SUBNET_ID, subnet.getSubnetId());
            headers.put(EC2Constants.INSTANCE_MIN_COUNT, 1);
            headers.put(EC2Constants.INSTANCE_MAX_COUNT, 1);
            
            ProducerTemplate template = camelctx.createProducerTemplate();
            RunInstancesResult result1 = template.requestBodyAndHeaders("direct:createAndRun", null, headers, RunInstancesResult.class);
            String instanceId = result1.getReservation().getInstances().get(0).getInstanceId();
            System.out.println("InstanceId: " + instanceId);
            
            // Terminate the instance 
            headers = new HashMap<>();
            headers.put(EC2Constants.INSTANCES_IDS, Collections.singleton(instanceId));
            
            TerminateInstancesResult result2 = template.requestBodyAndHeaders("direct:terminate", null, headers, TerminateInstancesResult.class);
            Assert.assertEquals(instanceId, result2.getTerminatingInstances().get(0).getInstanceId());
        } finally {
            camelctx.stop();
        }
    }
}
