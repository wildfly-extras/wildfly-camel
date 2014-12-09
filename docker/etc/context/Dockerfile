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

# Default to operating in standalone mode
ENV WILDFLY_OPERATING_MODE standalone

# Set the default command to run on startup
CMD ["/opt/jboss/wildfly/extras/startup.sh"]