FROM wildflyext/wildfly-camel

MAINTAINER Thomas Diesler <tdiesler@redhat.com>

# Set the default command to run on boot
CMD ["/opt/jboss/wildfly/bin/domain.sh", "--domain-config", "domain-camel.xml", "--host-config", "host-camel-slave.xml", "-Djboss.bind.address=0.0.0.0", "-Djboss.bind.address.management=0.0.0.0"]

