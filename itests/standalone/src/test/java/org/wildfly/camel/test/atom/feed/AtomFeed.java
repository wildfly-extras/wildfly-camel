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
package org.wildfly.camel.test.atom.feed;

import org.jboss.resteasy.plugins.providers.atom.Content;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.resteasy.plugins.providers.atom.Link;
import org.jboss.resteasy.plugins.providers.atom.Person;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

@Path("feed")
public class AtomFeed {
    @GET
    @Produces("application/atom+xml")
    public Feed getFeed() throws URISyntaxException {
        Feed feed = new Feed();
        feed.setId(new URI("http://test.com/1"));
        feed.setTitle("WildFly Camel Test Feed");
        feed.setUpdated(new Date());

        Link link = new Link();
        link.setHref(new URI("http://localhost"));
        link.setRel("edit");
        feed.getLinks().add(link);
        feed.getAuthors().add(new Person("WildFly Camel"));

        Entry entry = new Entry();
        entry.setTitle("Hello Kermit");

        Content content = new Content();
        content.setType(MediaType.TEXT_HTML_TYPE);
        content.setText("Greeting Kermit");
        entry.setContent(content);

        feed.getEntries().add(entry);
        return feed;
    }
}
