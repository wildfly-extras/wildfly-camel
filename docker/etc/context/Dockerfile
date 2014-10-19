# Use the wildfly-@version.wildfly@ image
FROM tdiesler/wildfly:@version.wildfly@

MAINTAINER Thomas Diesler <tdiesler@redhat.com>

# [TODO] Why does this not work?
# ADD wildfly-camel-patch-@project.version@.tar.gz /opt/jboss/wildfly

# Copy & Extract the WildFly Camel patch
COPY wildfly-camel-patch-@project.version@.tar.gz /tmp/wildfly-camel-patch.tar.gz
RUN tar -xzf /tmp/wildfly-camel-patch.tar.gz -C /opt/jboss/wildfly 

# Set the default command to run on boot
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-camel.xml", "-b", "0.0.0.0"]
