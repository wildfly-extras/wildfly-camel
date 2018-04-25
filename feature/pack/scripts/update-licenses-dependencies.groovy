#!/usr/bin/groovy
/*
* #%L
* Wildfly Camel
* %%
* Copyright (C) 2013 - 2018 RedHat
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

String[] fuseJars = new File("${project.build.directory}/fuse-resources.txt")
def skip = properties.get("skip-update-licenses-xml")
def licenseOutputDir = properties.get("license.output.dir")
def licenseSourceDir = properties.get("license.dir")
def licenseOutputXmlFile = new File("${licenseOutputDir}/licenses.xml")
def licenseSourceXmlFile = new File("${licenseSourceDir}/licenses.xml")

if (skip != null) {
    return
}

if (licenseOutputXmlFile.exists()) {
    licenseXml = new XmlParser().parse(licenseOutputXmlFile)
    other = new XmlParser().parse(licenseSourceXmlFile)

    licenseXml.dependencies.dependency.each { dependency ->
        String jarName = "${dependency.artifactId.text()}-${dependency.version.text()}.jar"

        // Remove all license entries for dependencies that are not in the fuse module layer
        def matchedJars = fuseJars.find({ jar -> jar.matches(jarName) })
        if (matchedJars == null) {
            dependency.replaceNode {}
        }
    }

    def urls = [:]
    licenseXml.dependencies.dependency.licenses.license.each { license ->
        String licenseName = license.name.text()
        String licenseFileName = ""
        String licenseUrlFileName = ""

        String licenseUrl = license.url.text()
        if (licenseUrl == null || licenseUrl.isEmpty()) {
            return
        }

        try {
            licenseUrlFileName = new File(new URL(licenseUrl).path).name
        } catch (MalformedURLException e) {
            return
        }

        if (urls[licenseUrl]) {
            licenseFileName = urls[licenseUrl]
        } else {
            if (licenseName != null && !licenseName.isEmpty()) {
                licenseFileName = licenseName.replaceAll( "/", "_" ) + " - " + licenseUrlFileName
            }

            int idx = licenseFileName.lastIndexOf(".")
            if (idx == -1 || idx > (licenseFileName.length() - 3 )) {
                licenseFileName += ".txt"
            }

            licenseFileName = licenseFileName.toLowerCase()
            urls[licenseUrl] = licenseFileName
        }

        license.append(new Node(null, "file", licenseFileName))
    }

    // Output modified XML
    def printer = new XmlNodePrinter(new PrintWriter(new FileWriter(licenseOutputXmlFile)))
    printer.preserveWhitespace = true
    printer.print(licenseXml)
}
