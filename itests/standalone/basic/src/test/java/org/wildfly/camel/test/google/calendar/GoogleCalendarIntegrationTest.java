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

package org.wildfly.camel.test.google.calendar;

import java.util.HashMap;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.calendar.GoogleCalendarComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.google.GoogleApiEnv;
import org.wildfly.extension.camel.CamelAware;

import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

/**
 * Read {@code google-api-testing.adoc} in the rood directory of the current Maven module to learn how to set up the credentials used by this class.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
public class GoogleCalendarIntegrationTest {
    private static final Logger log = Logger.getLogger(GoogleCalendarIntegrationTest.class);
    // userid of the currently authenticated user
    public static final String CURRENT_USERID = "me";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-google-calendar-tests.jar").addClass(GoogleApiEnv.class);
    }

    private static Calendar createTestCalendar(ProducerTemplate template, String testId) {
        Calendar calendar = new Calendar();

        calendar.setSummary(testId + UUID.randomUUID().toString());
        calendar.setTimeZone("America/St_Johns");

        return template.requestBody("google-calendar://calendars/insert?inBody=content", calendar, Calendar.class);
    }

    @SuppressWarnings("serial")
    @Test
    public void events() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        GoogleCalendarComponent gCalendarComponent = camelctx.getComponent("google-calendar", GoogleCalendarComponent.class);
        GoogleApiEnv.configure(gCalendarComponent.getConfiguration(), getClass(), log);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String pathPrefix = "events";

                // test route for calendarImport
                from("direct://CALENDARIMPORT").to("google-calendar://" + pathPrefix + "/calendarImport");

                // test route for delete
                from("direct://DELETE").to("google-calendar://" + pathPrefix + "/delete");

                // test route for get
                from("direct://GET").to("google-calendar://" + pathPrefix + "/get");

                // test route for insert
                from("direct://INSERT").to("google-calendar://" + pathPrefix + "/insert");

                // test route for instances
                from("direct://INSTANCES").to("google-calendar://" + pathPrefix + "/instances");

                // test route for list
                from("direct://LIST").to("google-calendar://" + pathPrefix + "/list?inBody=calendarId");

                // test route for move
                from("direct://MOVE").to("google-calendar://" + pathPrefix + "/move");

                // test route for patch
                from("direct://PATCH").to("google-calendar://" + pathPrefix + "/patch");

                // test route for quickAdd
                from("direct://QUICKADD").to("google-calendar://" + pathPrefix + "/quickAdd");

                // test route for update
                from("direct://UPDATE").to("google-calendar://" + pathPrefix + "/update");

                // test route for watch
                from("direct://WATCH").to("google-calendar://" + pathPrefix + "/watch");
            }
        });

        try {
            camelctx.start();

            String testId = getClass().getSimpleName() + ".events";

            ProducerTemplate template = camelctx.createProducerTemplate();

            final Calendar cal = createTestCalendar(template, testId);
            log.infof("Created test calendar %s", cal.getSummary());

            final String eventText = testId + " feed the camel";

            // Add an event
            final Event quickAddEvent = template.requestBodyAndHeaders("direct://QUICKADD", null, new HashMap<String, Object>() {{
                // parameter type is String
                put("CamelGoogleCalendar.calendarId", cal.getId());
                // parameter type is String
                put("CamelGoogleCalendar.text", eventText);
                }}, Event.class);
            Assert.assertNotNull("quickAdd result", quickAddEvent);

            // Check if it is in the list of events for this calendar
            final Events events = template.requestBody("direct://LIST", cal.getId(), Events.class);
            Event item = events.getItems().get(0);
            String eventId = item.getId();
            Assert.assertEquals(eventText, item.getSummary());

            // Get the event metadata
            final Event completeEvent = template.requestBodyAndHeaders("direct://GET", null, new HashMap<String, Object>() {{
                // parameter type is String
                put("CamelGoogleCalendar.calendarId", cal.getId());
                // parameter type is String
                put("CamelGoogleCalendar.eventId", eventId);
                }}, Event.class);
            Assert.assertEquals(eventText, quickAddEvent.getSummary());

            // Change the event
            completeEvent.setSummary(testId + " feed the camel later");
            // parameter type is com.google.api.services.calendar.model.Event
            Event newResult = template.requestBodyAndHeaders("direct://UPDATE", null, new HashMap<String, Object>() {{
                // parameter type is String
                put("CamelGoogleCalendar.calendarId", cal.getId());
                // parameter type is String
                put("CamelGoogleCalendar.eventId", eventId);
                put("CamelGoogleCalendar.content", completeEvent);
                }}, Event.class);
            Assert.assertEquals("Feed the Camel later", newResult.getSummary());

            // Delete the event
            template.requestBodyAndHeaders("direct://DELETE", null, new HashMap<String, Object>() {{
                // parameter type is String
                put("CamelGoogleCalendar.calendarId", cal.getId());
                // parameter type is String
                put("CamelGoogleCalendar.eventId", eventId);
                }}, Event.class);

            // Check if it is NOT in the list of events for this calendar
            Events eventsAfterDeletion = template.requestBody("direct://LIST", cal.getId(), Events.class);
            Assert.assertEquals(0, events.getItems().size());

        } finally {
            camelctx.stop();
        }

    }

}
