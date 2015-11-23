package org.wildfly.camel.test.cdi;


import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.CamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.subB.Bootstrap;
import org.wildfly.camel.test.cdi.subB.HelloBean;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarIntegrationTest {

    private static final String SIMPLE_JAR = "camel-ejb-jar.jar";
    private static final String SIMPLE_EAR = "camel-ejb-ear.ear";

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ejb-ear-tests");
    }

    @Test
    public void testEjbJarDeployment() throws Exception {
        // We don't actually know what the camel context name will be, so defer to JMX lookup
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> camelContextNames = server.queryNames(new ObjectName("*:type=context,*"), null);
        Assert.assertEquals(1, camelContextNames.size());

        ObjectName next = camelContextNames.iterator().next();
        String camelContextName = next.getKeyProperty("name").replace("\"", "");

        CamelContext camelctx = contextRegistry.getCamelContext(camelContextName);
        Assert.assertNotNull(camelctx);

        String result = camelctx.createProducerTemplate().requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Deployment(name = SIMPLE_EAR, managed = true, testable = false)
    public static EnterpriseArchive createEarDeployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR);
        ear.addAsModule(getEjbModule());
        return ear;
    }

    private static JavaArchive getEjbModule() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SIMPLE_JAR);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addClasses(Bootstrap.class, HelloBean.class);
        return jar;
    }
}
