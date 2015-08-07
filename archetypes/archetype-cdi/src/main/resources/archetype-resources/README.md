# ${artifactId}

This is a template Apache Camel CDI application for the WildFly-Camel subsystem. 

This project is setup to allow you to create a Apache Camel CDI application, which can be deployed to an application
server running the WildFly-Camel subsystem. An example CDI Camel Route has been created for you, together with an Arquillian
integration test.

## Prerequisites

* Minimum of Java 1.7
* Maven 3.2 or greater
* WildFly application server version ${version-wildfly}

## Getting started

1. Install WildFly-Camel subsystem distribution version ${version} on your application server

2. Conifgure a `$JBOSS_HOME` environment variable to point at your application server installation directory

3. Start the application server from the command line

For Linux:

`$JBOSS_HOME/bin/standalone.sh -c standalone-camel.xml`

For Windows:

`%JBOSS_HOME%\bin\standalone.bat -c standalone-camel.xml`

### Building the application

To build the application do:

`mvn clean install`

### Run Arquillian Tests
    
By default, tests are configured to be skipped as Arquillian requires the use of a container.

If you already have a running application server, you can run integration tests with:

`mvn clean test -Parq-remote`

Otherwise you can get Arquillian to start and stop the server for you (Note: you must have `JBOSS_HOME` configured beforehand):

`mvn clean test -Parq-managed`

### Deploying the application

To deploy the application to a running application server do:

`mvn clean package wildfly:deploy` 

### Undeploying the application

`mvn wildfly:undeploy`

## Further reading

* [WildFly-Camel documentation] (https://www.gitbook.com/book/wildflyext/wildfly-camel)
* [Apache Camel documentation] (http://camel.apache.org/)
