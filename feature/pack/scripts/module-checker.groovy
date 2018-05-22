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
        "${this.artifactId}${this.version == "" ? "" : "-${this.version}"}.jar"
    }
}

/** A JBoss Modules dependency graph as seen in $JBOSS_HOME/modules file tree */
class DependencyGraph {

    /** A set of Fuse modules (incl :slot) that all other Fuse modules are supposed to transitively depend on */
    def rootModules = [] as Set

    def modules = [] as Set

    /** A map from module names to sets of dependent module names */
    def dependentsIndex = [:]

    def DependencyGraph(rootModules) {
        this.rootModules = rootModules
    }

    /** Returns @{code true} if the given moduleName:slot conbination is a (possibly transitive) dependency of some
     *  element of rootModules */
    boolean isDependencyOfRootModule(moduleName, slot) {
        return isDependencyOfRootModuleInternal(moduleName +":"+ slot, [] as Set)
    }

    boolean isDependencyOfRootModuleInternal(moduleName, path) {
        if (rootModules.contains(moduleName)) {
            return true;
        }

        def dependents = dependentsIndex[moduleName]
        if (dependents == null) {
            return false
        } else {
            // recurse
            path << moduleName
            for (String dependent : dependents) {
                if (path.contains(dependent)) {
                    // ignore cyclic dependencies
                } else if (isDependencyOfRootModuleInternal(dependent, path)) {
                    return true
                }
            }
        }
        return false;
    }

    boolean containsModule(moduleNameSlot) {
        return modules.contains(moduleNameSlot)
    }

    def addModule(moduleName, slot) {
        modules << (moduleName +":"+ slot)
    }

    def addDependency(dependent, dependentSlot, dependency, dependencySlot) {
        dependentSlot = dependentSlot ?: "main"
        dependencySlot = dependencySlot ?: "main"
        def dependencyKey = dependency +":"+ dependencySlot;
        def dependents = dependentsIndex.get(dependencyKey)
        if (dependents == null) {
            dependentsIndex.put(dependencyKey, dependents = [] as Set)
        }
        dependents << (dependent +":"+ dependentSlot)
    }

}

def paths = [properties.get("wildfly.module.dir"), properties.get("wildfly.camel.module.dir")]

/** A set of WildFly Camel modules that all other WildFly Camel modules are supposed to transitively depend on */
def rootModules = [
    "org.wildfly.extension.camel:main",
    "org.wildfly.extras.config.plugin.camel:main",
    "org.wildfly.extras.patch:main",
    "org.apache.kafka:main" //  KafkaProducerIntegrationTest depends on it see https://github.com/wildfly-extras/wildfly-camel/issues/2539
] as Set

def smarticsFilesPrefix = properties.get("wildfly-camel-feature-pack.basedir") + "/../"
def smarticsDirectories = [
    "${smarticsFilesPrefix}modules/etc/smartics",
    "${smarticsFilesPrefix}extrasA/etc/smartics",
    "${smarticsFilesPrefix}extrasB/etc/smartics",
    "${smarticsFilesPrefix}extrasC/etc/smartics"
]

def modules = []
def duplicateResources = []
def problems = []
def dependencyGraph = new DependencyGraph(rootModules);

