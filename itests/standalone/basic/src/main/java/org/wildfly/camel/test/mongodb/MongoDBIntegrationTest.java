/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.wildfly.camel.test.mongodb;

import java.util.Formatter;
import java.util.List;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.IOHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

@CamelAware
@RunWith(Arquillian.class)
public class MongoDBIntegrationTest {

    private static final int PORT = 27017;
    private static EmbeddedMongoServer mongoServer;
    private MongoCollection<BasicDBObject> testCollection;
    private MongoCollection<BasicDBObject> dynamicCollection;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-mongodb-tests")
            .addClasses(EmbeddedMongoServer.class, EnvironmentUtils.class);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (!EnvironmentUtils.isAIX()) {
            if (mongoServer == null) {
                mongoServer = new EmbeddedMongoServer(PORT);
            }
            mongoServer.start();
        }
    }

    @AfterClass
    public static void afterClass() {
        if (mongoServer != null) 
            mongoServer.stop();
    }

    @Before
    public void setUp() throws Exception {
        if (!EnvironmentUtils.isAIX()) {
            MongoClient client = new MongoClient("localhost", PORT);
            MongoDatabase db = client.getDatabase("test");

            InitialContext context = new InitialContext();
            context.bind("mongoConnection", client);

            String testCollectionName = "camelTest";
            testCollection = db.getCollection(testCollectionName, BasicDBObject.class);
            testCollection.drop();
            testCollection = db.getCollection(testCollectionName, BasicDBObject.class);

            String dynamicCollectionName = testCollectionName.concat("Dynamic");
            dynamicCollection = db.getCollection(dynamicCollectionName, BasicDBObject.class);
            dynamicCollection.drop();
            dynamicCollection = db.getCollection(dynamicCollectionName, BasicDBObject.class);

            setupTestData();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!EnvironmentUtils.isAIX()) {
            InitialContext context = new InitialContext();
            context.unbind("mongoConnection");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMongoFindAll() throws Exception {

        Assume.assumeFalse("[ENTESB-6590] MongoDBIntegrationTest fails on AIX", EnvironmentUtils.isAIX());
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("mongodb:mongoConnection?database=test&collection=camelTest&operation=findAll&dynamicity=true");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            List<DBObject> result = template.requestBody("direct:start", null, List.class);

            Assert.assertEquals(1000, result.size());

            for (DBObject dbObject : result) {
                Assert.assertNotNull(dbObject.get("_id"));
                Assert.assertNotNull(dbObject.get("scientist"));
                Assert.assertNotNull(dbObject.get("fixedField"));
            }
        } finally {
            testCollection.drop();
            dynamicCollection.drop();
            camelctx.stop();
        }
    }


    private void setupTestData() {
        String[] scientists = {"Einstein", "Darwin", "Copernicus", "Pasteur", "Curie", "Faraday", "Newton", "Bohr", "Galilei", "Maxwell"};
        for (int i = 1; i <= 1000; i++) {
            int index = i % scientists.length;
            Formatter f = new Formatter();
            String doc = f.format("{\"_id\":\"%d\", \"scientist\":\"%s\", \"fixedField\": \"fixedValue\"}", i, scientists[index]).toString();
            IOHelper.close(f);
            testCollection.insertOne((BasicDBObject) JSON.parse(doc));
        }
    }
}
