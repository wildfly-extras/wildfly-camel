# Use the offical jboss/wildfly base image
FROM jboss/wildfly:@version.wildfly@

MAINTAINER Thomas Diesler <tdiesler@redhat.com>

# Add the WildFly-Camel patch
ADD wildfly-camel-patch-@project.version@.tar.gz /opt/jboss/wildfly

# Add the docker startup script
ADD startup.sh /opt/jboss/wildfly/extras/startup.sh

# Change the ownership of added files/dirs to `jboss`
USER root
RUN chown -R jboss.jboss /opt/jboss/wildfly
RUN chmod +x /opt/jboss/wildfly/extras/startup.sh
USER jboss

# Expose the native management port 
EXPOSE 9999

# Set default command line args for standalone mode
CMD ["/opt/jboss/wildfly/extras/startup.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-c", "standalone-camel.xml"]
