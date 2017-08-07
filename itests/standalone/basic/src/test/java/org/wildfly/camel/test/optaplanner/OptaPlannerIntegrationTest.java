/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.optaplanner;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.persistence.CloudBalancingGenerator;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class OptaPlannerIntegrationTest  {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-optaplanner-tests");
        archive.addPackages(true, "org.optaplanner.examples.cloudbalancing");
        archive.addPackages(true, "org.optaplanner.examples.common");
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "com.google.guava,org.apache.commons.lang3");
            return builder.openStream();
        });
        archive.addAsResource("optaplanner/cloudBalancingScoreRules.drl");
        archive.addAsResource("optaplanner/solverConfig.xml");
        return archive;
    }

    @Test
    public void testSynchronousProblemSolving() throws Exception {
        
        CloudBalancingGenerator generator = new CloudBalancingGenerator(true);
        final CloudBalance planningProblem = generator.createCloudBalance(4, 12);
        Assert.assertNull(planningProblem.getScore());
        Assert.assertNull(planningProblem.getProcessList().get(0).getComputer());
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:in").
                to("optaplanner:optaplanner/solverConfig.xml");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            CloudBalance bestSolution = (CloudBalance) template.requestBody("direct:in", planningProblem);

            Assert.assertEquals(4, bestSolution.getComputerList().size());
            Assert.assertEquals(12, bestSolution.getProcessList().size());
            Assert.assertNotNull(bestSolution.getScore());
            Assert.assertTrue(bestSolution.getScore().isFeasible());
            Assert.assertNotNull(bestSolution.getProcessList().get(0).getComputer());
        } finally {
            camelctx.stop();
        }
    }
}
