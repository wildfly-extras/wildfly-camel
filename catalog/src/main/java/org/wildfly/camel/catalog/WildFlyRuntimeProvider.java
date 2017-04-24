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
package org.wildfly.camel.catalog;

import org.apache.camel.catalog.DefaultRuntimeProvider;
import org.apache.camel.catalog.RuntimeProvider;

/**
 * A WildFly based {@link RuntimeProvider} which only includes the supported Camel components, data formats, and languages
 * for WildFly.
 */
public class WildFlyRuntimeProvider extends DefaultRuntimeProvider {

    private static final String COMPONENT_DIR = "org/wildfly/camel/catalog/components";
    private static final String DATAFORMAT_DIR = "org/wildfly/camel/catalog/dataformats";
    private static final String LANGUAGE_DIR = "org/wildfly/camel/catalog/languages";
    private static final String COMPONENTS_CATALOG = "org/wildfly/camel/catalog/components.properties";
    private static final String DATA_FORMATS_CATALOG = "org/wildfly/camel/catalog/dataformats.properties";
    private static final String LANGUAGE_CATALOG = "org/wildfly/camel/catalog/languages.properties";

    @Override
    public String getProviderName() {
        return "wildfly";
    }

    @Override
    public String getComponentJSonSchemaDirectory() {
        return COMPONENT_DIR;
    }

    @Override
    public String getDataFormatJSonSchemaDirectory() {
        return DATAFORMAT_DIR;
    }

    @Override
    public String getLanguageJSonSchemaDirectory() {
        return LANGUAGE_DIR;
    }

    @Override
    protected String getComponentsCatalog() {
        return COMPONENTS_CATALOG;
    }

    @Override
    protected String getDataFormatsCatalog() {
        return DATA_FORMATS_CATALOG;
    }

    @Override
    protected String getLanguageCatalog() {
        return LANGUAGE_CATALOG;
    }
}
