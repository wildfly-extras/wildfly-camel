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
import groovy.transform.EqualsAndHashCode
import groovy.text.SimpleTemplateEngine

/**
 * A script that collects the list of artifacts that need an entry in licenses.xml file.
 */

@EqualsAndHashCode(includes = "name,slot,layer")
class Module {
    String name
    String slot
    String path
    String layer
    def resources = []

    def getResource(resource) {
        resources.find { (resource == it) }
    }

    def isSameSlot(module) {
        return module.slot == this.slot
    }

    def findDuplicateResource(module, resource) {
        def duplicateResource = module.getResource(resource)

        if (duplicateResource != null ) {
            if (resource.groupId != duplicateResource.groupId) {
                return null
            } else if (module.isSameSlot(this)) {
                return duplicateResource
            } else if (!module.isSameSlot(this) && duplicateResource.version == resource.version) {
                return duplicateResource
            }
        }

        return null
    }

    @Override
    String toString() {
        "${name}:${slot}"
    }
}

@EqualsAndHashCode(includes = "artifactId")
class Resource {
    String groupId
    String artifactId
    String version

    Resource(groupId, artifactId, version) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
    }

    @Override
    String toString() {
        "${this.groupId}:${this.artifactId}"
    }
}

def paths = [
    "${basedir}/src/main/resources/modules"
]

def modules = []

// Build up a list of modules
paths.each { path ->
    new File(path).eachFileRecurse() { file ->

        if (file.name == "module.xml") {
            def parser = new XmlParser()
            moduleXml = parser.parseText(file.getText())

            module = new Module()
            module.name = moduleXml.attribute("name")
            module.slot = moduleXml.attribute("slot") ?: "main"
            module.layer = file.path.contains("layers${File.separator}base") ? "base" : "fuse"
            module.path = "modules/system/layers/${module.layer}${file.parent.replace(path, "")}"


            // Process standard <resource-root> elements
            moduleXml.resources."resource-root".@path.each { resource ->
                if (resource.endsWith(".jar")) {
                    def artifactId = resource.substring(0, resource.lastIndexOf("."))
                    def version = ""
                    if (resource.lastIndexOf("-") > -1) {
                        artifactId = "${resource.substring(0, resource.lastIndexOf("-"))}"
                        version= resource.substring(resource.lastIndexOf("-") + 1, resource.lastIndexOf("."))
                    }
                    module.resources << new Resource("unknown", artifactId, version)
                }
            }

            // Process feature pack <artifact> elements
            moduleXml.resources.artifact.@name.each { resource ->
                def gavParts = resource.replaceAll('(\\$|\\{|\\}|\\?jandex)', '').split(":")
                def groupId = gavParts[0]
                def artifactId = gavParts[1]
                module.resources << new Resource(groupId, artifactId, "")
            }

            modules << module
        }
    }
}

// Make sure we actually discovered some resources across all layers
fuseLayerResourceCount = modules.count {it.layer == "fuse" && it.resources.size > 0}

if (fuseLayerResourceCount == 0) {
    println ""
    println "ERROR - Discovered ${fuseLayerResourceCount} resources in layer 'fuse'"
    println ""
    fail("Unable to check module dependencies")
}

// Write a list of resources so that we can look them up for generating licenses.xml
final File fuseResources = new File("${project.build.directory}/fuse-resources.txt")
fuseResources.getParentFile().mkdirs()
fuseResources.withWriter('UTF-8') { writer ->
    modules.findAll({module -> module.layer == "fuse"}).each { module ->
        module.resources.each { resource ->
            writer.write('include gaPattern \\Q' + resource + '\\E\n')
        }
    }
}
