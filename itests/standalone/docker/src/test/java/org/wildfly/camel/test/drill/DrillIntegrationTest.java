package org.wildfly.camel.test.drill;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.drill.DrillConnectionMode;
import org.apache.camel.component.drill.DrillConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class DrillIntegrationTest {

    private static final String CONTAINER_NAME = "drill";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-drill-tests.jar")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_NAME);
        cubeController.start(CONTAINER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_NAME);
        cubeController.destroy(CONTAINER_NAME);
    }

    private static final DrillConnectionMode mode = DrillConnectionMode.DRILLBIT;
    private static final Integer DRILL_PORT = 31010;
    /**
     * A test query. We assume that the sample data available in the Drill distro will not change with new Drill releases.
     */
    private static final String QUERY = "select * from cp.`employee.json` order by employee_id limit 3";

    @Test
    public void producer() throws Exception {

        final String host = TestUtils.getDockerHost();
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:in").setHeader(DrillConstants.DRILL_QUERY, constant(QUERY)).to("drill://" + host + "?mode=" + mode.name() + "&port=" + DRILL_PORT).log("${body}")
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        camelctx.start();
        try {

            ProducerTemplate template = camelctx.createProducerTemplate();

            template.sendBody("direct:in", "");

            Assert.assertEquals(1, mockEndpoint.getExchanges().size());
            List<?> body = mockEndpoint.getExchanges().get(0).getIn().getBody(List.class);
            Assert.assertEquals(3, body.size());
            Map<?, ?> record0 = (Map<?, ?>) body.get(0);
            Assert.assertEquals(Long.valueOf(1), record0.get("employee_id"));
            Assert.assertEquals("Sheri Nowmer", record0.get("full_name"));

        } finally {
            camelctx.stop();
        }

    }

}
