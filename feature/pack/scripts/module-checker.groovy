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
        this.version = version.replaceAll("(-|\\.)redhat.*", "")
    }

    @Override
    String toString() {
        "${this.groupId}:${this.artifactId}"
    }
}

// A JBoss Modules dependency graph as seen in $JBOSS_HOME/modules file tree
class DependencyGraph {

    // A set of Fuse modules (incl :slot) that all other Fuse modules are supposed to transitively depend on
    def rootModules = [] as Set

    def modules = [] as Set

    // A map from module names to sets of dependent module names
    def dependentsIndex = [:]

    def DependencyGraph(rootModules) {
        this.rootModules = rootModules
    }

    // Returns @{code true} if the given moduleName:slot conbination is a (possibly transitive) dependency of some element of rootModules
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
def wfcHome = properties.get("wildfly.camel.dir")
def paths = ["${wfcHome}/modules/system/layers/base", "${wfcHome}/modules/system/layers/fuse"]

// A set of WildFly Camel modules that all other WildFly Camel modules are supposed to transitively depend on
def rootModules = [
    "org.wildfly.extension.camel:main",
    "org.wildfly.extras.config.plugin.camel:main",
    "org.wildfly.extras.config:main"
] as Set

def allowedDuplicateModules = [
] as Set

/**
 * Modules that exist in Wildfly, but are for portability reasons provided in alterniative slots
 *
 * [ENTESB-10879] Fuse/EAP Compatibility Contract
 */
def allowedDuplicateArtifacts = [
    "com.fasterxml.jackson.datatype:jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype:jackson-datatype-jsr310",
    "com.fasterxml.jackson.jaxrs:jackson-jaxrs-base",
    "com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider",
    "com.fasterxml.jackson.module:jackson-module-jaxb-annotations",
    "com.github.ben-manes.caffeine:caffeine",
    "com.github.spullara.mustache.java:compiler",
    "com.googlecode.javaewah:JavaEWAH",
    "com.google.code.gson:gson",
    "com.jcraft:jsch",
    "com.microsoft.azure:azure-storage",
    "com.squareup.okhttp3:okhttp",
    "com.squareup.okio:okio",
    "commons-beanutils:commons-beanutils",
    "commons-codec:commons-codec",
    "commons-collections:commons-collections",
    "commons-lang:commons-lang",
    "io.opentracing.contrib:opentracing-tracerresolver",
    "io.opentracing:opentracing-api",
    "io.opentracing:opentracing-noop",
    "io.reactivex.rxjava2:rxjava",
    "jaxen:jaxen",
    "joda-time:joda-time",
    "org.apache.commons:commons-lang3",
    "org.apache.httpcomponents:httpasyncclient",
    "org.apache.santuario:xmlsec",
    "org.apache.velocity:velocity-engine-core",
    "org.apache.thrift:libthrift",
    "org.codehaus.jackson:jackson-core-asl",
    "org.codehaus.jackson:jackson-jaxrs",
    "org.codehaus.jackson:jackson-mapper-asl",
    "org.codehaus.jackson:jackson-xc",
    "org.codehaus.woodstox:stax2-api",
    "org.eclipse.jdt.core.compiler:ecj",
    "org.eclipse.jgit:org.eclipse.jgit",
    "org.jdom:jdom",
    "org.jsoup:jsoup",
    "org.ow2.asm:asm",
    "org.reactivestreams:reactive-streams",
    "org.yaml:snakeyaml"
] as Set
def allowedOrphanArtifacts = [
	"org.fusesource.camel.component.sap",
	"com.sap.conn.jco"
] as Set


def smarticsFilesPrefix = properties.get("wildfly-camel-feature-pack.basedir") + "/../"
def smarticsDirectories = [
    "${smarticsFilesPrefix}common/etc/smartics",
    "${smarticsFilesPrefix}modules/etc/smartics",
    "${smarticsFilesPrefix}extrasA/etc/smartics",
    "${smarticsFilesPrefix}extrasB/etc/smartics",
    "${smarticsFilesPrefix}extrasC/etc/smartics"
]

// We ignore modules in smartics XML files having skip="true" unless the given file is explicitly present in this list
def smarticsFilesIgnoreSkip = [
] as Set
def smarticsManagedDirectories = [
    "${smarticsFilesPrefix}modules/etc/managed"
]

def modules = []
def duplicateResources = []
def problems = []
def dependencyGraph = new DependencyGraph(rootModules);

// Build up a list of modules and identify duplicates
paths.each { path ->

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

                // Get the version from the resource string
                if (gavParts.length >= 3) {
                    version = gavParts[2]
	                module.resources << new Resource(groupId, artifactId, version)
                } else {
                    problems << "Unable to determine version for artifact ${groupId}:${artifactId}"
                }
            }

            otherModule = modules.find { it.name == module.name && it.slot == module.slot }
            final String nameSlot = "${module.name}:${module.slot}"
            if (otherModule != null && !allowedDuplicateModules.contains(nameSlot)) {
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
                final String ga = "${resource.groupId}:${resource.artifactId}"
                if (!allowedDuplicateArtifacts.contains(ga)) {
                    duplicateResources << resource
                    problems << "Duplicate dependency ${resource.artifactId}\n\t${fuseModule.path}/${resource}\n\t${baseModule.path}/${duplicateResource}\n"
                }
            }
        }
    }
}

// Ban orphan modules
modules.findAll { (it.layer == "fuse") }.each { fuseModule ->
    if (!dependencyGraph.isDependencyOfRootModule(fuseModule.name, fuseModule.slot) && !allowedOrphanArtifacts.contains(fuseModule.name)) {
        problems << "Orphan module: ${fuseModule.name}:${fuseModule.slot} No relevant WildFly Camel module depends on it and can thus be removed"
    }
}

// Check that all modules from smartics files are materialized in a JBoss module
// and create a list of smartics modules
def smarticsModuleNames = [] as Set
smarticsDirectories.each { dir ->
    new File(dir).eachFile() { smarticsFile ->
        if (smarticsFile.isFile() && smarticsFile.name.endsWith(".xml")) {
            def ignoreSkip = smarticsFilesIgnoreSkip.contains(smarticsFile.path)
            def parser = new XmlParser()
            def smarticsDom = parser.parseText(smarticsFile.getText())
            smarticsDom.module.each { moduleNode ->
                if (ignoreSkip || moduleNode.@skip != "true") {
                    def key = moduleNode.@name + ":" + (moduleNode.@slot ? moduleNode.@slot : "main")
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

// add managed files to smarticsModuleNames
smarticsManagedDirectories.each { path ->
    new File(path).eachFileRecurse() { file ->
        if (file.name == "module.xml") {
            def parser = new XmlParser()
            moduleXml = parser.parseText(file.getText())
            def key = moduleXml.@name + ":"+ (moduleXml.@slot ?: "main")
            smarticsModuleNames.add(key)
        }
    }
}

// Check that each fuse module is explicitly defined in a smartics file
modules.findAll { (it.layer == "fuse") }.each { fuseModule ->
    if (!smarticsModuleNames.contains(fuseModule.name + ":"+ fuseModule.slot)) {
        problems << "Module ${fuseModule.name}:${fuseModule.slot} is not defined in any smartics xml file"
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
