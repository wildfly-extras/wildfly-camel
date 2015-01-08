# Groovy Module Checker Script

## What is this for?

This relates to [WildFly Camel issue #184](https://github.com/wildfly-extras/wildfly-camel/issues/184). Some means
of detecting when dependencies have been duplicated across WildFly-Camel modules and WildFly app server modules
is required to ensure that a known, consistent and stable set of dependencies is shipped with the Camel subsystem.

## How does it work?

The script does the following....

1. Gets a list of WildFly-Camel project dependencies. These are defined within the 'modules' Maven module pom.xml

2. Retrieves a list of WildFly app server dependencies by resolving dependencies on the WildFly build pom.xml. The
Maven GAV coordinates for this are determined by three properties:

        <appserver.groupId>org.wildfly</appserver.groupId>
        <appserver.artifactId>wildfly-build</appserver.artifactId>
        <appserver.version>${version.wildfly}</appserver.version>

3. Checks to see if any duplicate combination of module name and slot id exists between WildFly-Camel and WildFly app server modules

4. Checks for duplicate Maven group / artifact id combinations between dependencies included in WildFly-Camel and WildFly app server modules

5. Outputs any identified problems and fails the build

## Ignoring dependencies

For some scenarios, it may be necessary to ignore duplicated dependencies and allow the build to proceed. To do this,
add an 'ignoredDependencies' section to the groovy-maven-plugin configuration.

For example, to introduce another version of lucene-core:

    <defaults>
        <ignoredDependencies>
        <![CDATA[
            <dependencies>
                <dependency>
                    <groupId>org.apache.lucene</groupId>
                    <artifactId>lucene-core</artifactId>
                </dependency>
            </dependencies>
        ]]>
        </ignoredDependencies>
    </defaults>
