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
package org.wildfly.camel.test.spring.subD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogBean {

    public void log(String level, String message) {
        // FIXME: Ideally this should be initialized as a class (static) instance variable but #1883 prohibits this
        Logger log = LoggerFactory.getLogger(LogBean.class.getName());

        switch(level) {
            case "DEBUG":
                log.debug(message);
                break;
            case "ERROR":
                log.error(message);
                break;
            case "INFO":
                log.info(message);
                break;
            case "TRACE":
                log.trace(message);
                break;
            case "WARN":
                log.warn(message);
                break;
            default:
                log.info(message);
                break;
        }
    }
}
