# Hawtio console

After wildfly-camel is built, let's run the server inside `itests/standalone/smoke/target/wildfly-9.0.0.CR1`

Let's add `admin/admin` Management User without any special roles:

```
cd itests/standalone/smoke/target/wildfly-9.0.0.CR1
$ bin/add-user.sh

What type of user do you wish to add?
 a) Management User (mgmt-users.properties)
 b) Application User (application-users.properties)
(a): a

Enter the details of the new user to add.
Using realm 'ManagementRealm' as discovered from the existing property files.
Username : admin
The username 'admin' is easy to guess
Are you sure you want to add user 'admin' yes/no? yes
Password recommendations are listed below. To modify these restrictions edit the add-user.properties configuration file.
 - The password should be different from the username
 - The password should not be one of the following restricted values {root, admin, administrator}
 - The password should contain at least 8 characters, 1 alphabetic character(s), 1 digit(s), 1 non-alphanumeric symbol(s)
Password :
Re-enter Password :
What groups do you want this user to belong to? (Please enter a comma separated list, or leave blank for none)[  ]:
About to add user 'admin' for realm 'ManagementRealm'
Is this correct yes/no? yes
Added user 'admin' to file '/data/ggrzybek/sources/github.com/jboss-fuse/wildfly-camel/itests/standalone/smoke/target/wildfly-9.0.0.CR1/standalone/configuration/mgmt-users.properties'
Added user 'admin' to file '/data/ggrzybek/sources/github.com/jboss-fuse/wildfly-camel/itests/standalone/smoke/target/wildfly-9.0.0.CR1/domain/configuration/mgmt-users.properties'
Added user 'admin' with groups  to file '/data/ggrzybek/sources/github.com/jboss-fuse/wildfly-camel/itests/standalone/smoke/target/wildfly-9.0.0.CR1/standalone/configuration/mgmt-groups.properties'
Added user 'admin' with groups  to file '/data/ggrzybek/sources/github.com/jboss-fuse/wildfly-camel/itests/standalone/smoke/target/wildfly-9.0.0.CR1/domain/configuration/mgmt-groups.properties'
Is this new user going to be used for one AS process to connect to another AS process?
e.g. for a slave host controller connecting to the master or for a Remoting connection for server to server EJB calls.
yes/no? no
```

Let's check if there's hawtio role configured (in `itests/standalone/smoke/target/wildfly-9.0.0.CR1/standalone/configuration/standalone.xml`)

```xml
<system-properties>
    <property name="hawtio.authenticationEnabled" value="true" />
    <property name="hawtio.offline" value="true" />
    <property name="hawtio.realm" value="hawtio-domain" />
</system-properties>
```

If there's `hawtio.role=admin`, please remove it. Otherwise the user created earlier won't be able to log in (having no special roles configured)

Let's start WF:

    bin/standalone.sh -c standalone.xml

Without any changes, this will start WF-Camel without default route deployed, there's:

```xml
<subsystem xmlns="urn:jboss:domain:camel:1.0">
    <!-- You can add static camelContext definitions here. -->
    <!--
       <camelContext id="system-context-1">
         <![CDATA[
         <route>
           <from uri="direct:start"/>
           <transform>
             <simple>Hello #{body}</simple>
           </transform>
         </route>
         ]]>
       </camelContext>
    -->
</subsystem>
```

(example camel context is commented out)

We should be able to see this in console:

