Camel JPA Spring example
------------------------

This example demonstrates using the camel-jpa component with Spring and the WildFly Camel susbsystem to persist entities to an in-memory database.

In this example, a Camel route consumes XML files from ${JBOSS_JOME}/standalone/data/customers. Camel then uses JAXB to
unmarshal the data to a Customer entity. This entity is then passed to a jpa endpoint and is persisted to a 'customer' database
table.

Prerequisites
-------------

* Maven
* An application server with the wildfly-camel subsystem installed

Running the example
-------------------

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`
3. Browse to http://localhost:8080/example-camel-jpa-spring/customers

Testing Camel JPA Spring
------------------------

There are some example customer XML files within the `src/main/resources/customers` directory. To make Camel
consume them and use JPA to persist the data to an in-memory database, simply copy them to the customers input
directory.

For Linux / Mac users:

    cp src/main/resources/customers/*.xml ${JBOSS_HOME}/standalone/data/customers/

For Windows users:

    copy src\main\resources\customers\*.xml %JBOSS_HOME%/standalone\data\customers\

The console will output messages detailing what happened to each of the orders. The output
will look something like this.

```
2:09:39,385 INFO  [input] (Camel (camel-jpa-context) thread #5 - file:///wildfly/standalone/data/customers) Exchange[Id: ID-localhost-localdomain-35597-1438166553663-11-4, ExchangePattern: InOnly, Properties: {CamelBatchComplete=true, CamelBatchIndex=0, CamelBatchSize=1, CamelCreatedTimestamp=Wed Jul 29 12:09:39 BST 2015, CamelEntityManager=org.hibernate.jpa.internal.EntityManagerImpl@466a65b7, CamelFileExchangeFile=GenericFile[/wildfly/standalone/data/customers/customer.xml], CamelFileLockFileAcquired=true, CamelFileLockFileName=/wildfly/standalone/data/customers/customer.xml.camelLock, CamelMessageHistory=[DefaultMessageHistory[routeId=route7, node=unmarshal3], DefaultMessageHistory[routeId=route7, node=to6], DefaultMessageHistory[routeId=route7, node=to7]], CamelToEndpoint=log://input?showAll=true}, Headers: {breadcrumbId=ID-localhost-localdomain-35597-1438166553663-11-3, CamelFileAbsolute=true, CamelFileAbsolutePath=/wildfly/standalone/data/customers/customer.xml, CamelFileContentType=application/xml, CamelFileLastModified=1438168179000, CamelFileLength=418, CamelFileName=customer.xml, CamelFileNameConsumed=customer.xml, CamelFileNameOnly=customer.xml, CamelFileParent=/wildfly/standalone/data/customers, CamelFilePath=/wildfly/standalone/data/customers/customer.xml, CamelFileRelativePath=customer.xml}, BodyType: org.wildfly.camel.examples.jpa.model.Customer, Body: <?xml version="1.0" encoding="UTF-8" standalone="yes"?><customer xmlns="http://org/wildfly/camel/examples/jpa/model/Customer">    <id>2</id>    <firstName>John</firstName>    <lastName>Doe</lastName>    <dateOfBirth>1975-12-25T00:00:00Z</dateOfBirth></customer>, Out: null: ]
```

Browse http://localhost:8080/example-camel-jpa-spring/customers and observe the list of customers that have been extracted from the 'customer' in-memory database table.

Undeploy
--------

To undeploy the example run `mvn clean -Pdeploy`.
