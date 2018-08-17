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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.gridfs.GridFS;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.IOHelper;
import org.bson.Document;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class MongoDBIntegrationTest {

    private static final int ROWS = 20;
    private static final int PORT = 27017;
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBIntegrationTest.class);
    private static EmbeddedMongoServer mongoServer;

    private MongoCollection<BasicDBObject> testCollection;
    private MongoCollection<BasicDBObject> dynamicCollection;
    private MongoClient mongoClient;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-mongodb-tests")
            .addClasses(EmbeddedMongoServer.class, EnvironmentUtils.class);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (!EnvironmentUtils.isWindows()) {
            if (mongoServer == null) {
                mongoServer = new EmbeddedMongoServer(PORT);
            }
            mongoServer.start();
        }
    }

    @AfterClass
    public static void afterClass() {
        if (mongoServer != null) {
            mongoServer.stop();
        }
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeFalse("[#2486] MongoDBIntegrationTest fails on Windows", EnvironmentUtils.isWindows());

        mongoClient = new MongoClient("localhost", PORT);
        MongoDatabase db = mongoClient.getDatabase("test");

        InitialContext context = new InitialContext();
        context.bind("mdb", mongoClient);

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

    @After
    public void tearDown() {
        try {
            InitialContext context = new InitialContext();
            context.unbind("mdb");
        } catch (NamingException e) {
            // Ignore
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMongoFindAll() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("mongodb:mdb?database=test&collection=camelTest&operation=findAll&dynamicity=true");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            List<DBObject> result = template.requestBody("direct:start", null, List.class);

            Assert.assertEquals(ROWS, result.size());

            for (DBObject obj : result) {
                Assert.assertNotNull(obj.get("_id"));
                Assert.assertNotNull(obj.get("scientist"));
                Assert.assertNotNull(obj.get("fixedField"));
            }
        } finally {
            testCollection.drop();
            dynamicCollection.drop();
            camelctx.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMongo3FindAll() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("mongodb3:mdb?database=test&collection=camelTest&operation=findAll&dynamicity=true");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            List<Document> result = template.requestBody("direct:start", null, List.class);

            Assert.assertEquals(ROWS, result.size());

            for (Document obj : result) {
                Assert.assertNotNull(obj.get("_id"));
                Assert.assertNotNull(obj.get("scientist"));
                Assert.assertNotNull(obj.get("fixedField"));
            }
        } finally {
            testCollection.drop();
            dynamicCollection.drop();
            camelctx.stop();
        }
    }

    @Test
    public void testProducerOperations() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:create").to("mongodb-gridfs:mdb?database=testA&operation=create&bucket=" + getBucket());
                from("direct:remove").to("mongodb-gridfs:mdb?database=testA&operation=remove&bucket=" + getBucket());
                from("direct:findOne").to("mongodb-gridfs:mdb?database=testA&operation=findOne&bucket=" + getBucket());
                from("direct:listAll").to("mongodb-gridfs:mdb?database=testA&operation=listAll&bucket=" + getBucket());
                from("direct:count").to("mongodb-gridfs:mdb?database=testA&operation=count&bucket=" + getBucket());
                from("direct:headerOp").to("mongodb-gridfs:mdb?database=testA&bucket=" + getBucket());
            }
        });

        GridFS gridfs = new GridFS(mongoClient.getDB("testA"), getBucket());

        camelctx.start();
        try {
            String[] indexes = {
                getIndex("testA", getBucket(), "files"),
                getIndex("testA", getBucket(), "chunks"),
            };

            waitForIndexes("testA", indexes);

            Map<String, Object> headers = new HashMap<String, Object>();
            String fn = "filename.for.db.txt";
            Assert.assertEquals(0, gridfs.find(fn).size());

            headers.put(Exchange.FILE_NAME, fn);
            String data = "This is some stuff to go into the db";

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBodyAndHeaders("direct:create", data, headers);
            Assert.assertEquals(1, gridfs.find(fn).size());
            Assert.assertEquals(1, template.requestBodyAndHeaders("direct:count", null, headers));
            InputStream ins = template.requestBodyAndHeaders("direct:findOne", null, headers, InputStream.class);
            Assert.assertNotNull(ins);
            byte b[] = new byte[2048];
            int i = ins.read(b);
            Assert.assertEquals(data, new String(b, 0, i, "utf-8"));

            headers.put(Exchange.FILE_NAME, "2-" + fn);

            template.requestBodyAndHeaders("direct:create", data + "data2", headers);
            Assert.assertEquals(1, template.requestBodyAndHeaders("direct:count", null, headers));
            Assert.assertEquals(2, template.requestBody("direct:count", null, Integer.class).intValue());

            String s = template.requestBody("direct:listAll", null, String.class);
            Assert.assertTrue(s.contains("2-" + fn));
            template.requestBodyAndHeaders("direct:remove", null, headers);
            Assert.assertEquals(1, template.requestBody("direct:count", null, Integer.class).intValue());
            s = template.requestBody("direct:listAll", null, String.class);
            Assert.assertFalse(s.contains("2-" + fn));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testConsumerOperations() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {

                from("direct:create-a")
                .to("mongodb-gridfs:mdb?database=testB&operation=create&bucket=" + getBucket("-a"));

                from("direct:create-b")
                .to("mongodb-gridfs:mdb?database=testB&operation=create&bucket=" + getBucket("-b"));

                from("direct:create-c")
                .to("mongodb-gridfs:mdb?database=testB&operation=create&bucket=" + getBucket("-c"));

                from("mongodb-gridfs:mdb?database=testB&bucket=" + getBucket("-a"))
                .convertBodyTo(String.class).to("mock:test");

                from("mongodb-gridfs:mdb?database=testB&bucket=" + getBucket("-b") + "&queryStrategy=FileAttribute")
                .convertBodyTo(String.class).to("mock:test");

                from("mongodb-gridfs:mdb?database=testB&bucket=" + getBucket("-c") + "&queryStrategy=PersistentTimestamp")
                .convertBodyTo(String.class).to("mock:test");
            }
        });

        camelctx.start();
        try {
            String[] indexes = {
                getIndex("testB", getBucket("-a"), "files"),
                getIndex("testB", getBucket("-a"), "chunks"),
                getIndex("testB", getBucket("-b"), "files"),
                getIndex("testB", getBucket("-b"), "chunks"),
                getIndex("testB", getBucket("-c"), "files"),
                getIndex("testB", getBucket("-c"), "chunks"),
                getIndex("testB", "camel-timestamps", null),
            };

            waitForIndexes("testB", indexes);

            runTest(camelctx, "direct:create-a", new GridFS(mongoClient.getDB("testB"), getBucket("-a")));
            runTest(camelctx, "direct:create-b", new GridFS(mongoClient.getDB("testB"), getBucket("-b")));
            runTest(camelctx, "direct:create-c", new GridFS(mongoClient.getDB("testB"), getBucket("-c")));
        } finally {
            camelctx.stop();
        }
    }

    public void runTest(CamelContext camelctx, String target, GridFS gridfs) throws Exception {

        String data = "This is some stuff to go into the db";
        MockEndpoint mock = camelctx.getEndpoint("mock:test", MockEndpoint.class);
        mock.reset();
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived(data);

        Map<String, Object> headers = new HashMap<>();
        String fn = "filename.for.db.txt";
        Assert.assertEquals(0, gridfs.find(fn).size());

        headers.put(Exchange.FILE_NAME, fn);
        ProducerTemplate template = camelctx.createProducerTemplate();
        template.requestBodyAndHeaders(target, data, headers);

        mock.assertIsSatisfied(5000);

        mock.reset();
        mock.expectedMessageCount(3);
        mock.expectedBodiesReceived(data, data, data);

        headers.put(Exchange.FILE_NAME, fn + "_1");
        template.requestBodyAndHeaders(target, data, headers);
        headers.put(Exchange.FILE_NAME, fn + "_2");
        template.requestBodyAndHeaders(target, data, headers);
        headers.put(Exchange.FILE_NAME, fn + "_3");
        template.requestBodyAndHeaders(target, data, headers);
        mock.assertIsSatisfied(5000);
    }

    private void setupTestData() {
        String[] scientists = {"Einstein", "Darwin", "Copernicus", "Pasteur", "Curie", "Faraday", "Newton", "Bohr", "Galilei", "Maxwell"};
        for (int i = 1; i <= ROWS; i++) {
            int index = i % scientists.length;
            Formatter f = new Formatter();
            String doc = f.format("{\"_id\":\"%d\", \"scientist\":\"%s\", \"fixedField\": \"fixedValue\"}", i, scientists[index]).toString();
            IOHelper.close(f);
            testCollection.insertOne(BasicDBObject.parse(doc));
        }
    }

    private void waitForIndexes(String db, String ...indexNames) throws Exception {
        final long timeout = 15000;
        final long start = System.currentTimeMillis();
        final MongoDatabase database = mongoClient.getDatabase(db);
        final List<String> collectionNames = Arrays.asList(indexNames)
            .stream()
            .map((c) -> {
                if (c.matches(".*\\..*\\..*")) {
                    return c.substring(0, c.lastIndexOf("."));
                }
                return c;
            })
            .collect(Collectors.toList());

        int expectedIndexCount = indexNames.length * 2;
        do {
            int indexCount = 0;

            for (String collectionName : collectionNames) {
                MongoCollection<Document> collection = database.getCollection(collectionName);
                if (collection != null) {
                    ListIndexesIterable<Document> documents = collection.listIndexes();
                    MongoCursor<Document> iterator = documents.iterator();
                    while (iterator.hasNext()) {
                        iterator.next();
                        indexCount++;
                    }
                }
            }

            if (indexCount == expectedIndexCount) {
                LOG.info("MongoDB collection indexes created");
                return;
            }

            LOG.info("Waiting on creation of {} indexes. Currently have {}.", expectedIndexCount, indexCount);
            Thread.sleep(1000);
        } while (!((System.currentTimeMillis() - start) >= timeout));

        throw new IllegalStateException("Gave up waiting for MongoDB collection indexes to be created");
    }

    private static String getBucket() {
        return getBucket("");
    }

    private static String getBucket(String suffix) {
        return MongoDBIntegrationTest.class.getSimpleName() + suffix;
    }

    private static String getIndex(String db, String suffix, String type) {
        String index = suffix;
        if (type != null) {
            index += "." + type;
        }
        return index;
    }
}
