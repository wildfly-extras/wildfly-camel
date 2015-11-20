/*
 * #%L
 * Gravia :: Integration Tests :: Common
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
package org.wildfly.camel.test.compatibility.subA;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

@Component(service = { ServiceD1.class })
public class ServiceD1 {

    static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    final CountDownLatch modifiedLatch = new CountDownLatch(1);
    private volatile Map<String, ?> config;

    @Activate
    void activate(ComponentContext context, Map<String, ?> config) {
        this.config = config;
    }

    @Modified
    void modified(Map<String, ?> config) {
        this.config = config;
        modifiedLatch.countDown();
    }

    @Deactivate
    void deactivate() {
    }

    public boolean awaitModified(long timeout, TimeUnit unit) throws InterruptedException {
        return modifiedLatch.await(timeout, unit);
    }

    public String doStuff(String msg) {
        Object fooval = config.get("foo");
        return name + ":" + fooval + ":" + msg;
    }

    @Override
    public String toString() {
        return name;
    }
}
