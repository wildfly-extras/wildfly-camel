package org.wildfly.camel.test.wordpress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.wordpress.WordpressComponent;
import org.apache.camel.component.wordpress.WordpressComponentConfiguration;
import org.apache.camel.component.wordpress.api.WordpressConstants;
import org.apache.camel.component.wordpress.api.model.Content;
import org.apache.camel.component.wordpress.api.model.Post;
import org.apache.camel.component.wordpress.api.model.PublishableStatus;
import org.apache.camel.component.wordpress.api.model.User;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.wordpress.subA.WordpressApiMockServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class WordpressIntegrationTest {
    private static final String CONTEXT_NAME = "camel-wordpress-tests";
    @Deployment
    public static WebArchive createDeployment() throws IOException {
        WebArchive jar = ShrinkWrap.create(WebArchive.class, CONTEXT_NAME + ".war")
                .addClass(TestUtils.class)
                .addPackage(WordpressApiMockServlet.class.getPackage());
        Path resourcesDir = Paths.get("src/test/resources");
        Files.walk(resourcesDir.resolve("wordpress"))
                .forEach((p) -> jar.addAsResource(p.toFile(), resourcesDir.relativize(p).toString()));
        return jar;
    }

    private static void configureComponent(CamelContext camelctx) {
        final WordpressComponentConfiguration configuration = new WordpressComponentConfiguration();
        final WordpressComponent component = new WordpressComponent();
        configuration.setApiVersion(WordpressConstants.API_VERSION);
        configuration.setUrl("http://localhost:8080/"+ CONTEXT_NAME +"/");
        component.setConfiguration(configuration);
        camelctx.addComponent("wordpress", component);
    }

    @Test
    public void testPostSingleRequest() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("wordpress:post?id=114913").to("mock:resultSingle");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultSingle", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.allMessages().body().isInstanceOf(Post.class);

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPostListRequest() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("wordpress:post?criteria.perPage=10&criteria.orderBy=author&criteria.categories=camel,dozer,json")
                        .to("mock:resultList");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultList", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.allMessages().body().isInstanceOf(Post.class);

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testInsertPost() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("direct:insertPost").to("wordpress:post?user=ben&password=password123").to("mock:resultInsert");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultInsert", MockEndpoint.class);
            mock.expectedBodyReceived().body(Post.class);
            mock.expectedMessageCount(1);

            final Post request = new Post();
            request.setAuthor(2);
            request.setTitle(new Content("hello from postman 2"));

            ProducerTemplate template = camelctx.createProducerTemplate();
            final Post response = (Post) template.requestBody("direct:insertPost", request);
            Assert.assertEquals(response.getId(), Integer.valueOf(9));
            Assert.assertEquals(response.getStatus(), PublishableStatus.draft);

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUpdatePost() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("direct:updatePost").to("wordpress:post?id=9&user=ben&password=password123")
                        .to("mock:resultUpdate");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultUpdate", MockEndpoint.class);
            mock.expectedBodyReceived().body(Post.class);
            mock.expectedMessageCount(1);

            final Post request = new Post();
            request.setAuthor(2);
            request.setTitle(new Content("hello from postman 2 - update"));

            ProducerTemplate template = camelctx.createProducerTemplate();
            final Post response = (Post) template.requestBody("direct:updatePost", request);
            Assert.assertEquals(response.getId(), Integer.valueOf(9));
            Assert.assertEquals(response.getStatus(), PublishableStatus.draft);
            Assert.assertEquals(response.getTitle().getRaw(), "hello from postman 2 - update");
            Assert.assertEquals(response.getTitle().getRendered(), "hello from postman 2 &#8211; update");

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDeletePost() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("direct:deletePost").to("wordpress:post:delete?id=9&user=ben&password=password123")
                        .to("mock:resultDelete");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultDelete", MockEndpoint.class);
            mock.expectedBodyReceived().body(Post.class);
            mock.expectedMessageCount(1);

            ProducerTemplate template = camelctx.createProducerTemplate();
            final Post response = (Post) template.requestBody("direct:deletePost", "");
            Assert.assertEquals(response.getId(), Integer.valueOf(9));
            Assert.assertEquals(response.getStatus(), PublishableStatus.trash);

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUserSingleRequest() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("wordpress:user?id=114913").to("mock:resultSingle");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultSingle", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.allMessages().body().isInstanceOf(User.class);

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUserListRequest() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("wordpress:user?criteria.perPage=10&criteria.orderBy=name&criteria.roles=admin,editor")
                .to("mock:resultList");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultList", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.allMessages().body().isInstanceOf(User.class);

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testInsertUser() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("direct:insertUser").to("wordpress:user?user=ben&password=password123").to("mock:resultInsert");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultInsert", MockEndpoint.class);
            mock.expectedBodyReceived().body(User.class);
            mock.expectedMessageCount(1);

            final User request = new User();
            request.setEmail("bill.denbrough@derry.com");
            request.setUsername("bdenbrough");
            request.setFirstName("Bill");
            request.setLastName("Denbrough");
            request.setNickname("Big Bill");

            ProducerTemplate template = camelctx.createProducerTemplate();
            final User response = (User) template.requestBody("direct:insertUser", request);
            Assert.assertEquals(response.getId(), Integer.valueOf(3));
            Assert.assertEquals(response.getSlug(), "bdenbrough");

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUpdateUser() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("direct:updateUser").to("wordpress:user?id=9&user=ben&password=password123")
                .to("mock:resultUpdate");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultUpdate", MockEndpoint.class);
            mock.expectedBodyReceived().body(User.class);
            mock.expectedMessageCount(1);

            final User request = new User();
            request.setEmail("admin@email.com");

            ProducerTemplate template = camelctx.createProducerTemplate();
            final User response = (User) template.requestBody("direct:updateUser", request);
            Assert.assertEquals(response.getId(), Integer.valueOf(1));
            Assert.assertEquals(response.getEmail(), "admin@email.com");

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDeleteUser() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureComponent(getContext());

                from("direct:deleteUser").to("wordpress:user:delete?id=9&user=ben&password=password123")
                .to("mock:resultDelete");

            }
        });

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:resultDelete", MockEndpoint.class);
            mock.expectedBodyReceived().body(User.class);
            mock.expectedMessageCount(1);

            ProducerTemplate template = camelctx.createProducerTemplate();
            final User response = (User) template.requestBody("direct:deleteUser", "");
            Assert.assertEquals(response.getId(), Integer.valueOf(4));
            Assert.assertEquals(response.getUsername(), "bmarsh");

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

}
