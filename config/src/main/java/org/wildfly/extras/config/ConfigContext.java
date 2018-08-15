/*
 * Copyright 2015 JBoss Inc
 *
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
 */
package org.wildfly.extras.config;

import java.nio.file.Path;

import org.jdom.Document;


public final class ConfigContext {

    private final Path jbossHome;
    private final Path configuration;
    private final Document document;

    ConfigContext(Path jbossHome, Path configuration, Document document) {
        this.jbossHome = jbossHome;
        this.configuration = configuration;
        this.document = document;
    }

    public Path getJBossHome() {
        return jbossHome;
    }

    public Path getConfiguration() {
        return configuration;
    }

    public Document getDocument() {
        return document;
    }
}
