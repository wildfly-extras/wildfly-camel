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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ServiceD.class }, immediate = true)
public class ServiceD {

    static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    final AtomicReference<ServiceD1> ref = new AtomicReference<ServiceD1>();
    final CountDownLatch activateLatch = new CountDownLatch(1);
    final CountDownLatch deactivateLatch = new CountDownLatch(1);

    @Activate
    void activate(ComponentContext context) {
        activateLatch.countDown();
    }

    @Deactivate
    void deactivate() {
        deactivateLatch.countDown();
    }

    public boolean awaitActivate(long timeout, TimeUnit unit) throws InterruptedException {
        return activateLatch.await(timeout, unit);
    }

    public boolean awaitDeactivate(long timeout, TimeUnit unit) throws InterruptedException {
        return deactivateLatch.await(timeout, unit);
    }

    @Reference
    void bindServiceD1(ServiceD1 service) {
        ref.set(service);
    }

    void unbindServiceD1(ServiceD1 service) {
        ref.compareAndSet(service, null);
    }

    public ServiceD1 getServiceD1() {
        return ref.get();
    }

    public String doStuff(String msg) {
        ServiceD1 srv = ref.get();
        return name + ":" + srv.doStuff(msg);
    }

    @Override
    public String toString() {
        return name;
    }
}
