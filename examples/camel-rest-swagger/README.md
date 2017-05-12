Camel REST Swagger example
--------------------------

This example demonstrates using the camel REST DSL with the camel-swagger-java component.

The application defines a basic REST API for creating, reading, updating and deleting customers. The API is backed with Swagger
documentation and provides the Swagger UI so that you can test the API endpoints within your web browser.

Prerequisites
-------------

* Maven
* An application server with the wildfly-camel subsystem installed

Running the example
-------------------

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`
3. Browse to http://localhost:8080/example-camel-rest-swagger/

You should see a page welcoming you to the WildFly Camel REST API.

Testing Camel Rest Swagger
--------------------------

From the Swagger UI, click the 'list operations' link to view the available REST endpoints.

To start with, create a new customer by selecting the 'POST /customers' operation. Click the 'example value' JSON field
so that the request 'body' is populated. You can edit the 'firstName' or 'lastName' fields if you wish.

Now click the 'Try it out' button to send the request.

You should now be able to retrieve the customer by clicking on the 'GET /customers' operation. click 'Try it out' and
you'll see the JSON representation of the customer you just created.

To delete a customer, click the 'DELETE /customers' operation, enter the customer id into the 'id' field and click 'Try it out'.

Undeploy
--------

To undeploy the example run `mvn clean -Pdeploy`.
