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
import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.apache.maven.artifact.resolver.ArtifactResolutionResult
import org.apache.maven.artifact.resolver.filter.ArtifactFilter
import org.apache.maven.repository.RepositorySystem

/**
 * Enables searching across specific combinations of Maven GAV coordinates
 */
enum ArtifactSearchParameters {
    BASIC(["groupId", "artifactId"]),
    EXTENDED(["groupId", "artifactId", "version"])

    private searchParameters;

    private ArtifactSearchParameters(def searchParameters) {
        this.searchParameters = searchParameters
    }

    def getParameters() {
        return this.searchParameters
    }
}

@EqualsAndHashCode(excludes = "artifacts")
class Module {
    String name
    String slot
    def artifacts = []

    /**
     * Checks whether a module is associated with a particular artifact which matches a set of search parameters
     * @param artifact The artifact to search for
     * @param searchParameters The criteria to use for searching the artifact
     * @return True if a matching artifact was found, false if not
     */
    def hasArtifact(def artifact, ArtifactSearchParameters searchParameters) {
        return artifacts.find { moduleArtifact ->
            boolean match = true
            searchParameters.getParameters().each { parameter ->
                match &= moduleArtifact[parameter].equals(artifact[parameter])
            }
            return match
        }
    }

    @Override
    public String toString() {
        "${name}:${slot}"
    }
}

/**
 * Builds a list of module names, slot ids and resources from all module.xml files
 * from the given path location
 *
 * @param path The file path to the location of application server module XML definitions
 * @param artifactReferences A list of artifacts to cross reference with module resource attribute values
 * @return List of Module objects
 */
def getModules(def path, def artifactReferences, def ignoredDependencies) {
    modules = []

    new File(path).eachFileRecurse() { file ->
        if (file.name.equals("module.xml")) {
            module = new Module()
            moduleXml = new XmlParser().parseText(file.getText())

            // For each module resource, see if there's a Maven artifact with the same file name
            moduleXml.resources."resource-root".@path.each { resource ->
                artifactReferences.each { artifact ->
                    if (!isIgnoredArtifact(artifact, ignoredDependencies)) {
                        File artifactFile = artifact.getFile()
                        if (artifactFile != null && artifactFile.getName().equals(resource)) {
                            module.artifacts << artifact
                        }
                    }
                }
            }

            module.name = moduleXml.attribute("name")
            module.slot = moduleXml.attribute("slot") ?: "main"
            modules << module
        }
    }

    modules
}

/**
 * Obtains a list of dependencies for the target application server
 *
 * @return List of application server build dependencies
 */
def getAppServerDependencies() {
    repository = container.lookup(RepositorySystem.class)
    group = properties.get("appserver.groupId")
    artifiactId = properties.get("appserver.artifactId")
    version = properties.get("appserver.version")

    Artifact artifact = repository.createArtifact(group, artifiactId, version, "pom");

    ArtifactResolutionRequest request = new ArtifactResolutionRequest();
    request.setArtifact(artifact);
    request.setResolveRoot(true).setResolveTransitively(true);
    request.setServers(session.getRequest().getServers());
    request.setMirrors(session.getRequest().getMirrors());
    request.setProxies(session.getRequest().getProxies());
    request.setLocalRepository(session.getLocalRepository());

    def remoteRepositories = session.getRequest().getRemoteRepositories()
    remoteRepositories.addAll(project.remoteArtifactRepositories)

    request.setRemoteRepositories(remoteRepositories);
    request.setResolutionFilter(new ArtifactFilter() {
        @Override
        public boolean include(final Artifact a) {
            return a.getType().equals("jar") && a.getScope().equals("compile");
        }
    });

    ArtifactResolutionResult resolutionResult = repository.resolve(request);
    resolutionResult.getArtifacts();
}

/**
 * Checks whether an artifact is in the list of ignored dependencies
 *
 * @Param artifact The artifact to check if ignored
 * @Param ignoredDependencies List of ignored dependencies to check
 * @return Matching ignored artifact. Null if no match is found
 */
def isIgnoredArtifact(def artifact, def ignoredDependencies) {
    ignoredDependencies.dependency.find {
        it.groupId.text().equals(artifact.groupId) && it.artifactId.text().equals(artifact.artifactId)
    }
}

/*
 * Script execution starts here...
 */
problems = []
ignoredDependencies = []

if (properties.ignoredDependencies != null) {
    ignoredDependencies = new XmlParser().parseText(properties.ignoredDependencies)
}

String wildFlyModulePath = properties.get("wildfly.module.dir")
String wildFlyCamelModulePath = properties.get("smartics.module.dir")
wildFlyModules = getModules(wildFlyModulePath, (LinkedHashSet) getAppServerDependencies(), ignoredDependencies)
wildFlyCamelModules = getModules(wildFlyCamelModulePath, (LinkedHashSet) project.getArtifacts(), ignoredDependencies)

wildFlyCamelModules.each { module ->
    // Check for duplicate module name / slot id combinations
    if (wildFlyModules.contains(module)) {
        problems << "Duplicate module name and slot detected: ${module.name}:${module.slot}"
    }

    /*
     * Check for duplicate dependencies across all WildFly-Camel modules.
     *
     * In theory this should not be possible as Maven & Smartics plugin will prevent this.
     *
     * Check anyway in case that something has gone wrong...
     */
    module.artifacts.each { artifact ->
        match = wildFlyCamelModules.findAll {
            it.name != module.name && it.hasArtifact(artifact, ArtifactSearchParameters.EXTENDED)
        }

        match.each {
            problems << "Duplicate dependeny ${artifact.groupId}:${artifact.artifactId} detected in module: ${it}"
        }
    }
}

// Check for dependencies in WildFly-Camel modules that are duplicated in modules provided by the app server
wildFlyCamelModules.each { wildFlyCamelModule ->
    wildFlyCamelModule.artifacts.each { artifact ->
        wildFlyModules.each { wildFlyModule ->
            if (wildFlyModule.hasArtifact(artifact, ArtifactSearchParameters.BASIC)) {
                problems << "Duplicate dependency ${artifact.groupId}:${artifact.artifactId} detected in modules: ${wildFlyCamelModule} and ${wildFlyModule}"
            }
        }
    }
}

// Output detected problems
if (problems.size() > 0) {
    println ""
    println "DEPENDENCY ERRORS DETECTED!!"
    println ""

    problems.each { problem ->
        println problem
    }

    println ""
    fail("Module dependency conflicts were detected. Please fix your module dependencies.")
}
