## WildFly Docker Setup

Details about the WildFly CentOS image are described [here](https://github.com/openshift/openshift-docs/blob/master/using_images/s2i_images/wildfly.adoc).

    minishift delete
    minishift start --vm-driver=virtualbox --memory 12048 --cpus 3
    eval $(minishift docker-env)

Build the WildFly Camel docker image

    mvn clean install -Ddocker -pl docker -am

Run the WildFly Camel docker image

    docker run --rm -ti -e WILDFLY_MANAGEMENT_USER=admin -e WILDFLY_MANAGEMENT_PASSWORD=admin -p 8080:8080 -p 9990:9990 wildflyext/wildfly-camel

Test access to the console http://192.168.99.100:9990/console and to hawtio http://192.168.99.100:8080/hawtio

