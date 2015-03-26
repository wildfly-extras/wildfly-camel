#!/usr/bin/groovy
/*
* #%L
* Wildfly Camel
* %%
* Copyright (C) 2013 - 2015 RedHat
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

def jarList = []
def separator = properties.get("file.separator")
def moduleDir = properties.get("smartics.module.dir").replace("/", separator)

new File("${project.build.directory}/wildfly-patch/modules").eachFileRecurse() { file ->
    if(file.getName().endsWith(".jar")) {
        jarList << file.getAbsolutePath().replace(moduleDir, "").replace(separator, "/")
    }
}

jarList.sort({ a, b ->
    a <=> b
})

new File("${project.basedir}/etc/baseline/module-list.txt").withWriter { out ->
    jarList.each {
        out.writeLine(it)
    }
}
