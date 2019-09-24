/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.cmis;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cmis.CamelCMISActions;
import org.apache.camel.component.cmis.CamelCMISConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CmisIntegrationTest {

    private static final String CMIS_WAR = "chemistry-opencmis-server-inmemory.war";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cmis-tests.jar");
    }

    @Deployment(testable = false, name = CMIS_WAR, order = 1)
    public static WebArchive createCmisDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/dependencies/" + CMIS_WAR))
            .addAsWebInfResource("cmis/jboss-deployment-structure.xml", "jboss-deployment-structure.xml")
            .addAsWebInfResource("cmis/web.xml", "web.xml");
    }

    @Test
    public void testCmisConsumer() throws Exception {
        deleteAllContent();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("cmis://http://127.0.0.1:8080/chemistry-opencmis-server-inmemory/atom11?pageSize=50")
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(5);

        camelctx.start();
        try {
            populateRepositoryRootFolderWithTwoFoldersAndTwoDocuments();
            mockEndpoint.assertIsSatisfied();

            List<Exchange> exchanges = mockEndpoint.getExchanges();
            Assert.assertTrue(getNodeNameForIndex(exchanges, 0).equals("RootFolder"));
            Assert.assertTrue(getNodeNameForIndex(exchanges, 1).equals("Folder1"));
            Assert.assertTrue(getNodeNameForIndex(exchanges, 2).equals("Folder2"));
            Assert.assertTrue(getNodeNameForIndex(exchanges, 3).contains(".txt"));
            Assert.assertTrue(getNodeNameForIndex(exchanges, 4).contains(".txt"));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testCmisProducer() throws Exception {
        deleteAllContent();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("cmis://http://127.0.0.1:8080/chemistry-opencmis-server-inmemory/atom11");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            String content = "Some content to be store";
            String rootFolderId = createSession().getRootFolder().getId();

            Map<String,Object> headers = new HashMap<>();
            headers.put(PropertyIds.CONTENT_STREAM_MIME_TYPE, "text/plain; charset=UTF-8");
            headers.put(CamelCMISConstants.CMIS_OBJECT_ID, rootFolderId);
            headers.put(CamelCMISConstants.CMIS_ACTION, CamelCMISActions.CREATE);
            headers.put(PropertyIds.NAME, "test.file");

            CmisObject object = template.requestBodyAndHeaders("direct:start", "Some content to be store", headers, CmisObject.class);
            Assert.assertNotNull(object);

            String nodeContent = getDocumentContentAsString(object.getId());
            Assert.assertEquals(content, nodeContent);
        } finally {
            camelctx.close();
        }
    }

    private CmisObject retrieveCMISObjectByIdFromServer(String nodeId) throws Exception {
        Session session = createSession();
        return session.getObject(nodeId);
    }

    private String getDocumentContentAsString(String nodeId) throws Exception {
        CmisObject cmisObject = retrieveCMISObjectByIdFromServer(nodeId);
        Document doc = (Document)cmisObject;
        InputStream inputStream = doc.getContentStream().getStream();
        return readFromStream(inputStream);
    }

    private String readFromStream(InputStream in) throws Exception {
        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String strLine;
        while ((strLine = br.readLine()) != null) {
            result.append(strLine);
        }
        in.close();
        return result.toString();
    }


    private String getNodeNameForIndex(List<Exchange> exchanges, int index) {
        return exchanges.get(index).getIn().getHeader("cmis:name", String.class);
    }

    private void deleteAllContent() {
        Session session = createSession();
        Folder rootFolder = session.getRootFolder();
        ItemIterable<CmisObject> children = rootFolder.getChildren();
        for (CmisObject cmisObject : children) {
            if ("cmis:folder".equals(cmisObject.getPropertyValue(PropertyIds.OBJECT_TYPE_ID))) {
                List<String> notDeltedIdList = ((Folder)cmisObject)
                        .deleteTree(true, UnfileObject.DELETE, true);
                if (notDeltedIdList != null && notDeltedIdList.size() > 0) {
                    throw new RuntimeException("Cannot empty repo");
                }
            } else {
                cmisObject.delete(true);
            }
        }
        session.getBinding().close();
    }

    private void populateRepositoryRootFolderWithTwoFoldersAndTwoDocuments()
            throws UnsupportedEncodingException {
        Folder folder1 = createFolderWithName("Folder1");
        Folder folder2 = createChildFolderWithName(folder1, "Folder2");
        createTextDocument(folder2, "Document2.1", "2.1.txt");
        createTextDocument(folder2, "Document2.2", "2.2.txt");
    }

    private void createTextDocument(Folder newFolder, String content, String fileName)
            throws UnsupportedEncodingException {
        byte[] buf = content.getBytes("UTF-8");
        ByteArrayInputStream input = new ByteArrayInputStream(buf);
        ContentStream contentStream = createSession().getObjectFactory()
                .createContentStream(fileName, buf.length, "text/plain; charset=UTF-8", input);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, CamelCMISConstants.CMIS_DOCUMENT);
        properties.put(PropertyIds.NAME, fileName);
        newFolder.createDocument(properties, contentStream, VersioningState.NONE);
    }

    private Folder createFolderWithName(String folderName) {
        Folder rootFolder = createSession().getRootFolder();
        return createChildFolderWithName(rootFolder, folderName);
    }

    private Folder createChildFolderWithName(Folder parent, String childName) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, CamelCMISConstants.CMIS_FOLDER);
        properties.put(PropertyIds.NAME, childName);
        return parent.createFolder(properties);
    }

    private Session createSession() {
        SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();
        parameter.put(SessionParameter.ATOMPUB_URL, "http://127.0.0.1:8080/chemistry-opencmis-server-inmemory/atom11/atom11");
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        Repository repository = sessionFactory.getRepositories(parameter).get(0);
        return repository.createSession();
    }
}
