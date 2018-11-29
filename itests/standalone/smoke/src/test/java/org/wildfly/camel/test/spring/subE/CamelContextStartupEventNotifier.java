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

import javax.ejb.Stateless;

import org.apache.camel.impl.event.CamelContextStartedEvent;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;

@Stateless
public class CamelContextStartupEventNotifier extends EventNotifierSupport {

    private long startupTime;

    @Override
	public void notify(CamelEvent event) throws Exception {
        if (event instanceof CamelContextStartedEvent) {
            this.startupTime = System.currentTimeMillis();
        }
    }

    public long getStartupTime() {
        return startupTime;
    }
}
