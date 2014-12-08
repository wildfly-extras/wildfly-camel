# Use the offical jboss/wildfly base image
FROM jboss/wildfly:@version.wildfly@

MAINTAINER Thomas Diesler <tdiesler@redhat.com>

# Add the WildFly-Camel patch
ADD wildfly-camel-patch-@project.version@.tar.gz /opt/jboss/wildfly

# Change the ownership of added files/dirs to `jboss` 
USER root
RUN chown -R jboss.jboss /opt/jboss/wildfly
USER jboss

# Set the default command to run on boot
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-camel.xml", "-b", "0.0.0.0"]
