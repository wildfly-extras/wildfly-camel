# camel-mail example

This example demonstrates using the camel-email component with WildFly Camel susbsystem to send and receive email.

The example uses [Greenmail](http://www.icegreen.com/greenmail/) to configure a local mail server on your machine. This eliminates the need to
use external mail services. The configuration for the WildFly mail subsystem can be found within CLI scripts at `src/main/resources/cli`.

The mail session used by this example is bound to JNDI at `java:jboss/mail/greenmail`. Server entries are configured for SMTP and POP3 protocols.

```xml
<mail-session name="greenmail" jndi-name="java:jboss/mail/greenmail">
    <smtp-server outbound-socket-binding-ref="mail-greenmail-smtp" username="user1" password="password"/>
    <pop3-server outbound-socket-binding-ref="mail-greenmail-pop3" username="user2" password="password2"/>
</mail-session>
```

There are also some custom socket bindings to ensure that the mail session can connect to the ports exposed by Greenmail.

```xml
<!-- GreenMail SMTP Socket Binding -->
<outbound-socket-binding name="mail-greenmail-smtp">
    <remote-destination host="localhost" port="10025"/>
</outbound-socket-binding>

<!-- GreenMail POP3 Socket Binding -->
<outbound-socket-binding name="mail-greenmail-pop3">
    <remote-destination host="localhost" port="10110"/>
</outbound-socket-binding>
```

The Greenmail `mail-session` is injected into the `MailRouteBuilder` class using CDI. The mail session is then used to configure two Camel endpoints. One to send email
and another to receieve email.
```java
MailEndpoint sendMailEndpoint = (MailEndpoint) getContext().getEndpoint("smtp://localhost");
configureMailEndpoint(sendMailEndpoint);

MailEndpoint receiveMailEndpoint = (MailEndpoint) getContext().getEndpoint("pop3://user2@localhost?consumer.delay=30000");
configureMailEndpoint(receiveMailEndpoint);

from("direct:sendmail")
    .to(sendMailEndpoint);

from(receiveMailEndpoint)
    .to("log:emails?showAll=true");
```

## Running the example

To run the example.

1. Change into the `camel-mail` directory
2. Build the project `mvn clean install` 
3. Deploy the web application `mvn wildfly:run`
4. When the WildFly server has started, browse to `http://localhost:8080/example-camel-mail/`

You should see a form from which you can test sending emails with Camel.

## Testing Camel email
Enter a 'from' address, subject and email message body and click the 'send button'. Note that the pop3 mail endpoint was configured
to retrieve mail from the mailbox of 'user2@localhost'. Therefore the web UI is hard coded to route mail to this address.

The form details are posted to a servlet defined within the MailSendServlet class. This servlet forwards the data entered on the web form to the Camel
`direct:sendmail` endpoint. This triggers an email to be sent to the local Greenmail SMTP sevrer.

The pop3 endpoint checks for email from the local Greenmail mail server every 30 seconds. If you watch the console output you should see that the email you sent
is eventually reported by the Camel log endpoint. The output will look something like this.

    10:57:05,319 INFO  [emails] (Camel (mail-camel-context) thread #0 - pop3://user2@localhost) Exchange[
    , Id: ID-localhost-localdomain-60411-1424775393775-1-4
    , ExchangePattern: InOnly
    , Properties: {CamelBatchComplete=true, CamelBatchIndex=0, CamelBatchSize=1, CamelBinding=org.apache.camel.component.mail.MailBinding@1667d15e, CamelCreatedTimestamp=Tue Feb 24 10:57:05 GMT 2015, CamelMessageHistory=[DefaultMessageHistory[routeId=route2, node=to2]], CamelPop3Uid=a66b2985-23c1-3b85-a967-18a2de4e9a93, CamelToEndpoint=log://emails?multiline=true&showAll=true}
    , Headers: {breadcrumbId=ID-localhost-localdomain-60411-1424775393775-1-1, Content-Transfer-Encoding=7bit, Content-Type=text/plain, Date=Tue, 24 Feb 2015 10:56:41 +0000 (GMT), From=test@localhost, message=Hello World!, Message-ID=<1126195401.0.1424775401210.JavaMail.user1@localhost>, MIME-Version=1.0, Received=from 127.0.0.1 (HELO localhost.localdomain); Tue Feb 24 10:56:41 GMT 2015, Return-Path=<test@localhost>, Subject=Hello from camel, To=user2@localhost}
    , BodyType: String
    , Body: Hello World!
    , Out: null:
