## WildFly Camel

Provides [Apache Camel](http://camel.apache.org/) integration with the [WildFly Application Server](http://wildfly.org/).

The WildFly-Camel Subsystem allows you to add Camel Routes as part of the WildFly configuration. Routes can be deployed as part of JavaEE applications. JavaEE components can access the Camel Core API and various Camel Component APIs.

Your Enterprise Integration Solution can be architected as a combination of JavaEE and Camel functionality.

### Documentation

* [User Guide](http://wildflyext.gitbooks.io/wildfly-camel/content/)
* [JavaEE Integration](http://wildflyext.gitbooks.io/wildfly-camel/content/javaee/index.html)
* [Camel Components](http://wildflyext.gitbooks.io/wildfly-camel/content/components/index.html)

If you like to contribute to the docs, please file a [pull request](https://github.com/wildfly-extras/wildfly-camel-book/branches) against the next version branch.

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

Simply apply the [wildfly-camel-patch](https://github.com/wildfly-extras/wildfly-camel/releases) to a [compatible wildfly](http://wildflyext.gitbooks.io/wildfly-camel/content/start/compatibility.html) version.

If you use the [wildflyext/wildfly-camel](https://registry.hub.docker.com/u/wildflyext/wildfly-camel/) docker distribution this step does not need to be performed.

### Run

In your WildFly home directory run ...

```
$ bin/standalone.sh -c standalone-camel.xml
```

### Docker 

To setup OpenShift Origin with an integrated Docker environment, follow the instructions [here](wiki/OpenShift-Origin).

Then simply run the docker image like this ...

```
$ docker run --rm -ti -p 9990:9990 -p 8080:8080 -e WILDFLY_MANAGEMENT_USER=admin -e WILDFLY_MANAGEMENT_PASSWORD=admin wildflyext/wildfly-camel
```

Access WildFly Management Console at http://vagrant.origin:9990 and the Hawtio console at http://vagrant.origin:8080/hawtio
