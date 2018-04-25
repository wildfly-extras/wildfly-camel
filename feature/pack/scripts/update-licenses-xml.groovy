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
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

def skip = properties.get("skip-update-licenses-xml")
def licenseDir = properties.get("license.dir")
def licenseXmlFile = new File("${licenseDir}/licenses.xml")
String[] fuseJars = new File("${project.build.directory}/fuse-resources.txt")
String[] licenseSupplementary = new File("${licenseDir}/license-supplementary.txt")

if (skip != null) {
    return
}

if (licenseXmlFile.exists()) {
    licenseXml = new XmlParser().parse(licenseXmlFile)

    licenseXml.dependencies.dependency.each { dependency ->
        if (dependency.version.size() == 0) {
            return
        }

        String jarName = "${dependency.artifactId.text()}-${dependency.version.text()}.jar"

        // Remove all license entries for dependencies that are not in the fuse module layer
        def matchedJars = fuseJars.find({jar -> jar.matches(jarName)})
        if (matchedJars == null) {
            dependency.replaceNode {}
        }

        // Strip versions
        dependency.version.replaceNode {}
    }

    // Fix broken license info
    licenseSupplementary.each {
        def licenseDetail = it.split("\\|")

        def matchedDependency = licenseXml.dependencies.dependency.find { dep ->
            return dep.groupId.text() == licenseDetail[0] && dep.artifactId.text() == licenseDetail[1]
        }

        if (matchedDependency == null) {
            println "WARN - Skipping redundant entry '${licenseDetail[0]}:${licenseDetail[1]}' in license-supplementary.txt"
            return
        }

        if (matchedDependency.licenses.license.size() > 0) {
            def matchedLicense = matchedDependency.licenses.license.find { license ->
                return license.name.text() == licenseDetail[2]
            }

            if (matchedLicense != null && matchedLicense.url.text() != null) {
                matchedLicense.url[0].setValue(licenseDetail[3])
            }
        } else {
            Node licenseNode = new Node(null, "license")
            Node nameNode = new Node (licenseNode, "name", licenseDetail[2])
            Node urlNode = new Node (licenseNode, "url", licenseDetail[3])
            matchedDependency.licenses[0].append(licenseNode)
        }
    }

    // Clean up file names
    new File("${licenseDir}/licenses").eachFile { downloadedLicense ->
        String parent = downloadedLicense.parent
        String name = sanitizeFileName(downloadedLicense.name)
        downloadedLicense.renameTo("${parent}/${name}")
    }

    // Process licenses
    def licenses = []
    def urls = [:]
    licenseXml.dependencies.dependency.licenses.license.each { license ->
        String artifactId = license.parent().parent().artifactId.text()
        String licenseName = license.name.text()
        String licenseFileName = ""
        String licenseUrlFileName = ""

        // Fix broken URLs
        switch (license.url.text()) {
            case "http://www.quickfixj.org/documentation/license.html":
                license.url[0].setValue("https://www.quickfixj.org/documentation/license.html")
                break
            case "http://www.mozilla.org/MPL/MPL-1.1.txt":
                license.url[0].setValue("https://www.mozilla.org/en-US/MPL/1.1/")
                break
        }

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

            licenseFileName = sanitizeFileName(licenseFileName.toLowerCase())
            urls[licenseUrl] = licenseFileName
        }

        File file = new File("${licenseDir}/licenses/${licenseFileName}")
        if (!file.exists()) {
            file.write(downloadLicense(licenseUrl))
        }

        if (file.exists()) {
            String text = file.text

            // license-maven-plugin may fail to download licenses for HTTP -> HTTPS redirects
            if (text.contains("Moved Permanently") || text.isEmpty()) {
                text = downloadLicense(licenseUrl)
            }

            // Clean up any bad language
            if (text.matches(/(?ms).*F[a-z]{2}k.*/)) {
                text = text.replaceAll(/(?ms)(F[a-z]{2}k)/, "Fu*k")
            }

            // Add a file node so the XSL can reference it
            if (license.file.size() == 0) {
                license.append(new Node(null, "file", sanitizeFileName(licenseFileName)))
            }

            file.write(text)
        }

        if (!licenses.contains(licenseFileName)) {
            licenses << "${licenseFileName}"
        }
    }

    new File("${licenseDir}/licenses").eachFile { downloadedLicense ->
        if (downloadedLicense.name != "licenses.xml") {
            def match = licenses.find({license -> license == downloadedLicense.name})
            if (match == null) {
                // Delete redundant licenses
                downloadedLicense.delete()
            }
        }
    }

    // Add a generic public license
    def publicLicense = new File("${licenseDir}/licenses/public-domain.txt")
    if (!publicLicense.exists()) {
        publicLicense.write("Being in the public domain is not a license; rather, it means the material is not copyrighted and no license is needed.")
    }

    // Output modified XML
    def printer = new XmlNodePrinter(new PrintWriter(new FileWriter(licenseXmlFile)))
    printer.preserveWhitespace = true
    printer.print(licenseXml)
} else {
    fail("Unable to find licenses file: ${licenseXmlFile.path}")
}

def downloadLicense(String licenseUrl) {
    HttpGet get = new HttpGet(licenseUrl)
    CloseableHttpClient client = HttpClients.createDefault()
    try {
        println "INFO - Download ${licenseUrl}"
        HttpResponse response = client.execute(get)
        if (response.statusLine.statusCode < 400 ) {
            return EntityUtils.toString(response.getEntity())
        }
    } finally {
        client.close()
    }
}

def sanitizeFileName(String text) {
    return text.replaceAll("[^a-zA-Z0-9\\.\\- ]","").replaceAll(" ", "-").replaceAll("--+", "-")
}
