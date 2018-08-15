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

import java.util.regex.Pattern;

public class LayerConfig {

    public static LayerConfig FUSE_LAYER = new LayerConfig("fuse", LayerConfig.Type.INSTALLING, -10);

    static public enum Type {
        INSTALLING, OPTIONAL, REQUIRED,
    }

    public final String name;
    public final Pattern pattern;
    public final Type type;
    public final int priority;

    public LayerConfig(String nameAndVersion, Type type, int priority) {
        this.name = nameAndVersion;
        this.type = type;
        this.priority = priority;

        String shortName = nameAndVersion;
        int i = nameAndVersion.indexOf("_");
        if (i > 0) {
            shortName = nameAndVersion.substring(0, i);
        }
        this.pattern = Pattern.compile("^" + Pattern.quote(shortName) + "(_.*)?$");
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof LayerConfig)) return false;
        LayerConfig other = (LayerConfig) obj;
        return name.equals(other.name) && priority == other.priority && type.equals(other.type);
    }

    @Override
    public String toString() {
        return "Layer[name=" + name + ",prio=" + priority + ",type=" + type + "]";
    }

}
