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

import java.util.List;

import org.junit.Assert;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.AmazonSimpleDBClientBuilder;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;

public class SDBUtils {

    /* Attach a policy like this: MySDBFullAccess
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Sid": "Stmt1494415798000",
                        "Effect": "Allow",
                        "Action": [
                            "sdb:*"
                        ],
                        "Resource": [
                            "arn:aws:sdb:*"
                        ]
                    }
                ]
            }
     */

    public static AmazonSimpleDBClient createDBClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonSimpleDBClient client = !credentials.isValid() ? null : (AmazonSimpleDBClient)
                AmazonSimpleDBClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1").build();
        return client;
    }

    public static void createDomain(AmazonSimpleDBClient client, String domainName) throws InterruptedException {
        client.createDomain(new CreateDomainRequest(domainName));

        // Unfortunatly, there is no waiters for domain create

        int retries = 10;
        List<String> domainNames = client.listDomains().getDomainNames();
        while (!domainNames.contains(domainName) && 0 < retries--) {
            Thread.sleep(500);
            domainNames = client.listDomains().getDomainNames();
        }
        Assert.assertTrue(domainNames.contains(domainName));
    }

}