```
11:16:41,150 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-3) WFLYSRV0027: Starting deployment of "hawtio-wildfly-1.4.50.war" (runtime-name: "hawtio-wildfly-1.4.50.war")
...
11:16:43,414 INFO  [io.hawt.system.ConfigManager] (MSC service thread 1-3) Configuration will be discovered via JNDI
11:16:43,424 INFO  [io.hawt.jmx.JmxTreeWatcher] (MSC service thread 1-3) Welcome to hawtio 1.4.50 : http://hawt.io/ : Don't cha wish your console was hawt like me? ;-)
11:16:43,428 INFO  [io.hawt.jmx.UploadManager] (MSC service thread 1-3) Using file upload directory: /tmp/uploads
11:16:45,369 INFO  [io.undertow.servlet] (MSC service thread 1-3) jolokia-agent: Using access restrictor classpath:/jolokia-access.xml
11:16:46,180 INFO  [org.wildfly.extension.undertow] (MSC service thread 1-3) WFLYUT0021: Registered web context: /hawtio
11:16:46,234 INFO  [org.jboss.as.server] (ServerService Thread Pool -- 35) WFLYSRV0010: Deployed "hawtio-wildfly-1.4.50.war" (runtime-name : "hawtio-wildfly-1.4.50.war")
...
```

Browse to http://localhost:8080/hawtio, login screen will be opened. We will see this in the logs:

```
11:18:26,950 INFO  [io.hawt.web.AuthenticationFilter] (default task-1) Starting hawtio authentication filter, JAAS realm: "hawtio-domain" authorized role(s): "*" role principal classes: "org.jboss.security.SimplePrincipal"
```

Which means hawtio JAAS authentication was successfully integrated with WF security. We should be able to log in using
`admin` user with the password specified for `addUser.sh` script.

Hawtio should not present any "Camel" tabs, as we have no camel context deployed. There should be:

* Connect
* Dashoard
* JBoss
* JMX
* Threads

tabs. Under JBoss→Applications tab we can see `hawtio-wildfly-1.4.x.war` application deployed under `/hawtio` context.

Let's deploy some camel contexts. Drop:

* `itests/standalone/smoke/src/main/resources/spring/transform1-camel-context.xml `
* `itests/standalone/smoke/src/main/resources/spring/transform2-camel-context.xml `

into `itests/standalone/smoke/target/wildfly-9.0.0.CR1/standalone/deployments` without restarting anything.
We should see new "Camel" tab in hawtio.

This tab presents subset of JMX tree - objects in `org.apache.camel` domain.

1. From the *Camel Contexts* selection we can start/pause/destroy contexts
2. From *transform1* node we can see JMX properties and operations of context. *Charts* show changes of attribute values over time
3. When you click *transform1/Routes* or *transform1/Routes/route1*, we should see source view of camel route(s), we can modify it and update.
   This should be visible in logs (route restart):
```
11:23:41,228 INFO  [org.apache.camel.impl.DefaultShutdownStrategy] (default task-27) Starting to graceful shutdown 1 routes (timeout 300 seconds)
11:23:41,233 INFO  [org.apache.camel.impl.DefaultShutdownStrategy] (Camel (transform1) thread #0 - ShutdownTask) Route: route2 shutdown complete, was consuming from: Endpoint[direct://start]
11:23:41,234 INFO  [org.apache.camel.impl.DefaultShutdownStrategy] (default task-27) Graceful shutdown of 1 routes completed in 0 seconds
11:23:41,235 INFO  [org.apache.camel.spring.SpringCamelContext] (default task-27) Route: route2 is stopped, was consuming from: Endpoint[direct://start]
11:23:41,236 INFO  [org.apache.camel.spring.SpringCamelContext] (default task-27) Route: route2 is shutdown and removed, was consuming from: Endpoint[direct://start]
11:23:41,257 INFO  [org.apache.camel.spring.SpringCamelContext] (default task-27) Route: route2 started and consuming from: Endpoint[direct://start]
```
4. e.g., I changed `<simple>Hello ${body}</simple>` to `<simple>Hello ${body}!</simple>`, this change was reflected on both
   the diagram and a list of properties of *transform* EIP block

Now we can try restarting WF after uncommenting default camel context inside `<subsystem xmlns="urn:jboss:domain:camel:1.0">`
in `itests/standalone/smoke/target/wildfly-9.0.0.CR1/standalone/configuration/standalone.xml`.

