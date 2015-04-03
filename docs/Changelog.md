### Changelog

#### WildFly-Camel 2.2.0

**Features**

* [#32][32] Provide camel-quartz2 integration
* [#33][33] Provide camel-rss integration
* [#34][34] Provide camel-saxon integration
* [#35][35] Provide camel-script integration
* [#37][37] Provide camel-sql integration
* [#157][157] Provide camel-servlet integration
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

**Tasks**

* [#248][248] Review and improve JAX-WS example
* [#249][249] Review and improve JAX-RS example
* [#250][250] Review and improve ActiveMQ/JMS example
* [#251][251] Review and improve mail example
* [#264][264] Explicitly define and enforce set of publicly exposed packages
* [#306][306] Update to arquillian-1.1.7.Final
* [#320][320] Update to camel-2.15.0
* [#331][331] Add custom module for CXF 3.0.2
* [#485][485] Update to hawtio-1.4.48
* A number of minor tasks

For details see [2.2.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A2.2.0+label%3Atask)

**Bugs**

* [#254][254] Unable to load custom exception in load balancer
* [#255][255] Unable to load properties file from classpath
* [#319][319] Unable to locate Spring JEE Namespace Handler
* [#347][347] Generated module definitions contain optional dependencies
* [#381][381] Unable to configure ActiveMQ resource adapter
* [#391][391] Cannot obtain DOMImplementationRegistry instance
* [#410][410] Cannot load mina type converter
* A number of minor bugfixes

For details see [2.2.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A2.2.0+label%3Abug)

[32]: https://github.com/wildflyext/wildfly-camel/issues/32
[33]: https://github.com/wildflyext/wildfly-camel/issues/33
[34]: https://github.com/wildflyext/wildfly-camel/issues/34
[35]: https://github.com/wildflyext/wildfly-camel/issues/35
[37]: https://github.com/wildflyext/wildfly-camel/issues/37
[157]: https://github.com/wildflyext/wildfly-camel/issues/157
[158]: https://github.com/wildflyext/wildfly-camel/issues/158
[159]: https://github.com/wildflyext/wildfly-camel/issues/159
[161]: https://github.com/wildflyext/wildfly-camel/issues/161
[162]: https://github.com/wildflyext/wildfly-camel/issues/162
[199]: https://github.com/wildflyext/wildfly-camel/issues/199
[206]: https://github.com/wildflyext/wildfly-camel/issues/206
[233]: https://github.com/wildflyext/wildfly-camel/issues/233
[237]: https://github.com/wildflyext/wildfly-camel/issues/237
[245]: https://github.com/wildflyext/wildfly-camel/issues/245
[248]: https://github.com/wildflyext/wildfly-camel/issues/248
[249]: https://github.com/wildflyext/wildfly-camel/issues/249
[250]: https://github.com/wildflyext/wildfly-camel/issues/250
[251]: https://github.com/wildflyext/wildfly-camel/issues/251
[254]: https://github.com/wildflyext/wildfly-camel/issues/254
[255]: https://github.com/wildflyext/wildfly-camel/issues/255
[264]: https://github.com/wildflyext/wildfly-camel/issues/264
[306]: https://github.com/wildflyext/wildfly-camel/issues/306
[319]: https://github.com/wildflyext/wildfly-camel/issues/319
[320]: https://github.com/wildflyext/wildfly-camel/issues/320
[328]: https://github.com/wildflyext/wildfly-camel/issues/328
[331]: https://github.com/wildflyext/wildfly-camel/issues/331
[347]: https://github.com/wildflyext/wildfly-camel/issues/347
[366]: https://github.com/wildflyext/wildfly-camel/issues/366
[367]: https://github.com/wildflyext/wildfly-camel/issues/367
[368]: https://github.com/wildflyext/wildfly-camel/issues/368
[373]: https://github.com/wildflyext/wildfly-camel/issues/373
[381]: https://github.com/wildflyext/wildfly-camel/issues/381
[385]: https://github.com/wildflyext/wildfly-camel/issues/385
[390]: https://github.com/wildflyext/wildfly-camel/issues/390
[391]: https://github.com/wildflyext/wildfly-camel/issues/391
[392]: https://github.com/wildflyext/wildfly-camel/issues/392
[394]: https://github.com/wildflyext/wildfly-camel/issues/394
[410]: https://github.com/wildflyext/wildfly-camel/issues/410
[415]: https://github.com/wildflyext/wildfly-camel/issues/415
[442]: https://github.com/wildflyext/wildfly-camel/issues/442
[444]: https://github.com/wildflyext/wildfly-camel/issues/444
[446]: https://github.com/wildflyext/wildfly-camel/issues/446
[450]: https://github.com/wildflyext/wildfly-camel/issues/450
[451]: https://github.com/wildflyext/wildfly-camel/issues/451
[452]: https://github.com/wildflyext/wildfly-camel/issues/452
[453]: https://github.com/wildflyext/wildfly-camel/issues/453
[454]: https://github.com/wildflyext/wildfly-camel/issues/454
[455]: https://github.com/wildflyext/wildfly-camel/issues/455
[456]: https://github.com/wildflyext/wildfly-camel/issues/456
[457]: https://github.com/wildflyext/wildfly-camel/issues/457
[458]: https://github.com/wildflyext/wildfly-camel/issues/458
[459]: https://github.com/wildflyext/wildfly-camel/issues/459
[485]: https://github.com/wildflyext/wildfly-camel/issues/485
[498]: https://github.com/wildflyext/wildfly-camel/issues/498
[499]: https://github.com/wildflyext/wildfly-camel/issues/499
[500]: https://github.com/wildflyext/wildfly-camel/issues/500
[501]: https://github.com/wildflyext/wildfly-camel/issues/501

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
* [#239][239] Make ConfigurationAdmin accessible through JMX

**Tasks**

* [#93][93] Support WildFly domain mode in OpenShift
* [#95][95] Upgrade to WildFly 8.2.0.Final
* [#134][134] Upgrade to hawtio-1.4.42
* [#182][182] Use management realm for hawtio authentication
* [#201][201] Update to camel-2.14.1
* [#227][227] Put wildfly modules in a layer
* [#242][242] Make CamelContextFactory available as Gravia service
* A number of minor tasks

For details see [2.1.0 tasks](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A2.1.0+label%3Atask)

**Bugs**

* A number of minor bugfixes

For details see [2.1.0 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A2.1.0+label%3Abug)

[2]: https://github.com/wildflyext/wildfly-camel/issues/2
[21]: https://github.com/wildflyext/wildfly-camel/issues/21
[22]: https://github.com/wildflyext/wildfly-camel/issues/22
[23]: https://github.com/wildflyext/wildfly-camel/issues/23
[24]: https://github.com/wildflyext/wildfly-camel/issues/24
[26]: https://github.com/wildflyext/wildfly-camel/issues/26
[27]: https://github.com/wildflyext/wildfly-camel/issues/27
[28]: https://github.com/wildflyext/wildfly-camel/issues/28
[29]: https://github.com/wildflyext/wildfly-camel/issues/29
[30]: https://github.com/wildflyext/wildfly-camel/issues/30
[31]: https://github.com/wildflyext/wildfly-camel/issues/31
[75]: https://github.com/wildflyext/wildfly-camel/issues/75
[93]: https://github.com/wildflyext/wildfly-camel/issues/93
[95]: https://github.com/wildflyext/wildfly-camel/issues/95
[134]: https://github.com/wildflyext/wildfly-camel/issues/134
[140]: https://github.com/wildflyext/wildfly-camel/issues/140
[142]: https://github.com/wildflyext/wildfly-camel/issues/142
[148]: https://github.com/wildflyext/wildfly-camel/issues/148
[182]: https://github.com/wildflyext/wildfly-camel/issues/182
[201]: https://github.com/wildflyext/wildfly-camel/issues/201
[227]: https://github.com/wildflyext/wildfly-camel/issues/227
[239]: https://github.com/wildflyext/wildfly-camel/issues/239
[242]: https://github.com/wildflyext/wildfly-camel/issues/242

#### WildFly-Camel 2.0.0.CR1

**Features**

* [#5][5] Provide camel-jta integration
* [#25][25] Provide camel-jaxb integration
* [#43][43] Provide camel-cxf soap integration
* [#44][44] Provide camel-cxf rest integration
* [#45][45] Provide secure camel-cxf soap integration
* [#46][46] Provide ActiveMQ integration
* [#47][47] Provide transacted camel-jms integration
* [#52][52] Provide camel-cdi integration
* [#62][62] Add support for wildfly sysprops in camel routes
* [#74][74] Provide Camel subsystem for domain mode

**Tasks**

* [#19][19] Add switchyard supported camel components
* [#73][73] Secure access to the Hawt.io console

**Bugs**

* A number of minor bugfixes

For details see [2.0.0.CR1 bugs](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A%222.0.0+CR1%22+label%3Abug)

[5]: https://github.com/wildflyext/wildfly-camel/issues/5
[19]: https://github.com/wildflyext/wildfly-camel/issues/19
[25]: https://github.com/wildflyext/wildfly-camel/issues/25
[43]: https://github.com/wildflyext/wildfly-camel/issues/43
[44]: https://github.com/wildflyext/wildfly-camel/issues/44
[45]: https://github.com/wildflyext/wildfly-camel/issues/45
[46]: https://github.com/wildflyext/wildfly-camel/issues/46
[47]: https://github.com/wildflyext/wildfly-camel/issues/47
[52]: https://github.com/wildflyext/wildfly-camel/issues/52
[62]: https://github.com/wildflyext/wildfly-camel/issues/62
[73]: https://github.com/wildflyext/wildfly-camel/issues/73
[74]: https://github.com/wildflyext/wildfly-camel/issues/74

