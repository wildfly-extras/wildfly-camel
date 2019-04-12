#!/usr/bin/groovy
/*
 * #%L
 * Wildfly Camel
 * %%
 * Copyright (C) 2013 - 2019 RedHat
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

final Set<String> artifacts = new HashSet<String>()
final String linePrefix = "include gaPattern \\Q"
final String lineSuffix = "\\E"
new File("${basedir}/target/fuse-resources.txt").eachLine { line ->
    assert line.startsWith(linePrefix)
    line = line.substring(linePrefix.length())
    assert line.endsWith(lineSuffix)
    line = line.substring(0, line.length() - lineSuffix.length())
    artifacts.add(line)
}
final File licensesXmlPath = new File("${basedir}/target/licenses/licenses.xml")
def licensesDom = new XmlParser().parseText(licensesXmlPath.getText())
licensesDom.dependencies.dependency.each { dep ->
    final String key = dep.get("groupId").get(0).text() +":"+ dep.get("artifactId").get(0).text();
    assert artifacts.contains(key) : "There is a superfluous dependency in licenses.xml: "+ key +"\n\nYou may want to build with -Dupdate-licenses-xml\n\n"
    artifacts.remove(key)
}

assert artifacts.isEmpty() : "Artifacts missing in licenses.xml: "+ artifacts +"\n\nYou may want to build with -Dupdate-licenses-xml\n\n"
