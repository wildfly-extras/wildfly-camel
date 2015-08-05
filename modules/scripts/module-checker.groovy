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

/**
 * Script to help ensure that there is minimum dependency duplication between the wildfly-camel 'fuse'
 * module layer and the WildFly 'base' module layer.
 *
 * A module is considered a duplicate if:
 *  - It has the same name and slot combination as another module (in any layer)
 *
 * A dependency is considered a duplicate if:
 *  - It appears in another module (I.e has the same resource name)
 *  - It appears in another module under a different slot, but has the same resource name and version
 */

@EqualsAndHashCode(includes = "name,slot,layer")
class Module {
    String name
    String slot
    String path
    String layer
    def resources = []

    def getResource(def resource) {
        resources.find { resource.equals(it) }
    }

    def isSameSlot(def module) {
        return module.slot.equals(this.slot)
    }

    def findDuplicateResource(def module, def resource) {
        def duplicateResource = module.getResource(resource)

        if (duplicateResource != null ) {
            if (module.isSameSlot(this)) {
                return duplicateResource
            } else if (!module.isSameSlot(this) && duplicateResource.version.equals(resource.version)) {
                return duplicateResource
            }
        }

        return null
    }

    @Override
    public String toString() {
        "${name}:${slot}"
    }
}

@EqualsAndHashCode(includes = "name")
class Resource {
    String name
    String version

    public Resource(String rawName) {
        if (rawName.lastIndexOf("-") > -1) {
            this.name = "${rawName.substring(0, rawName.lastIndexOf("-"))}"
            this.version = rawName.substring(rawName.lastIndexOf("-") + 1, rawName.lastIndexOf("."))
        } else {
            this.name = rawName
        }
    }

    @Override
    public String toString() {
        def version = this.version == "" ? "" : "-${this.version}"
        "${this.name}${version}.jar"
    }
}

def paths = [properties.get("wildfly.module.dir"), properties.get("smartics.module.dir")]
def modules = []
def duplicateResources = []
def problems = []

// Build up a list of modules and identify duplicates
paths.each { path ->
    new File(path).eachFileRecurse() { file ->
        def parser = new XmlParser();

        if (file.name.equals("module.xml")) {
            module = new Module()

            moduleXml = parser.parseText(file.getText())
            moduleXml.resources."resource-root".@path.each { resource ->
                if (resource.endsWith(".jar")) {
                    module.resources << new Resource(resource)
                }
            }

            module.name = moduleXml.attribute("name")
            module.slot = moduleXml.attribute("slot") ?: "main"
            module.layer = file.path.contains("layers/base") ? "base" : "fuse"
            module.path = "modules/system/layers/${module.layer}${file.parent.replace(path, "")}"

            otherModule = modules.find { it.name.equals(module.name) && it.slot.equals(module.slot) }
            if (otherModule != null) {
                problems << "Duplicate module name and slot detected: ${module.name}:${module.slot}\n\t${module.path}\n\t${otherModule.path}\n"
            }

            modules << module
        }
    }
}

// Search for duplicated module resources
modules.findAll { it.layer.equals("fuse") }.each { fuseModule ->
    modules.findAll { it.layer.equals("base") }.each { baseModule ->
        fuseModule.resources.each { resource ->
            def duplicateResource = fuseModule.findDuplicateResource(baseModule, resource)
            if(duplicateResource != null && !duplicateResources.contains(resource)) {
                duplicateResources << resource
                problems << "Duplicate dependency ${resource.name}\n\t${fuseModule.path}/${resource}\n\t${baseModule.path}/${duplicateResource}\n"
            }
        }
    }
}

// Output detected problems
if (problems.size() > 0) {
    println ""
    println "MODULE DEPENDENCY ERRORS DETECTED!!"
    println ""

    problems.each { problem ->
        println problem
    }

    println ""
    fail("Module dependency conflicts were detected. Please fix your module dependencies.")
}
