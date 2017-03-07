package org.wildfly.camel.test.linkedin;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.linkedin.LinkedInComponent;
import org.apache.camel.component.linkedin.LinkedInConfiguration;
import org.apache.camel.component.linkedin.api.OAuthScope;
import org.apache.camel.component.linkedin.api.model.CompanySearch;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.IntrospectionSupport;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class LinkedInIntegrationTest {

    // Enum values correspond to environment variable names
    private enum LinkedInOption {
        LINKEDIN_USERNAME("userName"),
        LINKEDIN_PASSWORD("userPassword"),
        LINKEDIN_CLIENT_ID("clientId"),
        LINKEDIN_CLIENT_SECRET("clientSecret");

        private String configPropertyName;

        LinkedInOption(String configPropertyName) {
            this.configPropertyName = configPropertyName;
        }
    }

    private static final String LINKEDIN_REDIRECT_URI = "http://localhost";
    private static final String LINKEDIN_SCOPES = "r_basicprofile";

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "linkedin-tests");
    }

    @Test
    public void testSearchCompanies() throws Exception {
        Map<String, Object> linkedInOptions = createLinkedInOptions();

        Assume.assumeTrue("[#1677] Enable LinkedIn testing in Jenkins", 
                linkedInOptions.size() == LinkedInOption.values().length);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct://SEARCHCOMPANIES")
                .to("linkedin://search/searchCompanies");
            }
        });

        linkedInOptions.put("redirectUri", LINKEDIN_REDIRECT_URI);
        linkedInOptions.put("scopes", OAuthScope.fromValues(LINKEDIN_SCOPES.split(",")));

        final LinkedInConfiguration configuration = new LinkedInConfiguration();
        IntrospectionSupport.setProperties(configuration, linkedInOptions);

        final LinkedInComponent component = new LinkedInComponent(camelctx);
        component.setConfiguration(configuration);
        camelctx.addComponent("linkedin", component);

        final Map<String, Object> headers = new HashMap<>();
        headers.put("CamelLinkedIn.fields", "");
        headers.put("CamelLinkedIn.keywords", "linkedin");

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            CompanySearch result = producer.requestBodyAndHeaders("direct://SEARCHCOMPANIES", null, headers, CompanySearch.class);
            Assert.assertNotNull("CompanySearch not null", result);
        } finally {
            camelctx.stop();
        }
    }

    protected Map<String, Object> createLinkedInOptions() throws Exception {
        final Map<String, Object> options = new HashMap<>();

        for (LinkedInOption option : LinkedInOption.values()) {
            String envVar = System.getenv(option.name());
            if (envVar == null || envVar.length() == 0) {
                options.clear();
            } else {
                options.put(option.configPropertyName, envVar);
            }
        }

        return options;
    }
}
