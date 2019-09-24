/*
 * #%L
 * Fuse EAP :: Config
 * %%
 * Copyright (C) 2015 RedHat
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
package org.wildfly.extras.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Namespace;
import org.wildfly.extras.config.internal.IllegalArgumentAssertion;

public class NamespaceRegistry {
    private Map<String, List<Namespace>> namespaces = new LinkedHashMap<>();

    public void registerNamespace(String namespace, String version) {
        if (!namespaces.containsKey(namespace)) {
            namespaces.put(namespace, new ArrayList<Namespace>());
        }
        namespaces.get(namespace).add(Namespace.getNamespace(namespace + ":" + version));
    }

    public Namespace[] getNamespaces(String namespace) {
        IllegalArgumentAssertion.assertTrue(namespaces.containsKey(namespace), "Unsupported namespace: " + namespace);
        List<Namespace> namespaces = this.namespaces.get(namespace);
        return namespaces.toArray(new Namespace[namespaces.size()]);
    }
}
