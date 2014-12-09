FROM wildflyext/wildfly-camel

MAINTAINER Thomas Diesler <tdiesler@redhat.com>

COPY domain-camel-master.xml /opt/jboss/wildfly/domain/configuration/
COPY host-camel-master.xml /opt/jboss/wildfly/domain/configuration/

# Change the ownership of added files/dirs to `jboss` 
USER root
RUN chown -R jboss.jboss /opt/jboss/wildfly
USER jboss

# Add a management user to the domain controller
RUN /opt/jboss/wildfly/bin/add-user.sh --user slave --password slave --silent

# Expose the native management port 
EXPOSE 9999

# Set the default command to run on boot
CMD ["/opt/jboss/wildfly/bin/domain.sh", "--domain-config", "domain-camel-master.xml", "--host-config", "host-camel-master.xml", "-Djboss.bind.address=0.0.0.0", "-Djboss.bind.address.management=0.0.0.0"]

