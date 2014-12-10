FROM wildflyext/wildfly-camel

MAINTAINER Thomas Diesler <tdiesler@redhat.com>

COPY host-camel-slave.xml /opt/jboss/wildfly/domain/configuration/

# Change the ownership of added files/dirs to `jboss` 
USER root
RUN chown -R jboss.jboss /opt/jboss/wildfly
USER jboss

# Expose the native management port 
EXPOSE 9999

# Set the default command to run on boot
CMD ["/opt/jboss/wildfly/bin/domain.sh", "--domain-config", "domain-camel.xml", "--host-config", "host-camel-slave.xml", "-Djboss.bind.address=0.0.0.0", "-Djboss.bind.address.management=0.0.0.0"]

