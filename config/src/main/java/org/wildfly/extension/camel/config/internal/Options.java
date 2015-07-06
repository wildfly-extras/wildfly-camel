/*
 * #%L
 * Fuse Patch :: Core
 * %%
 * Copyright (C) 2015 Private
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
package org.wildfly.extension.camel.config.internal;

import org.kohsuke.args4j.Option;

final class Options {

    @Option(name = "--help", help = true)
    boolean help;

    @Option(name = "--configs", usage = "The configurations to enable/disable")
    String configs;

    @Option(name = "--enable", forbids = { "--disable" }, usage = "Enable the given configurations")
    boolean enable;

    @Option(name = "--disable", forbids = { "--enable" }, usage = "Disable the given configurations")
    boolean disable;
}
