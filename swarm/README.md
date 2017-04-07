## WildFly-Swarm Fraction Generator

Provides WildFly-Swarm fractions for the WildFly-Camel subsystem. 

To re-generate the camel fractions, do

    rm -r ../wildfly-swarm/fractions/camel/components
    mvn clean install -pl swarm -Doutdir=../wildfly-swarm/fractions/camel

Then in wildfly-swarm, update the wildfly-camel versions and run

    mvn clean install -f fractions/camel
    mvn clean install -pl testsuite/testsuite-camel-core,testsuite/testsuite-camel-cdi,testsuite/testsuite-camel-cxf,testsuite/testsuite-camel-ejb,testsuite/testsuite-camel-jms,testsuite/testsuite-camel-jmx,testsuite/testsuite-camel-jpa,testsuite/testsuite-camel-ognl
    