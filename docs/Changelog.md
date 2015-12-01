### Changelog

#### WildFly-Camel 3.2.0

**Features**

* [#76][76] Provide camel-infinispan integration
* [#846][846] Add support for spring data access with jdbc
* [#879][879] Allow automatic discovery/failover of the AMQ Broker
* [#931][931] Allow customisation for a Groovy Shell 
* [#940][940] Add support for user defined security domain/roles
* [#967][967] Provide camel-metrics integration
* [#985][985] Compatibility for DS based components
* [#990][990] Compatibility for LogService & ConfigurationAdmin
* [#1000][1000] Compatibility for Camel Java DSL deployments

For details see [3.2.0 features](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.2.0"+label%3Afeature)

**Tasks**

* [#847][847] Investigate usage of container managed thread pools / factories with Camel
* [#892][892] Update to camel-2.16.1
* [#910][910] Add test coverage for idempotent consumers
* [#919][919] Verify validity of wildfly-camel patch from the project test suite 
* [#921][921] Remove dependency on shrinkwrap
* [#928][928] Remove dependency on fuse-patch feature pack
* [#929][929] Unnecessary transitive dependencies when using WildFly Camel API
* [#932][932] Add maven-enforcer-plugin rules for Docker properties
* [#933][933] Upgrade Arquillian to 1.1.10.Final
* [#936][936] Add maven repository config for central
* [#943][943] Upgrade to WildFly-9.0.2
* [#945][945] Move tests that depend on standalone-full to basic
* [#952][952] Assert that every expression in exported-path-patterns is used
* [#953][953] Mark modules as private that do not contain exported packages
* [#960][960] Update to wildfly-arquillian-1.0.1.Final
* [#962][962] Use wildfly-core-parent for dependency version defs 
* [#965][965] Move enricher to testenricher
* [#973][973] Modify quickstart example README markdown to be more user friendly 
* [#980][980] Add compatibility tests with Karaf
* [#984][984] Evaluate compatibility for spring based camel routes
* [#986][986] Evaluate compatibility for CDI based components
* [#992][992] Enable Camel in Karaf/WildFly compatibility runtimes

For details see [3.2.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.2.0"+label%3Atask)

**Bugs**

* [#897][897] camel-rest example RestProducerRouteBuilder sometimes produces HTTP 404 errors 
* [#905][905] Cannot use maven release plugin 
* [#908][908] Unable to configure JDBC idempotent consumers
* [#914][914] Unable to configure JPA idempotent consumers
* [#915][915] Unable to configure Infinispan idempotent consumers
* [#923][923] Error with swagger - RestModelConverters
* [#938][938] Fuse patch support missing from wildfly-camel distribution
* [#949][949] HL7 on NETTY4 transport layer fails to define class
* [#969][969] ContextCreateHandler executed on non camel enabled deployments
* [#979][979] XStream unmarshalling requires explicit type permissions
* [#989][989] NCDFE starting gravia subsystem
* [#1002][1002] Failed to add resource root org.apache.felix.configadmin-1.8.8.jar

For details see [3.2.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.2.0"+label%3Abug)

[76]: https://github.com/wildfly-extras/wildfly-camel/issues/76
[846]: https://github.com/wildfly-extras/wildfly-camel/issues/846
[879]: https://github.com/wildfly-extras/wildfly-camel/issues/879
[931]: https://github.com/wildfly-extras/wildfly-camel/issues/931
[940]: https://github.com/wildfly-extras/wildfly-camel/issues/940
[967]: https://github.com/wildfly-extras/wildfly-camel/issues/967
[985]: https://github.com/wildfly-extras/wildfly-camel/issues/985
[990]: https://github.com/wildfly-extras/wildfly-camel/issues/990
[1000]: https://github.com/wildfly-extras/wildfly-camel/issues/1000
[847]: https://github.com/wildfly-extras/wildfly-camel/issues/847
[892]: https://github.com/wildfly-extras/wildfly-camel/issues/892
[910]: https://github.com/wildfly-extras/wildfly-camel/issues/910
[919]: https://github.com/wildfly-extras/wildfly-camel/issues/919
[921]: https://github.com/wildfly-extras/wildfly-camel/issues/921
[928]: https://github.com/wildfly-extras/wildfly-camel/issues/928
[929]: https://github.com/wildfly-extras/wildfly-camel/issues/929
[932]: https://github.com/wildfly-extras/wildfly-camel/issues/932
[933]: https://github.com/wildfly-extras/wildfly-camel/issues/933
[936]: https://github.com/wildfly-extras/wildfly-camel/issues/936
[943]: https://github.com/wildfly-extras/wildfly-camel/issues/943
[945]: https://github.com/wildfly-extras/wildfly-camel/issues/945
[952]: https://github.com/wildfly-extras/wildfly-camel/issues/952
[953]: https://github.com/wildfly-extras/wildfly-camel/issues/953
[960]: https://github.com/wildfly-extras/wildfly-camel/issues/960
[962]: https://github.com/wildfly-extras/wildfly-camel/issues/962
[965]: https://github.com/wildfly-extras/wildfly-camel/issues/965
[973]: https://github.com/wildfly-extras/wildfly-camel/issues/973
[980]: https://github.com/wildfly-extras/wildfly-camel/issues/980
[984]: https://github.com/wildfly-extras/wildfly-camel/issues/984
[986]: https://github.com/wildfly-extras/wildfly-camel/issues/986
[992]: https://github.com/wildfly-extras/wildfly-camel/issues/992
[897]: https://github.com/wildfly-extras/wildfly-camel/issues/897
[905]: https://github.com/wildfly-extras/wildfly-camel/issues/905
[908]: https://github.com/wildfly-extras/wildfly-camel/issues/908
[914]: https://github.com/wildfly-extras/wildfly-camel/issues/914
[915]: https://github.com/wildfly-extras/wildfly-camel/issues/915
[923]: https://github.com/wildfly-extras/wildfly-camel/issues/923
[938]: https://github.com/wildfly-extras/wildfly-camel/issues/938
[949]: https://github.com/wildfly-extras/wildfly-camel/issues/949
[969]: https://github.com/wildfly-extras/wildfly-camel/issues/969
[979]: https://github.com/wildfly-extras/wildfly-camel/issues/979
[989]: https://github.com/wildfly-extras/wildfly-camel/issues/989
[1002]: https://github.com/wildfly-extras/wildfly-camel/issues/1002

#### WildFly-Camel 3.1.0

**Features**

* [#766][766] Create simple archetype for camel project 
* [#778][778] Add support for camel-undertow 
* [#814][814] Add support for camel-elasticsearch
* [#859][859] Add support for camel-jasypt
* [#874][874] Add support for camel-stream
* [#883][883] Only add Camel dependencies for a deployment when used

For details see [3.1.0 features](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.1.0"+label%3Afeature)

**Tasks**

* [#555][555] Verify that Hawtio camel tab works as expected 
* [#592][592] Update to camel-2.16.0
* [#660][660] Update to wildfly-9.0.1.Final
* [#726][726] Remove explicit spring-security version
* [#746][746] Provide test coverage for zookeeper consumer
* [#747][747] Add test coverage for Kafka consumer/producer
* [#754][754] Investigate integration of container managed transactions within Camel routes
* [#779][779] Endpoint creation on existing undertow server
* [#792][792] Review use of Maven dependency resolution in module-checker script 
* [#797][797] Split up dozer into individual module from camel-dozer
* [#799][799] Add dependence on SAP module to extras by default
* [#801][801] Restore or remove skipped tests in CamelSubsystemTestCase
* [#802][802] Restore or remove skipped tests in FileURLDecodingTest
* [#803][803] Restore or remove skipped tests in JMSIntegrationTest
* [#806][806] Remove activemq-rar dependency from modules pom.xml
* [#808][808] Prevent org.wildfly.camel.wildfly-camel-modules module from being added to the patch
* [#811][811] Restore ability for module-checker script to ignore dependencies
* [#817][817] wildfly-camel archetypes group id should be consistent with related projects
* [#818][818] WildFly-Camel archetype example application should be interactive
* [#822][822] Configure descriptions for WildFly-Camel archetypes
* [#831][831] Migrate config core functionality to fuse-patch
* [#834][834] Remove wildfly-camel-enricher dependency from generated wildfly-camel archetype pom.xml
* [#836][836] Expose quartz public API in separate module
* [#838][838] Remove GitHub style markdown from archetype generated README files 
* [#849][849] Cleanup/Simplify profile handling
* [#857][857] Add test coverage for camel-crypto component
* [#860][860] Add test coverage for markRollbackOnly DSL command
* [#861][861] Add test coverage for secure routes utilising SslContextParameters
* [#862][862] Add test coverage for a transactional SQL / JPA camel routes
* [#865][865] Add test coverage for JpaTransactionManager
* [#868][868] Remove camel-sap component
* [#889][889] Increase docker-maven-plugin timeout property for domain mode tests
* [#893][893] Separate jetty from component salesforce
* [#894][894] Revisit plain camel test dependencies

For details see [3.1.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.1.0"+label%3Atask)

**Bugs**

* [#591][591] JMS Session not accessible from route
* [#780][780] Many modules have duplicated dependency declarations
* [#794][794] camel-rest example integration tests throw FileSystemNotFoundException
* [#796][796] Docker maven module versions not updated
* [#815][815] Missing javax.activation.api dependency for org.apache.abdera.core
* [#841][841] Multiple contexts not undeployed properly
* [#844][844] Cannot resolve beanmapping XML Schema
* [#848][848] Skip wiring of Camel modules for resource adapter deployments
* [#853][853] Add feature-pack dependency to archetypes module to ensure correct build order
* [#870][870] Path org.springframework.orm.jpa not exported from org.springframework.orm module
* [#872][872] JpaTransactionManager test throws NCDFE for ResourceTransactionManager

For details see [3.1.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.1.0"+label%3Abug)

[766]: https://github.com/wildfly-extras/wildfly-camel/issues/766
[778]: https://github.com/wildfly-extras/wildfly-camel/issues/778
[814]: https://github.com/wildfly-extras/wildfly-camel/issues/814
[859]: https://github.com/wildfly-extras/wildfly-camel/issues/859
[874]: https://github.com/wildfly-extras/wildfly-camel/issues/874
[883]: https://github.com/wildfly-extras/wildfly-camel/issues/883
[555]: https://github.com/wildfly-extras/wildfly-camel/issues/555
[592]: https://github.com/wildfly-extras/wildfly-camel/issues/592
[660]: https://github.com/wildfly-extras/wildfly-camel/issues/660
[726]: https://github.com/wildfly-extras/wildfly-camel/issues/726
[746]: https://github.com/wildfly-extras/wildfly-camel/issues/746
[747]: https://github.com/wildfly-extras/wildfly-camel/issues/747
[754]: https://github.com/wildfly-extras/wildfly-camel/issues/754
[779]: https://github.com/wildfly-extras/wildfly-camel/issues/779
[792]: https://github.com/wildfly-extras/wildfly-camel/issues/792
[797]: https://github.com/wildfly-extras/wildfly-camel/issues/797
[799]: https://github.com/wildfly-extras/wildfly-camel/issues/799
[801]: https://github.com/wildfly-extras/wildfly-camel/issues/801
[802]: https://github.com/wildfly-extras/wildfly-camel/issues/802
[803]: https://github.com/wildfly-extras/wildfly-camel/issues/803
[806]: https://github.com/wildfly-extras/wildfly-camel/issues/806
[808]: https://github.com/wildfly-extras/wildfly-camel/issues/808
[811]: https://github.com/wildfly-extras/wildfly-camel/issues/811
[817]: https://github.com/wildfly-extras/wildfly-camel/issues/817
[818]: https://github.com/wildfly-extras/wildfly-camel/issues/818
[822]: https://github.com/wildfly-extras/wildfly-camel/issues/822
[831]: https://github.com/wildfly-extras/wildfly-camel/issues/831
[834]: https://github.com/wildfly-extras/wildfly-camel/issues/834
[836]: https://github.com/wildfly-extras/wildfly-camel/issues/836
[838]: https://github.com/wildfly-extras/wildfly-camel/issues/838
[849]: https://github.com/wildfly-extras/wildfly-camel/issues/849
[857]: https://github.com/wildfly-extras/wildfly-camel/issues/857
[860]: https://github.com/wildfly-extras/wildfly-camel/issues/860
[861]: https://github.com/wildfly-extras/wildfly-camel/issues/861
[862]: https://github.com/wildfly-extras/wildfly-camel/issues/862
[865]: https://github.com/wildfly-extras/wildfly-camel/issues/865
[868]: https://github.com/wildfly-extras/wildfly-camel/issues/868
[889]: https://github.com/wildfly-extras/wildfly-camel/issues/889
[893]: https://github.com/wildfly-extras/wildfly-camel/issues/893
[894]: https://github.com/wildfly-extras/wildfly-camel/issues/894
[591]: https://github.com/wildfly-extras/wildfly-camel/issues/591
[780]: https://github.com/wildfly-extras/wildfly-camel/issues/780
[794]: https://github.com/wildfly-extras/wildfly-camel/issues/794
[796]: https://github.com/wildfly-extras/wildfly-camel/issues/796
[815]: https://github.com/wildfly-extras/wildfly-camel/issues/815
[841]: https://github.com/wildfly-extras/wildfly-camel/issues/841
[844]: https://github.com/wildfly-extras/wildfly-camel/issues/844
[848]: https://github.com/wildfly-extras/wildfly-camel/issues/848
[853]: https://github.com/wildfly-extras/wildfly-camel/issues/853
[870]: https://github.com/wildfly-extras/wildfly-camel/issues/870
[872]: https://github.com/wildfly-extras/wildfly-camel/issues/872

#### WildFly-Camel 3.0.0

**Features**

* [#11][11] Provide camel subsystem as feature pack
* [#155][155] Provide camel-sap support
* [#160][160] Provide camel-jgroups support
* [#340][340] Make use of TCCL in ARQ ManagedContainer configurable 
* [#541][541] Add support for wildfly-9.0.x docker image
* [#563][563] Add executable to enable/disable the camel subsytem
* [#615][615] Provide camel-zookeeper support
* [#654][654] Provide camel-salesforce support
* [#680][680] Add support for extending set of wired modules
* [#723][723] Add support for Exchange.AUTHENTICATION header
* [#732][732] Add support for camel-kafka

For details see [3.0.0 features](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.0.0"+label%3Afeature)

**Tasks**

* [#10][10] Update to wildfly-9.0.0.CR1
* [#98][98] Use docker-maven-plugin to generate images
* [#565][565] Ensure smartics-jboss-modules-maven-plugin changes are merged upstream
* [#629][629] Move module org.bouncycastle.pgp to extras
* [#630][630] Move module org.apache.cxf.ext to extras
* [#681][681] Migrate config tool from decentxml to jdom
* [#682][682] Remove shading of the config tool
* [#683][683] Provide pluggable SPI for config tool
* [#689][689] Exclude generated subsystem module definition from SCM
* [#700][700] Add support for module layers to config tool 
* [#708][708] Move mqtt-client dependency into its own module
* [#711][711] Review service loader contract for ConfigPlugins
* [#713][713] Split integration tests in smoke, basic, extras
* [#721][721] Reduce redundant plugin configurations in examples
* [#733][733] Provide unit tests for PackageScanClassResolver
* [#739][739] ExportedPathsTest should ignore paths exported from org.wildfly.camel.extras
* [#750][750] Verify that wildfly-camel runs on windows
* [#751][751] Ensure smartics artifact element values are prefixed with ':' when group id is ignored
* [#753][753] Fix docker build on Jenkins
* [#755][755] Use javax.el.api provided by wildfly
* [#757][757] Verify that tests/examples delegate to the WildFly TransactionManager
* [#758][758] Provide SAP connector module by default
* [#760][760] Generate camel subsystem modules
* [#762][762] Remove activemq-rar from module definition
* [#776][776] Upgrade to smartics-2.1.0
* [#777][777] Update OpenShift to 1.0.3
* [#781][781] Migrate away from deprecated subsystem API
* [#787][787] Add README files to camel-cdi and camel-jpa quickstarts
* [#788][788] Enhance deployment of Camel quickstart
* [#789][789] Make io.github.rometools.rome module available

For details see [3.0.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.0.0"+label%3Atask)

**Bugs**

* [#345][345] xnio-file-watcher exception
* [#534][534] CXF cannot access springframework beans module
* [#540][540] jboss-modules does not respect path excludes
* [#544][544] Examples fail with ClassNotFoundException
* [#677][677] Publish docker images script cannot be exited with ctrl+c
* [#684][684] Replace joda-time module dependency in org.json4s with the WildFly version
* [#686][686] Docs maven module version not updated
* [#692][692] The build should fail if execution of ConfigMain utility is not successful
* [#701][701] Unable to load camel type converters from package locations specified in TypeConverter manifest
* [#703][703] Permission denied on bin/fuseconfig.sh  access
* [#710][710] XSLT transform route cannot load net.sf.saxon
* [#717][717] NoSuchFileException in ActiveMQExampleTest.testFileToActiveMQRoute
* [#738][738] Kafka module cannot access JMX API
* [#773][773] Logging.properties is not loaded for all module test suites on Windows
* [#785][785] Config tool generates files with incorrect line endings

For details see [3.0.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"3.0.0"+label%3Abug)

[11]: https://github.com/wildfly-extras/wildfly-camel/issues/11
[155]: https://github.com/wildfly-extras/wildfly-camel/issues/155
[160]: https://github.com/wildfly-extras/wildfly-camel/issues/160
[340]: https://github.com/wildfly-extras/wildfly-camel/issues/340
[541]: https://github.com/wildfly-extras/wildfly-camel/issues/541
[563]: https://github.com/wildfly-extras/wildfly-camel/issues/563
[615]: https://github.com/wildfly-extras/wildfly-camel/issues/615
[654]: https://github.com/wildfly-extras/wildfly-camel/issues/654
[680]: https://github.com/wildfly-extras/wildfly-camel/issues/680
[723]: https://github.com/wildfly-extras/wildfly-camel/issues/723
[732]: https://github.com/wildfly-extras/wildfly-camel/issues/732
[10]: https://github.com/wildfly-extras/wildfly-camel/issues/10
[98]: https://github.com/wildfly-extras/wildfly-camel/issues/98
[565]: https://github.com/wildfly-extras/wildfly-camel/issues/565
[629]: https://github.com/wildfly-extras/wildfly-camel/issues/629
[630]: https://github.com/wildfly-extras/wildfly-camel/issues/630
[681]: https://github.com/wildfly-extras/wildfly-camel/issues/681
[682]: https://github.com/wildfly-extras/wildfly-camel/issues/682
[683]: https://github.com/wildfly-extras/wildfly-camel/issues/683
[689]: https://github.com/wildfly-extras/wildfly-camel/issues/689
[700]: https://github.com/wildfly-extras/wildfly-camel/issues/700
[708]: https://github.com/wildfly-extras/wildfly-camel/issues/708
[711]: https://github.com/wildfly-extras/wildfly-camel/issues/711
[713]: https://github.com/wildfly-extras/wildfly-camel/issues/713
[721]: https://github.com/wildfly-extras/wildfly-camel/issues/721
[733]: https://github.com/wildfly-extras/wildfly-camel/issues/733
[739]: https://github.com/wildfly-extras/wildfly-camel/issues/739
[750]: https://github.com/wildfly-extras/wildfly-camel/issues/750
[751]: https://github.com/wildfly-extras/wildfly-camel/issues/751
[753]: https://github.com/wildfly-extras/wildfly-camel/issues/753
[755]: https://github.com/wildfly-extras/wildfly-camel/issues/755
[757]: https://github.com/wildfly-extras/wildfly-camel/issues/757
[758]: https://github.com/wildfly-extras/wildfly-camel/issues/758
[760]: https://github.com/wildfly-extras/wildfly-camel/issues/760
[762]: https://github.com/wildfly-extras/wildfly-camel/issues/762
[776]: https://github.com/wildfly-extras/wildfly-camel/issues/776
[777]: https://github.com/wildfly-extras/wildfly-camel/issues/777
[781]: https://github.com/wildfly-extras/wildfly-camel/issues/781
[787]: https://github.com/wildfly-extras/wildfly-camel/issues/787
[788]: https://github.com/wildfly-extras/wildfly-camel/issues/788
[789]: https://github.com/wildfly-extras/wildfly-camel/issues/789
[345]: https://github.com/wildfly-extras/wildfly-camel/issues/345
[534]: https://github.com/wildfly-extras/wildfly-camel/issues/534
[540]: https://github.com/wildfly-extras/wildfly-camel/issues/540
[544]: https://github.com/wildfly-extras/wildfly-camel/issues/544
[677]: https://github.com/wildfly-extras/wildfly-camel/issues/677
[684]: https://github.com/wildfly-extras/wildfly-camel/issues/684
[686]: https://github.com/wildfly-extras/wildfly-camel/issues/686
[692]: https://github.com/wildfly-extras/wildfly-camel/issues/692
[701]: https://github.com/wildfly-extras/wildfly-camel/issues/701
[703]: https://github.com/wildfly-extras/wildfly-camel/issues/703
[710]: https://github.com/wildfly-extras/wildfly-camel/issues/710
[717]: https://github.com/wildfly-extras/wildfly-camel/issues/717
[738]: https://github.com/wildfly-extras/wildfly-camel/issues/738
[773]: https://github.com/wildfly-extras/wildfly-camel/issues/773
[785]: https://github.com/wildfly-extras/wildfly-camel/issues/785

#### WildFly-Camel 2.3.0

**Features**

* [#395][395] Provide camel-swagger support
* [#397][397] Add support for parameter binding using annotioans
* [#398][398] Add support for language-based bean binding annotations
* [#575][575] Release camel examples as part of the patch
* [#582][582] Add support for authentication and authorization policies
* [#616][616] Add support for dynamically adding/removing system camel contexts
* [#657][657] Allow SOAP Header manipulation in cxf component
* [#671][671] Disable camel subsystem for switchyard deployments

For details see [2.3.0 features](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.3.0"+label%3Afeature)

**Tasks**

* [#252][252] Automate the release of wildfly-camel docker images 
* [#405][405] Add compatibility for Maven 3.2.5 and above
* [#428][428] Replace external RSS feed access with betamax
* [#432][432] Remove explicit dependency on restlet 
* [#474][474] Remove explicit dependency on ognl
* [#477][477] Remove explicit dependency on castor
* [#497][497] Move CustomConverter to sub package
* [#519][519] Update to camel-2.15.1
* [#520][520] Update to camel-2.15.2
* [#521][521] Remove explicit dependency on script engines
* [#524][524] Investigate usage of maven-changes-plugin
* [#530][530] Remove dependency on Felix SCR and Gravia Provisioner
* [#532][532] Remove dependency on Gravia Repository and ConfigurationAdmin
* [#542][542] Add client side arquillian log for examples
* [#551][551] Allow standalone tests to get executed against running server
* [#556][556] Verify that Hawtio connect works as expected
* [#567][567] Update OpenShift to v0.5
* [#576][576] Add integration test to verify that hawtio/jolokia is deployed and secured.
* [#585][585] Add eclipse import ordering spec
* [#598][598] Rename CamelContextRegistry.getContext(String name)
* [#602][602] Move subsystem module definition from patch to modules
* [#605][605] Document testsuite application/management credentials
* [#618][618] Add capability for integration tests to be used in other projects 
* [#619][619] Rationalise configuration files
* [#626][626] Configure users/roles required by tests programmatically
* [#627][627] Include XML Beans schemaorg content in integration tests jar
* [#632][632] Remove examples distro module
* [#637][637] Move mock-javamail to dependency management section in root pom
* [#646][646] Put examples in ${jboss.home}/quickstarts/camel
* [#649][649] Skip wiring of camel modules for switchyard deployments
* [#652][652] Disable CamelContext hooks for switchyard deployments
* [#662][662] Replace hardcoded domain-camel.xml with generated config

For details see [2.3.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.3.0"+label%3Atask)

**Bugs**

* [#387][387] Usage of camel-xstream depends on TCCL
* [#490][490] Usage of camel-xmlbeans depends on TCCL
* [#507][507] Generated module definitions depend on maven/jdk version
* [#522][522] Global dependency exclude for org.osgi.core needed
* [#525][525] org.apache.camel.script.jruby module does not provide specified bsf dependency
* [#538][538] Generated CXF module definition has duplicate spring dependencies
* [#557][557] Adding context XML in EAR cause duplicate context creation
* [#566][566] REST example no longer works as expected for Openshift demo
* [#571][571] Hawtio login does not work
* [#580][580] JMSIntegrationTest depends on message ordering
* [#587][587] Lucene uses slot 4.6 for version 4.10.x
* [#589][589] Module checker script does not catch duplicate module resources in all cases
* [#594][594] Examples depend on distro which is not available in remote mvn repo
* [#600][600] Documentation references incorrect paths for examples
* [#607][607] Config generator drops CDATA section
* [#608][608] Operations on ManagedCamelContext may fail with JAXB error
* [#622][622] Path to exported-paths.txt in ExportedPathsTest should be configurable
* [#625][625] JMS Example cannot find OrdersQueue
* [#639][639] JAXBIntegrationTest may fail with shrinkwrap resolver issue
* [#642][642] Betamax tapeRoot property should be a relative path 
* [#643][643] Cannot add schemaorg_apache_xmlbeans when sourced from jar
* [#664][664] Generated domain.xml does not work in testsuite
* [#666][666] Password must have at least 1 non-alphanumeric symbol
* [#668][668] Build fails on OpenJDK

For details see [2.3.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.3.0"+label%3Abug)

[395]: https://github.com/wildfly-extras/wildfly-camel/issues/395
[397]: https://github.com/wildfly-extras/wildfly-camel/issues/397
[398]: https://github.com/wildfly-extras/wildfly-camel/issues/398
[575]: https://github.com/wildfly-extras/wildfly-camel/issues/575
[582]: https://github.com/wildfly-extras/wildfly-camel/issues/582
[616]: https://github.com/wildfly-extras/wildfly-camel/issues/616
[657]: https://github.com/wildfly-extras/wildfly-camel/issues/657
[671]: https://github.com/wildfly-extras/wildfly-camel/issues/671
[252]: https://github.com/wildfly-extras/wildfly-camel/issues/252
[405]: https://github.com/wildfly-extras/wildfly-camel/issues/405
[428]: https://github.com/wildfly-extras/wildfly-camel/issues/428
[432]: https://github.com/wildfly-extras/wildfly-camel/issues/432
[474]: https://github.com/wildfly-extras/wildfly-camel/issues/474
[477]: https://github.com/wildfly-extras/wildfly-camel/issues/477
[497]: https://github.com/wildfly-extras/wildfly-camel/issues/497
[519]: https://github.com/wildfly-extras/wildfly-camel/issues/519
[520]: https://github.com/wildfly-extras/wildfly-camel/issues/520
[521]: https://github.com/wildfly-extras/wildfly-camel/issues/521
[524]: https://github.com/wildfly-extras/wildfly-camel/issues/524
[530]: https://github.com/wildfly-extras/wildfly-camel/issues/530
[532]: https://github.com/wildfly-extras/wildfly-camel/issues/532
[542]: https://github.com/wildfly-extras/wildfly-camel/issues/542
[551]: https://github.com/wildfly-extras/wildfly-camel/issues/551
[556]: https://github.com/wildfly-extras/wildfly-camel/issues/556
[567]: https://github.com/wildfly-extras/wildfly-camel/issues/567
[576]: https://github.com/wildfly-extras/wildfly-camel/issues/576
[585]: https://github.com/wildfly-extras/wildfly-camel/issues/585
[598]: https://github.com/wildfly-extras/wildfly-camel/issues/598
[602]: https://github.com/wildfly-extras/wildfly-camel/issues/602
[605]: https://github.com/wildfly-extras/wildfly-camel/issues/605
[618]: https://github.com/wildfly-extras/wildfly-camel/issues/618
[619]: https://github.com/wildfly-extras/wildfly-camel/issues/619
[626]: https://github.com/wildfly-extras/wildfly-camel/issues/626
[627]: https://github.com/wildfly-extras/wildfly-camel/issues/627
[632]: https://github.com/wildfly-extras/wildfly-camel/issues/632
[637]: https://github.com/wildfly-extras/wildfly-camel/issues/637
[646]: https://github.com/wildfly-extras/wildfly-camel/issues/646
[649]: https://github.com/wildfly-extras/wildfly-camel/issues/649
[652]: https://github.com/wildfly-extras/wildfly-camel/issues/652
[662]: https://github.com/wildfly-extras/wildfly-camel/issues/662
[387]: https://github.com/wildfly-extras/wildfly-camel/issues/387
[490]: https://github.com/wildfly-extras/wildfly-camel/issues/490
[507]: https://github.com/wildfly-extras/wildfly-camel/issues/507
[522]: https://github.com/wildfly-extras/wildfly-camel/issues/522
[525]: https://github.com/wildfly-extras/wildfly-camel/issues/525
[538]: https://github.com/wildfly-extras/wildfly-camel/issues/538
[557]: https://github.com/wildfly-extras/wildfly-camel/issues/557
[566]: https://github.com/wildfly-extras/wildfly-camel/issues/566
[571]: https://github.com/wildfly-extras/wildfly-camel/issues/571
[580]: https://github.com/wildfly-extras/wildfly-camel/issues/580
[587]: https://github.com/wildfly-extras/wildfly-camel/issues/587
[589]: https://github.com/wildfly-extras/wildfly-camel/issues/589
[594]: https://github.com/wildfly-extras/wildfly-camel/issues/594
[600]: https://github.com/wildfly-extras/wildfly-camel/issues/600
[607]: https://github.com/wildfly-extras/wildfly-camel/issues/607
[608]: https://github.com/wildfly-extras/wildfly-camel/issues/608
[622]: https://github.com/wildfly-extras/wildfly-camel/issues/622
[625]: https://github.com/wildfly-extras/wildfly-camel/issues/625
[639]: https://github.com/wildfly-extras/wildfly-camel/issues/639
[642]: https://github.com/wildfly-extras/wildfly-camel/issues/642
[643]: https://github.com/wildfly-extras/wildfly-camel/issues/643
[664]: https://github.com/wildfly-extras/wildfly-camel/issues/664
[666]: https://github.com/wildfly-extras/wildfly-camel/issues/666
[668]: https://github.com/wildfly-extras/wildfly-camel/issues/668

#### WildFly-Camel 2.2.0

**Features**

* [#32][32] Provide camel-quartz2 integration
* [#33][33] Provide camel-rss integration
* [#34][34] Provide camel-saxon integration
* [#35][35] Provide camel-script integration
* [#37][37] Provide camel-sql integration
* [#157][157] Provide camel-servlet support
* [#158][158] Provide camel-http4 support
* [#159][159] Provide camel-restlet support
* [#161][161] Provide camel-ejb support
* [#162][162] Provide camel-dozer support
* [#199][199] Make set of wired camel components configurable
* [#206][206] Provide multiple Camel config files per deployment
* [#233][233] Make deployed camel context CDI injectable
* [#237][237] Provide camel-rest integration
* [#245][245] Provide camel-file support
* [#328][328] Disabling the camel subsystem entirely per deployment
* [#366][366] Add support for packageScan
* [#367][367] Add support for contextScan
* [#368][368] Add support for import resource in context XML
* [#373][373] Add support for @RecipientList processing
* [#385][385] Provide camel-xstream support
* [#390][390] Provide support for out of the box data formats
* [#392][392] Add support for Velocity templates
* [#394][394] Add support for custom type converters
* [#415][415] Add support for SpringRouteBuilder
* [#442][442] Add support for camel-castor data format
* [#444][444] Add support for camel-crypto data format
* [#446][446] Add support for camel-csv data format
* [#450][450] Add support for camel-flatpack data format
* [#451][451] Add support for gzip data format
* [#452][452] Add support for camel-jackson data format
* [#453][453] Add support for camel-protobuf data format
* [#454][454] Add support for camel-soap data format
* [#455][455] Add support for camel-serialization data format
* [#456][456] Add support for TidyMarkup data format
* [#457][457] Add support for camel-xmlbeans data format
* [#458][458] Add support for camel-xmlsecurity data format
* [#459][459] Add support for camel-zip data format
* [#498][498] Add support for javaScript
* [#499][499] Add support for python scripts
* [#500][500] Add support for ruby scripts
* [#501][501] Add support for groovy scripts

For details see [2.2.0 features](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.2.0"+label%3Afeature)

**Tasks**

* [#248][248] Review and improve JAX-WS example
* [#249][249] Review and improve JAX-RS example
* [#250][250] Review and improve ActiveMQ/JMS example
* [#251][251] Review and improve mail example
* [#262][262] Add wildfly-camel module XML files to Git
* [#264][264] Explicitly define and enforce set of publicly exposed packages
* [#267][267] Review dependency on Spring JtaTransactionManager
* [#306][306] Update to arquillian-1.1.7.Final
* [#320][320] Update to camel-2.15.0
* [#331][331] Add custom module for CXF 3.0.2
* [#349][349] Revisit example camel-mail wildfly server config and startup
* [#359][359] Use camel-parent BOM defined artefact versions
* [#365][365] Reduce redefinition of camel 3rd party dependencies
* [#371][371] Split CXF integration tests into jax-rs and jax-ws
* [#382][382] Cleanup stale repository definitions
* [#384][384] Remove references to staging repositories
* [#386][386] Document why CamelContextFactory is used
* [#406][406] Enable client side logging for standalone tests
* [#408][408] Explicitly define heap and perm size for examples server
* [#413][413] Move test distribution module to itests
* [#417][417] Revisit ContextCreateHandler processing
* [#422][422] Remove dependency on deprecated camel Container API
* [#425][425] Switch to more reliable RSS feed than jboss.org
* [#429][429] Remove explicit dependency on org.apache.cxf
* [#430][430] Remove explicit dependency on org.apache.activemq
* [#433][433] Explicitly mark versions that need to be aligned with the Karaf runtime
* [#440][440] Verify access to log component
* [#460][460] Consolidate smartics dependency excludes
* [#469][469] Remove dependency on org.apache.servicemix.bundles.ognl
* [#470][470] Remove dependency on org.glassfish.web.javax.el
* [#485][485] Update to hawtio-1.4.48
* [#494][494] Reduce paths exported through camel-xmlbeans 
* [#509][509] Replace literal string comparison with normalized XML
* [#511][511] Move scripting languages to separate modules
* [#516][516] Verify that resource streams obtained from a class loader are closed properly

For details see [2.2.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.2.0"+label%3Atask)

**Bugs**

* [#185][185] docker:stop cannot reliably stop/kill containers
* [#254][254] Unable to load custom exception in load balancer
* [#255][255] Unable to load properties file from classpath
* [#258][258] Incorrect version of spring-core is being packaged with the subsystem
* [#265][265] Server bootstrap hangs in Hawtio remote Git access
* [#271][271] Module camel-script exposes bsh
* [#272][272] Module camel-rss exposes com.sun.syndication
* [#274][274] Module camel-core exposes org.springframework
* [#278][278] Intermittent failures of Docker domain mode tests
* [#282][282] Usage of camel-atom depends on TCCL
* [#283][283] Usage of camel-cxf depends on TCCL
* [#284][284] Usage of camel-ftp depends on TCCL
* [#285][285] Usage of camel-hl7 depends on TCCL
* [#286][286] Usage of camel-http depends on TCCL
* [#287][287] Usage of camel-jaxb depends on TCCL
* [#288][288] Usage of camel-jms depends on TCCL
* [#289][289] Usage of camel-lucene depends on TCCL
* [#290][290] Usage of camel-mail depends on TCCL
* [#291][291] Usage of camel-mina2 depends on TCCL
* [#292][292] Camel endpoint discovery depends on TCCL
* [#299][299] Usage of camel-script depends on TCCL
* [#300][300] Usage of SpringCamelContextFactory depends on TCCL
* [#316][316] Conflict between camel-http / camel-http4 components 
* [#319][319] Unable to locate Spring JEE Namespace Handler 
* [#343][343] Examples can no longer run standalone 
* [#347][347] Generated module definitions contain optional dependencies
* [#354][354] Distro module attaches tar archive artifact multiple times
* [#357][357] ActiveMQIntegrationTest bundles all ActiveMQ jars
* [#361][361] Dependency on lucene defined twice
* [#376][376] Camel atom component requires exported paths for abdera core 
* [#378][378] Intermittent failures of JMS integration tests
* [#380][380] Unable to use the wildfly-maven-plugin run goal to execute WildFly Camel examples  
* [#381][381] Unable to configure ActiveMQ resource adapter
* [#391][391] Cannot obtain DOMImplementationRegistry instance
* [#399][399] Fix missing plugin versions
* [#402][402] CamelEmailIntegrationTest cannot connect
* [#410][410] Cannot load mina type converter
* [#439][439] Unable to configure CXF producer endpoints in Spring XML contexts 
* [#464][464] Object may get marshalled to wrong CSV order
* [#467][467] Docker domain unit tests fail on environments with SELinux enabled
* [#502][502] Project does not build successfully on Windows
* [#503][503] wildfly-camel-modules pom.xml has duplicate camel-soap dependency

For details see [2.2.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.2.0"+label%3Abug)

[32]: https://github.com/wildfly-extras/wildfly-camel/issues/32
[33]: https://github.com/wildfly-extras/wildfly-camel/issues/33
[34]: https://github.com/wildfly-extras/wildfly-camel/issues/34
[35]: https://github.com/wildfly-extras/wildfly-camel/issues/35
[37]: https://github.com/wildfly-extras/wildfly-camel/issues/37
[157]: https://github.com/wildfly-extras/wildfly-camel/issues/157
[158]: https://github.com/wildfly-extras/wildfly-camel/issues/158
[159]: https://github.com/wildfly-extras/wildfly-camel/issues/159
[161]: https://github.com/wildfly-extras/wildfly-camel/issues/161
[162]: https://github.com/wildfly-extras/wildfly-camel/issues/162
[199]: https://github.com/wildfly-extras/wildfly-camel/issues/199
[206]: https://github.com/wildfly-extras/wildfly-camel/issues/206
[233]: https://github.com/wildfly-extras/wildfly-camel/issues/233
[237]: https://github.com/wildfly-extras/wildfly-camel/issues/237
[245]: https://github.com/wildfly-extras/wildfly-camel/issues/245
[328]: https://github.com/wildfly-extras/wildfly-camel/issues/328
[366]: https://github.com/wildfly-extras/wildfly-camel/issues/366
[367]: https://github.com/wildfly-extras/wildfly-camel/issues/367
[368]: https://github.com/wildfly-extras/wildfly-camel/issues/368
[373]: https://github.com/wildfly-extras/wildfly-camel/issues/373
[385]: https://github.com/wildfly-extras/wildfly-camel/issues/385
[390]: https://github.com/wildfly-extras/wildfly-camel/issues/390
[392]: https://github.com/wildfly-extras/wildfly-camel/issues/392
[394]: https://github.com/wildfly-extras/wildfly-camel/issues/394
[415]: https://github.com/wildfly-extras/wildfly-camel/issues/415
[442]: https://github.com/wildfly-extras/wildfly-camel/issues/442
[444]: https://github.com/wildfly-extras/wildfly-camel/issues/444
[446]: https://github.com/wildfly-extras/wildfly-camel/issues/446
[450]: https://github.com/wildfly-extras/wildfly-camel/issues/450
[451]: https://github.com/wildfly-extras/wildfly-camel/issues/451
[452]: https://github.com/wildfly-extras/wildfly-camel/issues/452
[453]: https://github.com/wildfly-extras/wildfly-camel/issues/453
[454]: https://github.com/wildfly-extras/wildfly-camel/issues/454
[455]: https://github.com/wildfly-extras/wildfly-camel/issues/455
[456]: https://github.com/wildfly-extras/wildfly-camel/issues/456
[457]: https://github.com/wildfly-extras/wildfly-camel/issues/457
[458]: https://github.com/wildfly-extras/wildfly-camel/issues/458
[459]: https://github.com/wildfly-extras/wildfly-camel/issues/459
[498]: https://github.com/wildfly-extras/wildfly-camel/issues/498
[499]: https://github.com/wildfly-extras/wildfly-camel/issues/499
[500]: https://github.com/wildfly-extras/wildfly-camel/issues/500
[501]: https://github.com/wildfly-extras/wildfly-camel/issues/501
[248]: https://github.com/wildfly-extras/wildfly-camel/issues/248
[249]: https://github.com/wildfly-extras/wildfly-camel/issues/249
[250]: https://github.com/wildfly-extras/wildfly-camel/issues/250
[251]: https://github.com/wildfly-extras/wildfly-camel/issues/251
[262]: https://github.com/wildfly-extras/wildfly-camel/issues/262
[264]: https://github.com/wildfly-extras/wildfly-camel/issues/264
[267]: https://github.com/wildfly-extras/wildfly-camel/issues/267
[306]: https://github.com/wildfly-extras/wildfly-camel/issues/306
[320]: https://github.com/wildfly-extras/wildfly-camel/issues/320
[331]: https://github.com/wildfly-extras/wildfly-camel/issues/331
[349]: https://github.com/wildfly-extras/wildfly-camel/issues/349
[359]: https://github.com/wildfly-extras/wildfly-camel/issues/359
[365]: https://github.com/wildfly-extras/wildfly-camel/issues/365
[371]: https://github.com/wildfly-extras/wildfly-camel/issues/371
[382]: https://github.com/wildfly-extras/wildfly-camel/issues/382
[384]: https://github.com/wildfly-extras/wildfly-camel/issues/384
[386]: https://github.com/wildfly-extras/wildfly-camel/issues/386
[406]: https://github.com/wildfly-extras/wildfly-camel/issues/406
[408]: https://github.com/wildfly-extras/wildfly-camel/issues/408
[413]: https://github.com/wildfly-extras/wildfly-camel/issues/413
[417]: https://github.com/wildfly-extras/wildfly-camel/issues/417
[422]: https://github.com/wildfly-extras/wildfly-camel/issues/422
[425]: https://github.com/wildfly-extras/wildfly-camel/issues/425
[429]: https://github.com/wildfly-extras/wildfly-camel/issues/429
[430]: https://github.com/wildfly-extras/wildfly-camel/issues/430
[433]: https://github.com/wildfly-extras/wildfly-camel/issues/433
[440]: https://github.com/wildfly-extras/wildfly-camel/issues/440
[460]: https://github.com/wildfly-extras/wildfly-camel/issues/460
[469]: https://github.com/wildfly-extras/wildfly-camel/issues/469
[470]: https://github.com/wildfly-extras/wildfly-camel/issues/470
[485]: https://github.com/wildfly-extras/wildfly-camel/issues/485
[494]: https://github.com/wildfly-extras/wildfly-camel/issues/494
[509]: https://github.com/wildfly-extras/wildfly-camel/issues/509
[511]: https://github.com/wildfly-extras/wildfly-camel/issues/511
[516]: https://github.com/wildfly-extras/wildfly-camel/issues/516
[185]: https://github.com/wildfly-extras/wildfly-camel/issues/185
[254]: https://github.com/wildfly-extras/wildfly-camel/issues/254
[255]: https://github.com/wildfly-extras/wildfly-camel/issues/255
[258]: https://github.com/wildfly-extras/wildfly-camel/issues/258
[265]: https://github.com/wildfly-extras/wildfly-camel/issues/265
[271]: https://github.com/wildfly-extras/wildfly-camel/issues/271
[272]: https://github.com/wildfly-extras/wildfly-camel/issues/272
[274]: https://github.com/wildfly-extras/wildfly-camel/issues/274
[278]: https://github.com/wildfly-extras/wildfly-camel/issues/278
[282]: https://github.com/wildfly-extras/wildfly-camel/issues/282
[283]: https://github.com/wildfly-extras/wildfly-camel/issues/283
[284]: https://github.com/wildfly-extras/wildfly-camel/issues/284
[285]: https://github.com/wildfly-extras/wildfly-camel/issues/285
[286]: https://github.com/wildfly-extras/wildfly-camel/issues/286
[287]: https://github.com/wildfly-extras/wildfly-camel/issues/287
[288]: https://github.com/wildfly-extras/wildfly-camel/issues/288
[289]: https://github.com/wildfly-extras/wildfly-camel/issues/289
[290]: https://github.com/wildfly-extras/wildfly-camel/issues/290
[291]: https://github.com/wildfly-extras/wildfly-camel/issues/291
[292]: https://github.com/wildfly-extras/wildfly-camel/issues/292
[299]: https://github.com/wildfly-extras/wildfly-camel/issues/299
[300]: https://github.com/wildfly-extras/wildfly-camel/issues/300
[316]: https://github.com/wildfly-extras/wildfly-camel/issues/316
[319]: https://github.com/wildfly-extras/wildfly-camel/issues/319
[343]: https://github.com/wildfly-extras/wildfly-camel/issues/343
[347]: https://github.com/wildfly-extras/wildfly-camel/issues/347
[354]: https://github.com/wildfly-extras/wildfly-camel/issues/354
[357]: https://github.com/wildfly-extras/wildfly-camel/issues/357
[361]: https://github.com/wildfly-extras/wildfly-camel/issues/361
[376]: https://github.com/wildfly-extras/wildfly-camel/issues/376
[378]: https://github.com/wildfly-extras/wildfly-camel/issues/378
[380]: https://github.com/wildfly-extras/wildfly-camel/issues/380
[381]: https://github.com/wildfly-extras/wildfly-camel/issues/381
[391]: https://github.com/wildfly-extras/wildfly-camel/issues/391
[399]: https://github.com/wildfly-extras/wildfly-camel/issues/399
[402]: https://github.com/wildfly-extras/wildfly-camel/issues/402
[410]: https://github.com/wildfly-extras/wildfly-camel/issues/410
[439]: https://github.com/wildfly-extras/wildfly-camel/issues/439
[464]: https://github.com/wildfly-extras/wildfly-camel/issues/464
[467]: https://github.com/wildfly-extras/wildfly-camel/issues/467
[502]: https://github.com/wildfly-extras/wildfly-camel/issues/502
[503]: https://github.com/wildfly-extras/wildfly-camel/issues/503

#### WildFly-Camel 2.1.0

**Features**

* [#2][2] support expressions in subsystem configuration
* [#21][21] Provide camel-atom integration
* [#22][22] Provide camel-bindy integration
* [#23][23] Provide camel-ftp integration
* [#24][24] Provide camel-hl7 integration
* [#26][26] Provide camel-mail integration
* [#27][27] Provide camel-mina2 integration
* [#28][28] Provide camel-mqtt integration
* [#29][29] Provide camel-mvel integration
* [#30][30] Provide camel-netty integration
* [#31][31] Provide camel-ognl integration
* [#75][75] Provide OpenShift v3 example
* [#140][140] Provide camel-weather integration
* [#142][142] Provide camel-lucene integration
* [#148][148] Provide WildFly domain mode in Docker

For details see [2.1.0 features](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.1.0"+label%3Afeature)

**Tasks**

* [#1][1] Define content and format of the Camel subsystem configuration
* [#93][93] Support WildFly domain mode in OpenShift
* [#95][95] Upgrade to WildFly 8.2.0.Final
* [#106][106] Application/Management user setup for docker container
* [#107][107] Remove the default camel webapp 
* [#124][124] Provide Fabric8 V2 example
* [#134][134] Upgrade to hawtio-1.4.42
* [#137][137] Use the official tagged jboss/wildfly image as base
* [#145][145] Remove ActiveMQ resource adapter declaration from domain-camel.xml
* [#152][152] Docker username / password environment variables should be unset after use
* [#153][153] Use command line args instead of environment vars to configure Docker WildFly startup
* [#163][163] Run docker wildfly domain with default base image
* [#172][172] Simplify image parameter list using ENTRYPOINT
* [#178][178] Define version for war and license plugin
* [#182][182] Use management realm for hawtio authentication
* [#186][186] Review todos in spring modules
* [#187][187] Review all [TODO] markers and add issue references 
* [#201][201] Update to camel-2.14.1
* [#218][218] Domain mode Docker integration tests are not portable across different platforms 
* [#227][227] Put wildfly modules in a layer 
* [#231][231] Rename activemq component module
* [#235][235] Remove duplicate REST annotations in GreetingService
* [#242][242] Make CamelContextFactory available as Gravia service

For details see [2.1.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.1.0"+label%3Atask)

**Bugs**

* [#79][79] ActiveMQ module should not be creating a broker
* [#130][130] Error while attempting to retrieve a connection from the pool (on shutdown)
* [#165][165] WeatherTest may fail with 511
* [#167][167] example-camel-rest generates orphan image
* [#184][184] smartics generator includes redundant module definitions
* [#192][192] Cannot build on openjdk
* [#194][194] camel-lucene component is not compatible with the WildFly Lucene module
* [#217][217] Test security domain should not be present in standalone-camel.xml
* [#225][225] Components not being loaded on XML Context definitions
* [#229][229] Unwanted module XML definition is being generated for org.wildfly.camel:wildfly-camel-modules
* [#246][246] Cannot load components defined in system contexts

For details see [2.1.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.1.0"+label%3Abug)

[2]: https://github.com/wildfly-extras/wildfly-camel/issues/2
[21]: https://github.com/wildfly-extras/wildfly-camel/issues/21
[22]: https://github.com/wildfly-extras/wildfly-camel/issues/22
[23]: https://github.com/wildfly-extras/wildfly-camel/issues/23
[24]: https://github.com/wildfly-extras/wildfly-camel/issues/24
[26]: https://github.com/wildfly-extras/wildfly-camel/issues/26
[27]: https://github.com/wildfly-extras/wildfly-camel/issues/27
[28]: https://github.com/wildfly-extras/wildfly-camel/issues/28
[29]: https://github.com/wildfly-extras/wildfly-camel/issues/29
[30]: https://github.com/wildfly-extras/wildfly-camel/issues/30
[31]: https://github.com/wildfly-extras/wildfly-camel/issues/31
[75]: https://github.com/wildfly-extras/wildfly-camel/issues/75
[140]: https://github.com/wildfly-extras/wildfly-camel/issues/140
[142]: https://github.com/wildfly-extras/wildfly-camel/issues/142
[148]: https://github.com/wildfly-extras/wildfly-camel/issues/148
[1]: https://github.com/wildfly-extras/wildfly-camel/issues/1
[93]: https://github.com/wildfly-extras/wildfly-camel/issues/93
[95]: https://github.com/wildfly-extras/wildfly-camel/issues/95
[106]: https://github.com/wildfly-extras/wildfly-camel/issues/106
[107]: https://github.com/wildfly-extras/wildfly-camel/issues/107
[124]: https://github.com/wildfly-extras/wildfly-camel/issues/124
[134]: https://github.com/wildfly-extras/wildfly-camel/issues/134
[137]: https://github.com/wildfly-extras/wildfly-camel/issues/137
[145]: https://github.com/wildfly-extras/wildfly-camel/issues/145
[152]: https://github.com/wildfly-extras/wildfly-camel/issues/152
[153]: https://github.com/wildfly-extras/wildfly-camel/issues/153
[163]: https://github.com/wildfly-extras/wildfly-camel/issues/163
[172]: https://github.com/wildfly-extras/wildfly-camel/issues/172
[178]: https://github.com/wildfly-extras/wildfly-camel/issues/178
[182]: https://github.com/wildfly-extras/wildfly-camel/issues/182
[186]: https://github.com/wildfly-extras/wildfly-camel/issues/186
[187]: https://github.com/wildfly-extras/wildfly-camel/issues/187
[201]: https://github.com/wildfly-extras/wildfly-camel/issues/201
[218]: https://github.com/wildfly-extras/wildfly-camel/issues/218
[227]: https://github.com/wildfly-extras/wildfly-camel/issues/227
[231]: https://github.com/wildfly-extras/wildfly-camel/issues/231
[235]: https://github.com/wildfly-extras/wildfly-camel/issues/235
[242]: https://github.com/wildfly-extras/wildfly-camel/issues/242
[79]: https://github.com/wildfly-extras/wildfly-camel/issues/79
[130]: https://github.com/wildfly-extras/wildfly-camel/issues/130
[165]: https://github.com/wildfly-extras/wildfly-camel/issues/165
[167]: https://github.com/wildfly-extras/wildfly-camel/issues/167
[184]: https://github.com/wildfly-extras/wildfly-camel/issues/184
[192]: https://github.com/wildfly-extras/wildfly-camel/issues/192
[194]: https://github.com/wildfly-extras/wildfly-camel/issues/194
[217]: https://github.com/wildfly-extras/wildfly-camel/issues/217
[225]: https://github.com/wildfly-extras/wildfly-camel/issues/225
[229]: https://github.com/wildfly-extras/wildfly-camel/issues/229
[246]: https://github.com/wildfly-extras/wildfly-camel/issues/246

#### WildFly-Camel 2.0.0 CR1

**Features**

* [#3][3] Provide camel-cdi integration
* [#4][4] Provide camel-cxfrs integration
* [#5][5] Provide camel-jta integration
* [#6][6] Provide camel-jpa integration
* [#25][25] Provide camel-jaxb integration
* [#43][43] Provide camel-cxf soap integration
* [#44][44] Provide camel-cxf rest integration
* [#45][45] Provide secure camel-cxf soap integration
* [#46][46] Provide ActiveMQ integration
* [#47][47] Provide transacted camel-jms integration
* [#52][52] Provide camel-cdi integration
* [#62][62] Add support for wildfly sysprops in camel routes
* [#74][74] Provide Camel subsystem for domain mode

For details see [2.0.0 CR1 features](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.0.0+CR1"+label%3Afeature)

**Tasks**

* [#7][7] Remove provisioner usage from testsuite
* [#8][8] Switch to Apache License Version 2.0
* [#9][9] Move documentation to GitBook
* [#12][12] Provide docker distribution
* [#14][14] Remove dependency on jboss-logging
* [#15][15] Remove need to embed camel-cdi, deltaspike
* [#16][16] Find a common way to lookup camel contexts
* [#17][17] Verify compatibility with EAP 6.4
* [#19][19] Add switchyard supported camel components
* [#20][20] Remove hard coded dependency wiring for components
* [#42][42] Use cdi-api-1.0 for camel-cdi in eap-4.2
* [#50][50] Separate modules from wildfly patch
* [#73][73] Secure access to the Hawt.io console
* [#99][99] Build REST example as docker image

For details see [2.0.0 CR1 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.0.0+CR1"+label%3Atask)

**Bugs**

* [#15][15] Remove need to embed camel-cdi, deltaspike
* [#54][54] Examples run starts multiple wildfly instances
* [#56][56] JPA example leaves untracked files behind
* [#83][83] ActiveMQ integration test fails intermittently
* [#96][96] CXF SOAP Example does not trigger any Camel route to run

For details see [2.0.0 CR1 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A"2.0.0+CR1"+label%3Abug)

[3]: https://github.com/wildfly-extras/wildfly-camel/issues/3
[4]: https://github.com/wildfly-extras/wildfly-camel/issues/4
[5]: https://github.com/wildfly-extras/wildfly-camel/issues/5
[6]: https://github.com/wildfly-extras/wildfly-camel/issues/6
[25]: https://github.com/wildfly-extras/wildfly-camel/issues/25
[43]: https://github.com/wildfly-extras/wildfly-camel/issues/43
[44]: https://github.com/wildfly-extras/wildfly-camel/issues/44
[45]: https://github.com/wildfly-extras/wildfly-camel/issues/45
[46]: https://github.com/wildfly-extras/wildfly-camel/issues/46
[47]: https://github.com/wildfly-extras/wildfly-camel/issues/47
[52]: https://github.com/wildfly-extras/wildfly-camel/issues/52
[62]: https://github.com/wildfly-extras/wildfly-camel/issues/62
[74]: https://github.com/wildfly-extras/wildfly-camel/issues/74
[7]: https://github.com/wildfly-extras/wildfly-camel/issues/7
[8]: https://github.com/wildfly-extras/wildfly-camel/issues/8
[9]: https://github.com/wildfly-extras/wildfly-camel/issues/9
[12]: https://github.com/wildfly-extras/wildfly-camel/issues/12
[14]: https://github.com/wildfly-extras/wildfly-camel/issues/14
[15]: https://github.com/wildfly-extras/wildfly-camel/issues/15
[16]: https://github.com/wildfly-extras/wildfly-camel/issues/16
[17]: https://github.com/wildfly-extras/wildfly-camel/issues/17
[19]: https://github.com/wildfly-extras/wildfly-camel/issues/19
[20]: https://github.com/wildfly-extras/wildfly-camel/issues/20
[42]: https://github.com/wildfly-extras/wildfly-camel/issues/42
[50]: https://github.com/wildfly-extras/wildfly-camel/issues/50
[73]: https://github.com/wildfly-extras/wildfly-camel/issues/73
[99]: https://github.com/wildfly-extras/wildfly-camel/issues/99
[15]: https://github.com/wildfly-extras/wildfly-camel/issues/15
[54]: https://github.com/wildfly-extras/wildfly-camel/issues/54
[56]: https://github.com/wildfly-extras/wildfly-camel/issues/56
[83]: https://github.com/wildfly-extras/wildfly-camel/issues/83
[96]: https://github.com/wildfly-extras/wildfly-camel/issues/96