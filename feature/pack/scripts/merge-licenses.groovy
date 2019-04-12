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
/**
 * A script to merge the license.xml files and downloaded licenses from the dependency feature pack projects
 */

def dependencyFeaturePackPaths = [
    "${basedir}/../extrasA",
    "${basedir}/../extrasB",
    "${basedir}/../extrasC",
    "${basedir}/../modules",
]
final File outputFile = new File(properties['galleon.licenses.xml.path'])
final File outputDir = outputFile.getParentFile()
outputDir.mkdirs()

def result = null

// Build up a list of modules and identify duplicates
dependencyFeaturePackPaths.eachWithIndex { path, i ->

    final File licensesXmlPath = new File("${path}/target/licenses/licenses.xml")
    assert licensesXmlPath.exists() : "${licensesXmlPath} does not exist; you may want to build the dependency project"

    if (i == 0) {
        result = new XmlParser().parseText(licensesXmlPath.getText())
    } else {
        def toAdd = new XmlParser().parseText(licensesXmlPath.getText())
        toAdd.dependencies.dependency.each{ dep ->
            result.dependencies[0].children().add(dep)
        }
    }

    new File("${path}/src/main/license-resources/licenses").eachFile { licFile ->
        final File dest = new File(outputDir, licFile.getName())
        dest << licFile.bytes
    }

}

result.dependencies[0].children().sort(true) { dep ->
    dep.get("groupId").get(0).text() +":"+ dep.get("artifactId").get(0).text() +":"+ dep.get("version").get(0).text()
}

outputFile.withWriter('UTF-8') { writer ->
    writer.write(groovy.xml.XmlUtil.serialize(result))
}


