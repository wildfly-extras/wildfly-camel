WildFly Camel Roadmap
=====================

[Milestone 1.0.0.CR1](https://github.com/wildflyext/wildfly-camel/milestones/1.0.0%20CR1)

This is going to be the first candidate release of the Camel subsystem for for WildFly-8.1.0 and EAP-6.4.
The expected release date is **01-Dec-2014**.

#### Features

* Camel Context definition as part of the subsystem configuration
* Camel Context deployments as XML and as part of JavaEE deployments
* Integration of camel-cxf for JAX-WS and JAX-RS
* Integration of camel-jms with HornetQ
* Integration of camel-jmx
* Integration of camel-cdi
* Integration of camel-jpa
* Integration with JNDI
* Hawt.io integration
* Docker image

A number of camel component modules will be included. 
These are expected to be shared by an optional SwitchYard installation.

* camel-atom
* camel-bindy
* camel-cdi
* camel-cxf
* camel-ftp
* camel-hl7
* camel-jaxb
* camel-jms
* camel-jmx
* camel-jpa
* camel-mail
* camel-mina2
* camel-mqtt
* camel-mvel
* camel-netty
* camel-ognl
* camel-quartz
* camel-rss
* camel-saxon
* camel-script
* camel-soap
* camel-spring
* camel-sql

Support for these camel components will show up in the feature list over the course of the following releases. 
A component is supported when there is minimal test coverage and [documentation](http://tdiesler.gitbooks.io/wildfly-camel/content). 

A number of quickstarts will be included that show how to leverage camel functionality from JavaEE deployments. 
There will be a quickstart dedicated to ActiveMQ messaging. How this aligns with the HornetQ based messaging subsystem will be discussed.

Functionality that is being worked on, but may not make it in time includes:

* Camel insight integration
* Integration with Eclipse tooling

Following this milestone we plan to have regular time boxed releases every eight weeks.