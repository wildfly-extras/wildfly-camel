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

* Align the version on the [documentation frontpage](https://github.com/wildflyext/wildfly-camel-book/blob/master/README.md)
* Build the final GitBook and draft a [book release](https://github.com/wildflyext/wildfly-camel-book/releases)
* Attach the final binaries and publish to the book release
* Prepend relevant changes to the [changelog](Changelog.md)
* Prepare the maven release `mvn release:prepare -Dts.all`
* Perform the maven release `mvn release:perform -Dts.all`
* Release the artefacts in the [Nexus Repository](https://repository.jboss.org/nexus)
* Draft a [new release](https://github.com/wildflyext/wildfly-camel/releases) in github
* Add release notes to the github release
* Attach the wildfly-camel-patch and the docbook pdf to the github release
* Build and publish the `wildflyext/wildfly-camel:[version]` docker image
* Publish the `wildflyext/wildfly-camel` docker image as latest
* Publish the `wildflyext/example-camel-rest` docker image as latest
* Publish the github release

## Announcing the Release

* Post a message to thecore@redhat.com
* Post a message to fuse-engineering@redhat.com
* Post a message to https://developer.jboss.org/en/products/fuse
* Post a message to https://developer.jboss.org/en/products/eap
* Post a message to users@camel.apache.org
* Post a message to dev@camel.apache.org
* Go and have a celebration