## WildFly Camel

Provides [Apache Camel](http://camel.apache.org/) integration with the [WildFly Application Server](http://wildfly.org/).

The WildFly-Camel Subsystem allows you to add Camel Routes as part of the WildFly configuration. Routes can be deployed as part of JavaEE applications. JavaEE components can access the Camel Core API and various Camel Component APIs.

Your Enterprise Integration Solution can be architected as a combination of JavaEE and Camel functionality.

### Documentation

* [User Guide](http://wildflyext.gitbooks.io/wildfly-camel/content/)
* [Roadmap](https://github.com/wildfly-extras/wildfly-camel/wiki/Roadmap)
* [Whishlist](https://github.com/wildfly-extras/wildfly-camel/wiki/Whishlist)
* [JavaEE Integration](http://wildflyext.gitbooks.io/wildfly-camel/content/javaee/README.html)
* [Camel Components](http://wildflyext.gitbooks.io/wildfly-camel/content/components/README.html)

If you like to contribute to the docs, please file a [pull request](https://github.com/wildfly-extras/wildfly-camel-book/branches) against the next version branch.

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

or simply run the docker image like this ...

```
$ docker run --rm -ti -p 9990:9990 -p 8080:8080 -e WILDFLY_MANAGEMENT_USER=admin -e WILDFLY_MANAGEMENT_PASSWORD=admin wildflyext/wildfly-camel
```

Access WildFly Management Console at http://192.168.59.103:9990 and the Hawtio console at http://192.168.59.103:8080/hawtio

### Docker 

Docker images and related testsuites are included if you have `DOCKER_IP` and `DOCKER_HOST` environment variables.

On a Mac you would automatically have those when you run the build in a `boot2docker` shell.

```
DOCKER_CERT_PATH=~/.boot2docker/certs/boot2docker-vm
DOCKER_HOST=tcp://192.168.59.103:2376
DOCKER_IP=192.168.59.103
DOCKER_TLS_VERIFY=1
```

On Linux you may have to set these environment variables yourself

```
DOCKER_HOST=tcp://127.0.0.1:2375
DOCKER_IP=[host ip]
```

Make sure the docker deamon binds to a socket for a client to connect to

```
$ docker -d -H tcp://127.0.0.1:2375
```
