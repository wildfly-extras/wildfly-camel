/*
 * #%L
 * Gravia :: Resource
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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

package org.wildfly.camel.test.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple manifest builder.
 *
 * @author thomas.diesler@jboss.com
 * @since 08-Mar-2010
 */
public final class ManifestBuilder {

    public static final Logger LOGGER = LoggerFactory.getLogger(ManifestBuilder.class);

    public enum Type {
        String,
        Version,
        VersionRange,
        Long,
        Double
    }

    private List<String> lines = new ArrayList<String>();
    private Manifest manifest;

    public ManifestBuilder() {
        this("1.0");
    }

    public ManifestBuilder(String manifestVersion) {
        if (manifestVersion != null) {
            append(Attributes.Name.MANIFEST_VERSION + ": " + manifestVersion);
        }
    }

    public ManifestBuilder addManifestHeader(String key, String value) {
        append(key + ": " + value);
        return this;
    }

    public Manifest getManifest() {
        if (manifest == null) {
            StringWriter out = new StringWriter();
            PrintWriter pw = new PrintWriter(out);
            for(String line : lines) {
                byte[] bytes = line.getBytes();
                while (bytes.length >= 512) {
                    byte[] head = Arrays.copyOf(bytes, 256);
                    bytes = Arrays.copyOfRange(bytes, 256, bytes.length);
                    pw.println(new String(head));
                    pw.print(" ");
                }
                pw.println(new String(bytes));
            }

            String content = out.toString();
            if (LOGGER.isTraceEnabled())
                LOGGER.trace(content);

            try {
                manifest = new Manifest(new ByteArrayInputStream(content.getBytes()));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot create manifest", ex);
            }
        }
        return manifest;
    }

    public InputStream openStream() {
        Manifest manifest = getManifest();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            manifest.write(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot provide manifest input stream", ex);
        }
    }

    public void append(String line) {
        if (manifest != null)
            throw new IllegalStateException("Cannot append to existing manifest");

        lines.add(line);
    }
}
