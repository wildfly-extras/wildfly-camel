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

package org.wildfly.camel.test.google.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.mail.GoogleMailComponent;
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

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Profile;

/**
 * Read {@code google-api-testing.adoc} in the rood directory of the current Maven module to learn how to set up the credentials used by this class.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
public class GoogleMailIntegrationTest {
    private static final Logger log = Logger.getLogger(GoogleMailIntegrationTest.class);
    // userid of the currently authenticated user
    public static final String CURRENT_USERID = "me";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-google-mail-tests.jar").addClass(GoogleApiEnv.class);
    }

    private static Message createMessage(ProducerTemplate template, String subject)
            throws MessagingException, IOException {

        Profile profile = template.requestBody("google-mail://users/getProfile?inBody=userId", CURRENT_USERID,
                Profile.class);
        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage mm = new MimeMessage(session);
        mm.addRecipients(javax.mail.Message.RecipientType.TO, profile.getEmailAddress());
        mm.setSubject(subject);
        mm.setContent("Camel rocks!\n" //
                + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()) + "\n" //
                + "user: " + System.getProperty("user.name"), "text/plain");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mm.writeTo(baos);
        String encodedEmail = Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private static Message createThreadedMessage(String previousThreadId, String subject, ProducerTemplate template)
            throws MessagingException, IOException {
        Message message = createMessage(template, subject);
        if (previousThreadId != null) {
            message.setThreadId(previousThreadId);
        }

        Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", CURRENT_USERID);
        // parameter type is com.google.api.services.gmail.model.Message
        headers.put("CamelGoogleMail.content", message);

        return template.requestBodyAndHeaders("google-mail://messages/send", null, headers, Message.class);
    }

    private static Label findLabel(ListLabelsResponse labels, String label) {
        for (Label l : labels.getLabels()) {
            if (label.equals(l.getName())) {
                return l;
            }
        }
        return null;
    }

    private static boolean idInList(String testEmailId, ListMessagesResponse listOfMessages) {
        Assert.assertNotNull("list result", listOfMessages);
        List<Message> messages = listOfMessages.getMessages();
        if (messages != null) {
            for (Message m : listOfMessages.getMessages()) {
                if (testEmailId.equals(m.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void labels() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        GoogleMailComponent gMailComponent = camelctx.getComponent("google-mail", GoogleMailComponent.class);
        GoogleApiEnv.configure(gMailComponent.getConfiguration(), getClass(), log);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String pathPrefix = "labels";
                // test route for create
                from("direct://CREATE").to("google-mail://" + pathPrefix + "/create");

                // test route for delete
                from("direct://DELETE").to("google-mail://" + pathPrefix + "/delete");

                // test route for get
                from("direct://GET").to("google-mail://" + pathPrefix + "/get");

                // test route for list
                from("direct://LIST").to("google-mail://" + pathPrefix + "/list?inBody=userId");

                // test route for patch
                from("direct://PATCH").to("google-mail://" + pathPrefix + "/patch");

                // test route for update
                from("direct://UPDATE").to("google-mail://" + pathPrefix + "/update");
            }
        });

        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();

            // using String message body for single parameter "userId"
            ListLabelsResponse labels = template.requestBody("direct://LIST", CURRENT_USERID, ListLabelsResponse.class);

            final String testLabel = getClass().getSimpleName() + ".labels " + UUID.randomUUID().toString();

            String labelId = null;
            if (findLabel(labels, testLabel) == null) {
                Map<String, Object> headers = new HashMap<String, Object>();
                // parameter type is String
                headers.put("CamelGoogleMail.userId", CURRENT_USERID);
                Label label = new Label().setName(testLabel).setMessageListVisibility("show")
                        .setLabelListVisibility("labelShow");
                // parameter type is com.google.api.services.gmail.model.Label
                headers.put("CamelGoogleMail.content", label);

                Label result = template.requestBodyAndHeaders("direct://CREATE", null, headers, Label.class);

                Assert.assertNotNull("create result", result);
                labelId = result.getId();
            } else {
                labelId = findLabel(labels, testLabel).getId();
            }

            // using String message body for single parameter "userId"
            labels = template.requestBody("direct://LIST", CURRENT_USERID, ListLabelsResponse.class);
            Assert.assertTrue(findLabel(labels, testLabel) != null);

            Map<String, Object> headers = new HashMap<String, Object>();
            // parameter type is String
            headers.put("CamelGoogleMail.userId", CURRENT_USERID);
            // parameter type is String
            headers.put("CamelGoogleMail.id", labelId);

            template.requestBodyAndHeaders("direct://DELETE", null, headers);

            // using String message body for single parameter "userId"
            labels = template.requestBody("direct://LIST", CURRENT_USERID, ListLabelsResponse.class);
            Assert.assertTrue(findLabel(labels, testLabel) == null);

        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void messages() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        GoogleMailComponent gMailComponent = camelctx.getComponent("google-mail", GoogleMailComponent.class);
        GoogleApiEnv.configure(gMailComponent.getConfiguration(), getClass(), log);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                final String pathPrefix = "messages";

                // test route for attachments
                from("direct://ATTACHMENTS").to("google-mail://" + pathPrefix + "/attachments");

                // test route for delete
                from("direct://DELETE").to("google-mail://" + pathPrefix + "/delete");

                // test route for get
                from("direct://GET").to("google-mail://" + pathPrefix + "/get");

                // test route for gmailImport
                from("direct://GMAILIMPORT").to("google-mail://" + pathPrefix + "/gmailImport");

                // test route for gmailImport
                from("direct://GMAILIMPORT_1").to("google-mail://" + pathPrefix + "/gmailImport");

                // test route for insert
                from("direct://INSERT").to("google-mail://" + pathPrefix + "/insert");

                // test route for insert
                from("direct://INSERT_1").to("google-mail://" + pathPrefix + "/insert");

                // test route for list
                from("direct://LIST").to("google-mail://" + pathPrefix + "/list?inBody=userId");

                // test route for modify
                from("direct://MODIFY").to("google-mail://" + pathPrefix + "/modify");

                // test route for send
                from("direct://SEND").to("google-mail://" + pathPrefix + "/send");

                // test route for send
                from("direct://SEND_1").to("google-mail://" + pathPrefix + "/send");

                // test route for trash
                from("direct://TRASH").to("google-mail://" + pathPrefix + "/trash");

                // test route for untrash
                from("direct://UNTRASH").to("google-mail://" + pathPrefix + "/untrash");

            }
        });

        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            // ==== Send test email ====

            final String subject = getClass().getSimpleName() + ".messages " + UUID.randomUUID().toString();

            Message testEmail = createMessage(template, subject);
            Map<String, Object> headers = new HashMap<String, Object>();
            // parameter type is String
            headers.put("CamelGoogleMail.userId", CURRENT_USERID);
            // parameter type is com.google.api.services.gmail.model.Message
            headers.put("CamelGoogleMail.content", testEmail);

            Message result = template.requestBodyAndHeaders("direct://SEND", null, headers, Message.class);
            Assert.assertNotNull("send result", result);
            String testEmailId = result.getId();

            // ==== Search for message we just sent ====
            headers = new HashMap<String, Object>();
            headers.put("CamelGoogleMail.q", "subject:\"" + subject + "\"");
            // using String message body for single parameter "userId"
            ListMessagesResponse listOfMessages = template.requestBody("direct://LIST", CURRENT_USERID,
                    ListMessagesResponse.class);
            Assert.assertTrue(idInList(testEmailId, listOfMessages));

            // ===== trash it ====
            headers = new HashMap<String, Object>();
            // parameter type is String
            headers.put("CamelGoogleMail.userId", CURRENT_USERID);
            // parameter type is String
            headers.put("CamelGoogleMail.id", testEmailId);
            template.requestBodyAndHeaders("direct://TRASH", null, headers);

            // ==== Search for message we just trashed ====
            headers = new HashMap<String, Object>();
            headers.put("CamelGoogleMail.q", "subject:\"" + subject + "\"");
            // using String message body for single parameter "userId"
            listOfMessages = template.requestBody("direct://LIST", CURRENT_USERID, ListMessagesResponse.class);
            Assert.assertFalse(idInList(testEmailId, listOfMessages));

            // ===== untrash it ====
            headers = new HashMap<String, Object>();
            // parameter type is String
            headers.put("CamelGoogleMail.userId", CURRENT_USERID);
            // parameter type is String
            headers.put("CamelGoogleMail.id", testEmailId);
            template.requestBodyAndHeaders("direct://UNTRASH", null, headers);

            // ==== Search for message we just untrashed ====
            headers = new HashMap<String, Object>();
            headers.put("CamelGoogleMail.q", "subject:\"" + subject + "\"");
            // using String message body for single parameter "userId"
            listOfMessages = template.requestBody("direct://LIST", CURRENT_USERID, ListMessagesResponse.class);
            Assert.assertTrue(idInList(testEmailId, listOfMessages));

            // ===== permanently delete it ====
            headers = new HashMap<String, Object>();
            // parameter type is String
            headers.put("CamelGoogleMail.userId", CURRENT_USERID);
            // parameter type is String
            headers.put("CamelGoogleMail.id", testEmailId);
            template.requestBodyAndHeaders("direct://DELETE", null, headers);

            // ==== Search for message we just deleted ====
            headers = new HashMap<String, Object>();
            headers.put("CamelGoogleMail.q", "subject:\"" + subject + "\"");
            // using String message body for single parameter "userId"
            listOfMessages = template.requestBody("direct://LIST", CURRENT_USERID, ListMessagesResponse.class);
            Assert.assertFalse(idInList(testEmailId, listOfMessages));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void profile() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();

        GoogleMailComponent gMailComponent = camelctx.getComponent("google-mail", GoogleMailComponent.class);
        GoogleApiEnv.configure(gMailComponent.getConfiguration(), getClass(), log);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String pathPrefix = "users";
                // test route for attachments
                from("direct://GETPROFILE").to("google-mail://" + pathPrefix + "/getProfile?inBody=userId");
            }
        });

        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            // using String message body for single parameter "userId"
            final Profile result = template.requestBody("direct://GETPROFILE", CURRENT_USERID, Profile.class);

            Assert.assertNotNull("getProfile result", result);
            Assert.assertNotNull("Should be email address associated with current account", result.getEmailAddress());
            System.out.println("getProfile: " + result);
        } finally {
            camelctx.stop();
        }
    }

    @SuppressWarnings("serial")
    @Test
    public void threads() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        GoogleMailComponent gMailComponent = camelctx.getComponent("google-mail", GoogleMailComponent.class);
        GoogleApiEnv.configure(gMailComponent.getConfiguration(), getClass(), log);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String pathPrefix = "threads";
                ;
                // test route for delete
                from("direct://DELETE").to("google-mail://" + pathPrefix + "/delete");

                // test route for get
                from("direct://GET").to("google-mail://" + pathPrefix + "/get");

                // test route for list
                from("direct://LIST").to("google-mail://" + pathPrefix + "/list?inBody=userId");

                // test route for modify
                from("direct://MODIFY").to("google-mail://" + pathPrefix + "/modify");

                // test route for trash
                from("direct://TRASH").to("google-mail://" + pathPrefix + "/trash");

                // test route for untrash
                from("direct://UNTRASH").to("google-mail://" + pathPrefix + "/untrash");

            }
        });

        try {
            camelctx.start();

            final String subject = getClass().getSimpleName() + ".threads " + UUID.randomUUID().toString();

            ProducerTemplate template = camelctx.createProducerTemplate();
            Message m1 = createThreadedMessage(null, subject, template);
            final String threadId = m1.getThreadId();
            createThreadedMessage(threadId, subject, template);

            // using String message body for single parameter "userId"
            ListThreadsResponse result = template.requestBodyAndHeaders("direct://LIST", CURRENT_USERID,
                    Collections.singletonMap("CamelGoogleMail.q", "subject:\"" + subject + "\""),
                    ListThreadsResponse.class);

            Assert.assertNotNull("list result", result);
            Assert.assertTrue(result.getThreads().size() > 0);

            // ===== trash it ====
            template.requestBodyAndHeaders("direct://TRASH", null, new HashMap<String, Object>() {
                {
                    put("CamelGoogleMail.userId", CURRENT_USERID);
                    put("CamelGoogleMail.id", threadId);
                }
            });

            // ==== Search for message we just trashed ====
            result = template.requestBodyAndHeaders("direct://LIST", CURRENT_USERID,
                    Collections.singletonMap("CamelGoogleMail.q", "subject:\"" + subject + "\""),
                    ListThreadsResponse.class);
            Assert.assertNotNull("list result", result);
            Assert.assertTrue(result.getThreads() == null
                    || result.getThreads().stream().noneMatch(t -> threadId.equals(t.getId())));

            /* For some reason the thread deletion often needs some delay to succeed */
            int attemptCount = 0;
            for (;;) {
                try {
                    template.requestBodyAndHeaders("direct://DELETE", null, new HashMap<String, Object>() {
                        {
                            put("CamelGoogleMail.userId", CURRENT_USERID);
                            put("CamelGoogleMail.id", threadId);
                        }
                    });
                    break; /* success */
                } catch (Exception e) {
                    if (attemptCount >= 5) {
                        throw e; /* too many attempts */
                    } else {
                        /* retry */
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

        } finally {
            camelctx.stop();
        }

    }
}
