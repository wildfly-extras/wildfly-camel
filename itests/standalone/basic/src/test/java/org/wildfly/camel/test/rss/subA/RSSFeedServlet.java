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

package org.wildfly.camel.test.rss.subA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

@SuppressWarnings("serial")
@WebServlet(name = "RSSFeedServlet", urlPatterns = { "/*" }, loadOnStartup = 1)
public class RSSFeedServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle("WildFly-Camel Test RSS Feed");
        feed.setLink("http://localhost:8080/rss-tests");
        feed.setDescription("Test RSS feed for the camel-rss component");

        List<SyndEntry> entries = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            entries.add(createFeedEntry("Test entry: ", "Test content: ", i));
        }
        feed.setEntries(entries);

        SyndFeedOutput output = new SyndFeedOutput();
        try {
            output.output(feed, response.getWriter());
        } catch (FeedException e) {
            throw new IllegalStateException("Error generating RSS feed", e);
        }
    }

    private SyndEntry createFeedEntry(String title, String content, int index) {
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(title + index);
        entry.setLink("http://localhost:8080/rss-tests/" + index);
        entry.setPublishedDate(new Date());

        SyndContent description = new SyndContentImpl();
        description.setType("text/plain");
        description.setValue(content + index);
        entry.setDescription(description);
        return entry;
    }
}
