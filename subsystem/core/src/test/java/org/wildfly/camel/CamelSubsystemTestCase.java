/*
 * #%L
 * Wildfly Camel :: Subsystem
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

package org.wildfly.camel;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.wildfly.extension.camel.parser.CamelExtension;

/**
 * A simple camel subsystem test.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelSubsystemTestCase extends AbstractSubsystemBaseTest {

        public CamelSubsystemTestCase() {
            super(CamelExtension.SUBSYSTEM_NAME, new CamelExtension());
        }

        @Override
        protected String getSubsystemXml() throws IOException {
            return readResource("subsystem-camel-1.0.xml");
        }
}
