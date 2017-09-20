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

import org.junit.Assert;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Subnet;

public class EC2Utils {

    // Attach Policy: AmazonEC2FullAccess
    public static AmazonEC2Client createEC2Client() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonEC2Client client = !credentials.isValid() ? null : (AmazonEC2Client) 
                AmazonEC2ClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
    }

    public static String getSubnetId(AmazonEC2Client ec2Client) {
        Subnet subnet = null;
        for (Subnet aux : ec2Client.describeSubnets().getSubnets()) {
            System.out.println();
            if (aux.getState().equals("available") && aux.getAvailabilityZone().startsWith("eu-west-1")) {
                subnet = aux;
                break;
            }
        }
        Assert.assertNotNull("Subnet not null", subnet);
        return subnet.getSubnetId();
    }
}
