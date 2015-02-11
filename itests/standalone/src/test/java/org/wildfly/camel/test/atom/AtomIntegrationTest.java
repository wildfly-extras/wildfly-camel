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

package org.wildfly.camel.test.atom;

import org.apache.abdera.model.Entry;
import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.atom.feed.AtomFeed;
import org.wildfly.camel.test.atom.feed.FeedConstants;

@RunWith(Arquillian.class)
public class AtomIntegrationTest {

    @Deployment
    public static WebArchive createdeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "atom-test.war");
        archive.addAsWebInfResource(new StringAsset(""), "beans.xml");
        archive.addPackage(AtomFeed.class.getPackage());
        return archive;
    }

    @Test
    public void testConsumeAtomFeed() throws Exception {
                
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("atom://http://localhost:8080/atom-test/atom/feed?splitEntries=true")
                .to("direct:end");
            }
        });
        camelctx.start();

        PollingConsumer pollingConsumer = camelctx.getEndpoint("direct:end").createPollingConsumer();
        pollingConsumer.start();

        Entry result = pollingConsumer.receive().getIn().getBody(Entry.class);

        Assert.assertEquals(FeedConstants.ENTRY_TITLE, result.getTitle());
        Assert.assertEquals(FeedConstants.ENTRY_CONTENT, result.getContent());

        camelctx.stop();
    }
}
