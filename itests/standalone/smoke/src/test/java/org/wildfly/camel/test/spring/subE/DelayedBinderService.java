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
package org.wildfly.camel.test.spring.subE;

import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link BinderService} that delays the availability of a JNDI binding
 */
public class DelayedBinderService extends BinderService {

    private static final long SLEEP_DELAY = 3000;
    private static final Logger LOG = LoggerFactory.getLogger(DelayedBinderServiceActivator.class);
    private long sleepStart;

    public DelayedBinderService(String name) {
        super(name);
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            LOG.info("DelayedBinderService.start() sleep begin");
            sleepStart = System.currentTimeMillis();
            Thread.sleep(SLEEP_DELAY);
            LOG.info("DelayedBinderService.start() sleep end");
            super.start(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long getSleepStart() {
        return sleepStart;
    }

    public long getSleepDelay() {
        return SLEEP_DELAY;
    }
}
