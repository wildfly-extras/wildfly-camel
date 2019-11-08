# Testing CI, please don't merge

## WildFly Camel

[![Jenkins](https://img.shields.io/jenkins/s/https/ci.fabric8.io/wildfly-camel.svg?maxAge=600)](https://fabric8-ci.fusesource.com/view/wildfly-camel/job/wildfly-camel/)
[![License](https://img.shields.io/:license-Apache2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.wildfly.camel/wildfly-camel.svg?maxAge=600)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Aorg.wildfly.camel%20a%3Awildfly-camel)
[![Join the chat at freenode:wildfly-camel](https://img.shields.io/badge/irc-freenode%3A%20%23wildfly--camel-blue.svg)](http://webchat.freenode.net/?channels=%23wildfly-camel)

[![Open workspace in Eclipse Che](http://beta.codenvy.com/factory/resources/codenvy-contribute.svg)](https://beta.codenvy.com/f?id=chknwakr0ykhqr1q)

Provides [Apache Camel](http://camel.apache.org/) integration with the [WildFly Application Server](http://wildfly.org/).

The WildFly-Camel Subsystem allows you to add Camel Routes as part of the WildFly configuration. Routes can be deployed as part of JavaEE applications. JavaEE components can access the Camel Core API and various Camel Component APIs.

Your Enterprise Integration Solution can be architected as a combination of JavaEE and Camel functionality.

### Documentation

The docs are generated using [Asciidoctor](http://asciidoctor.org/docs).

* [User Guide](http://wildfly-extras.github.io/wildfly-camel)
* [JavaEE Integration](http://wildfly-extras.github.io/wildfly-camel/#_javaee_integration)
* [Camel Components](http://wildfly-extras.github.io/wildfly-camel/#_camel_components)

To generate an update of the docs use:

```
$ mvn clean install -f docs
```

If you like to contribute to the docs, please file a [pull request](https://github.com/wildfly-extras/wildfly-camel) against the master branch.

### Running Examples

The [wildfly-camel-examples](https://github.com/wildfly-extras/wildfly-camel-examples) are also available on Eclipse Che.

[![Open workspace in Eclipse Che](http://beta.codenvy.com/factory/resources/codenvy-contribute.svg)](https://beta.codenvy.com/f?id=chknwakr0ykhqr1q)

### System Requirements

#### Java

Minimum of Java 1.8, to run WildFly and Maven.

#### Maven

Minimum of Maven 3.2.3.


### Build

The default build is straight forward

```
$ mvn clean install
```

The extended build includes the set of JavaEE integration examples

```
$ mvn clean install -Dts.all
```

If you like to contribute to the project, please file a [pull request](https://github.com/wildfly-extras/wildfly-camel/pulls).

### Install

Simply apply the [wildfly-camel-patch](https://github.com/wildfly-extras/wildfly-camel/releases) to a [compatible wildfly](https://github.com/wildfly-extras/wildfly-camel/blob/master/docs/guide/start/compatibility.adoc) version.

### Run

In your WildFly home directory run ...

```
$ bin/standalone.sh -c standalone-camel.xml
```

### Docker

To setup OpenShift Origin with an integrated Docker environment, follow the instructions [here](http://wildfly-extras.github.io/wildfly-camel/#_openshift_local).

Then simply run the docker image like this ...

```
$ docker run --rm -ti -p 9990:9990 -p 8080:8080 -e WILDFLY_MANAGEMENT_USER=admin -e WILDFLY_MANAGEMENT_PASSWORD=admin wildflyext/wildfly-camel
```

Access WildFly Management Console at https://10.2.2.2:8443/console and the Hawtio console at http://10.2.2.2:8080/hawtio
