/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.camel;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarFile;

import org.jboss.modules.AbstractResourceLoader;
import org.jboss.modules.ClassSpec;
import org.jboss.modules.IterableResourceLoader;
import org.jboss.modules.PackageSpec;
import org.jboss.modules.Resource;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class JavaArchiveResourceLoader extends AbstractResourceLoader implements IterableResourceLoader {

    private final IterableResourceLoader delegate;

    public JavaArchiveResourceLoader(JavaArchive archive) throws IOException {
        // Access the package protected constructor
        try {
            Class<?> clazz = getClass().getClassLoader().loadClass("org.jboss.modules.JarFileResourceLoader");
            Constructor<?> ctor = clazz.getDeclaredConstructor(new Class[] {String.class, JarFile.class});
            ctor.setAccessible(true);
            delegate = (IterableResourceLoader) ctor.newInstance(archive.getName(), toJarFile(archive));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String getRootName() {
        return delegate.getRootName();
    }

    public ClassSpec getClassSpec(String fileName) throws IOException {
        return delegate.getClassSpec(fileName);
    }

    public PackageSpec getPackageSpec(String name) throws IOException {
        return delegate.getPackageSpec(name);
    }

    public Resource getResource(String name) {
        return delegate.getResource(name);
    }

    public String getLibrary(String name) {
        return delegate.getLibrary(name);
    }

    public Collection<String> getPaths() {
        return delegate.getPaths();
    }

    @Override
    public Iterator<Resource> iterateResources(String startPath, boolean recursive) {
        return delegate.iterateResources(startPath, recursive);
    }

    private JarFile toJarFile(JavaArchive archive) throws IOException {
        ZipExporter exporter = archive.as(ZipExporter.class);
        File targetFile = new File("target/shrinkwrap/" + archive.getName());
        targetFile.getParentFile().mkdirs();
        exporter.exportTo(targetFile, true);
        return new JarFile(targetFile);
    }
}