// Build up a list of modules and identify duplicates
paths.each { path ->
    def featurePackFile = new File("${path}/../../../../wildfly-feature-pack.xml")
    def featurePack = null
    if (featurePackFile.exists()) {
        featurePack = new XmlParser().parse(featurePackFile)
    }

    new File(path).eachFileRecurse() { file ->
        def parser = new XmlParser()

        if (file.name == "module.xml") {
            moduleXml = parser.parseText(file.getText())

            module = new Module()
            module.name = moduleXml.attribute("name")
            module.slot = moduleXml.attribute("slot") ?: "main"
            module.layer = file.path.contains("layers${File.separator}base") ? "base" : "fuse"
            module.path = "modules/system/layers/${module.layer}${file.parent.replace(path, "")}"
            if (module.layer == "fuse") {
                moduleXml.dependencies.module.each { dep ->
                    dependencyGraph.addDependency(module.name, module.slot, dep.@name, dep.@slot)
                }
                dependencyGraph.addModule(module.name, module.slot)
            }

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
                def version = ""

                // Get the version from the resource string or lookup from feature-pack definition
                if (gavParts.length >= 3) {
                    version = gavParts[2]
                } else {
                    if (featurePack == null) {
                        println ""
                        println "ERROR - Unable to determine version for artifact ${groupId}:${artifactId}. Feature pack definition not found ${featurePackFile.canonicalPath}"
                        println ""
                        fail
                    }

                    def match = featurePack."artifact-versions".artifact.find { artifact ->
                        artifact.@groupId == groupId && artifact.@artifactId == artifactId
                    }

                    if (match == null) {
                        println ""
                        println "ERROR - Could not find artifact reference ${groupId}:${artifactId} in ${featurePackFile.canonicalPath}"
                        println ""
                        fail
                    }

                    version = match.@version
                }

                module.resources << new Resource(groupId, artifactId, version)
            }

            otherModule = modules.find { it.name == module.name && it.slot == module.slot }
            if (otherModule != null) {
                problems << "Duplicate module name and slot detected: ${module.name}:${module.slot}\n\t${module.path}\n\t${otherModule.path}\n"
            }

            modules << module
        }
    }
}

// Make sure we actually discovered some resources across all layers
fuseLayerResourceCount = modules.count {it.layer == "fuse" && it.resources.size > 0}
baseLayerResourceCount = modules.count {it.layer == "base" && it.resources.size > 0}

if (fuseLayerResourceCount == 0 || baseLayerResourceCount == 0) {
    println ""
    println "ERROR - Discovered ${baseLayerResourceCount} resources in layer 'base' and ${fuseLayerResourceCount} resources in layer 'fuse'"
    println ""
    fail("Unable to check module dependencies")
}

// Search for duplicated module resources
modules.findAll { (it.layer == "fuse") }.each { fuseModule ->
    modules.findAll { (it.layer == "base") }.each { baseModule ->
        fuseModule.resources.each { resource ->
            def duplicateResource = fuseModule.findDuplicateResource(baseModule, resource)
            if(duplicateResource != null && !duplicateResources.contains(resource)) {
                duplicateResources << resource
                problems << "Duplicate dependency ${resource.artifactId}\n\t${fuseModule.path}/${resource}\n\t${baseModule.path}/${duplicateResource}\n"
            }
        }
    }
}

// Ban orphan modules
modules.findAll { (it.layer == "fuse") }.each { fuseModule ->
    if (!dependencyGraph.isDependencyOfRootModule(fuseModule.name, fuseModule.slot)) {
        problems << "Orphan module: ${fuseModule.name}:${fuseModule.slot} No relevant WildFly Camel module depends on it and can thus be removed"
    }
}

// Check that all modules from smartics files are materialized in a JBoss module
// and create a list of smartics modules
def smarticsModuleNames = [] as Set
smarticsDirectories.each { dir ->
    new File(dir).eachFile() { smarticsFile ->
        if (smarticsFile.isFile() && smarticsFile.name.endsWith(".xml")) {
            def parser = new XmlParser()
            def smarticsDom = parser.parseText(smarticsFile.getText())
            smarticsDom.module.each { moduleNode ->
                if (moduleNode.@skip != "true") {
                    def key = moduleNode.@name + ":"+ (moduleNode.@slot ?: "main")
                    smarticsModuleNames.add(key)
                    if (!dependencyGraph.containsModule(key)) {
                        String shortPath = smarticsFile.toString().substring(smarticsFilesPrefix.length())
                        problems << "${key}   unused in distro but defined in ${shortPath}"
                    }
                }
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
} else {
    // Write a list of resources so that we can look them up for generating licenses.xml
    def binding = ["modules" : modules]
    def engine = new SimpleTemplateEngine()
    def text = '''
<%
modules.findAll({module -> module.layer == "fuse"}).each {it.resources.each {resource -> println resource}}
%>
'''
    def template = engine.createTemplate(text).make(binding)
    new File("${project.build.directory}/fuse-resources.txt").setText(template.toString().trim())
}
