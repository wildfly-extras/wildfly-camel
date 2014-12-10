FROM wildflyext/wildfly-camel

MAINTAINER Thomas Diesler <tdiesler@redhat.com>

# Add a management user to the domain controller
RUN /opt/jboss/wildfly/bin/add-user.sh --user slave --password slave --silent

# Expose the native management port 
EXPOSE 9999

# Set the default command to run on boot
CMD ["/opt/jboss/wildfly/bin/domain.sh", "--domain-config", "domain-camel.xml", "--host-config", "host-camel.xml", "-Djboss.bind.address=0.0.0.0", "-Djboss.bind.address.management=0.0.0.0"]

