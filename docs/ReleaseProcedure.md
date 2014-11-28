# Release Procedure

We do a release cycle of approximately eight weeks per minor release.
 
To improve the predictability of releases we follow these rules:

## The Release Cycle

```
---------------------------------
W1
W2 Define set of issues
----------------------------------
W3
W4
W5
W6 Work on issues
----------------------------------
W7
W8 QA, Docs, Packaging
----------------------------------
```

At the end of W2 there are no more issues added to a release. Development continues in master until we reach code freeze at the end of W6. At the end of W6 there should only be stability or documentation related issues left unresolved. Work unrelated to the release must happen in another branch. Finally we do a maven release from master.

## Doing the actual Release

1. Align the version on the [documentation frontpage](https://github.com/wildflyext/wildfly-camel-book/blob/master/README.md)
2. Build the final GitBook and copy the resulting PDF to the [docs folder](../docs)
3. Prepend relevant changes to the [changelog](Changelog.md)
4. Prepare the maven release `mvn release:prepare -Dts.all`
5. Perform the maven release `mvn release:perform -Dts.all`
6. Release the artefacts in the [Nexus Repository](https://repository.jboss.org/nexus)
7. Draft a [new release](https://github.com/tdiesler/wildfly-camel/releases) in github
8. Add release notes to the github release
9. Attach the wildfly-camel-patch-[version].tar.gz to the github release
10. Build and publish the `wildflyext/wildfly-camel:[version]` docker image
11. Publish the `wildflyext/wildfly-camel` docker image as latest
12. Publish the `wildflyext/example-camel-rest` docker image as latest
13. Publish the github release

## Announcing the Release

1. Post a message to thecore@redhat.com
2. Post a message to fuse-engineering@redhat.com
3. Post a message to https://developer.jboss.org/en/products/fuse
4. Post a message to https://developer.jboss.org/en/products/eap
5. Post a message to users@camel.apache.org
6. Post a message to dev@camel.apache.org
7. Go and have a celebration

