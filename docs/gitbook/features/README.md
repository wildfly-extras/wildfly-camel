# Features

## Camel Context Definitions

Camel Contexts can be defined as part of the WildFly Camel subsystem definition.

## Camel Context Deployments

Camel Contexts can be deployed as single XML file or part of another deployment supported by WildFly.

## Camel Feature Provisioning

WildFly Camel provides feature provisioning similar to Eclipse or Karaf. It uses Gravia to provide this functionality.

## Integration with JAX-WS

WebService support is provided through the camel-cxf component which integrates with the WildFly WebServices subsystem.

## Integration with JMS

Messaging support is provided through the camel-jms component which integrates with the WildFly Messaging subsystem.

## Integration with JNDI

Naming support is provided through the WildFlyCamelContext which integrates with the WildFly Naming subsystem.

## Integration with JMX

Management support is provided through the camel-jmx component which integrates with the WildFly JMX subsystem.

## Arquillian Test Support

WildFly Camel uses Arquillian to connect to an already running WildFly server or start one up when needed.
