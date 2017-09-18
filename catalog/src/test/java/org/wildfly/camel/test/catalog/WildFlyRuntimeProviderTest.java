/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.catalog;

import java.util.List;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.catalog.WildFlyRuntimeProvider;

public class WildFlyRuntimeProviderTest {

    static CamelCatalog catalog;

    @BeforeClass
    public static void createCamelCatalog() {
        catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new WildFlyRuntimeProvider());
    }

    @Test
    public void testGetVersion() throws Exception {
        String version = catalog.getCatalogVersion();
        Assert.assertNotNull(version);

        String loaded = catalog.getLoadedVersion();
        Assert.assertNotNull(loaded);
        Assert.assertEquals(version, loaded);
    }

    @Test
    public void testProviderName() throws Exception {
        Assert.assertEquals("wildfly", catalog.getRuntimeProvider().getProviderName());
    }

    @Test
    public void testFindComponentNames() throws Exception {
        List<String> names = catalog.findComponentNames();

        Assert.assertNotNull(names);
        Assert.assertFalse(names.isEmpty());

        Assert.assertTrue(names.contains("file"));
        Assert.assertTrue(names.contains("ftp"));
        Assert.assertTrue(names.contains("jms"));
        Assert.assertTrue(names.contains("ejb"));

        // Test rejected
        Assert.assertFalse(names.contains("apns"));
    }

    @Test
    public void testFindDataFormatNames() throws Exception {
        List<String> names = catalog.findDataFormatNames();

        Assert.assertNotNull(names);
        Assert.assertFalse(names.isEmpty());

        Assert.assertTrue(names.contains("bindy-csv"));
        Assert.assertTrue(names.contains("zip"));
        Assert.assertTrue(names.contains("zipfile"));

        // Test rejected
        //Assert.assertFalse(names.contains("boon"));
    }

    @Test
    public void testFindLanguageNames() throws Exception {
        List<String> names = catalog.findLanguageNames();

        Assert.assertNotNull(names);
        Assert.assertFalse(names.isEmpty());

        Assert.assertTrue(names.contains("simple"));
        Assert.assertTrue(names.contains("spel"));
        Assert.assertTrue(names.contains("xpath"));

        // Test rejected
        Assert.assertFalse(names.contains("sql"));
    }

    @Test
    public void testComponentArtifactId() throws Exception {
        String json = catalog.componentJSonSchema("ftp");
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("camel-ftp"));
    }

    @Test
    public void testDataFormatArtifactId() throws Exception {
        String json = catalog.dataFormatJSonSchema("bindy-csv");
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("camel-bindy"));
    }

    @Test
    public void testLanguageArtifactId() throws Exception {
        String json = catalog.languageJSonSchema("spel");
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("camel-spring"));
    }

}
