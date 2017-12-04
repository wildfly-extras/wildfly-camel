Release Notes
-------------------

WildFly-Camel 5.0.0 provides Camel-2.20.1 integration with WildFly-11.0.0

This is a major upgrade release for supported components, dataformats and languages, which now reaches feature parity with other runtimes. i.e. all available dataformats and languages are now also supported on WildFly.


Additional components in the [supported set](http://wildfly-extras.github.io/wildfly-camel/#_camel_components) are:

* [apns](http://wildfly-extras.github.io/wildfly-camel/#_apns)
* [asterisk](http://wildfly-extras.github.io/wildfly-camel/#_asterisk)
* [atomix](http://wildfly-extras.github.io/wildfly-camel/#_atomix)
* [azure-blob](http://wildfly-extras.github.io/wildfly-camel/#_azure-blob)
* [azure-queue](http://wildfly-extras.github.io/wildfly-camel/#_azure-queue)
* [beanstalk](http://wildfly-extras.github.io/wildfly-camel/#_beanstalk)
* [caffeine](http://wildfly-extras.github.io/wildfly-camel/#_caffeine)
* [chronicle-engine](http://wildfly-extras.github.io/wildfly-camel/#_chronicle-engine)
* [chunk](http://wildfly-extras.github.io/wildfly-camel/#_chunk)
* [cm-sms](http://wildfly-extras.github.io/wildfly-camel/#_cm-sms)
* [consul](http://wildfly-extras.github.io/wildfly-camel/#_consul)
* [couchbase](http://wildfly-extras.github.io/wildfly-camel/#_couchbase)
* [crypto-cms](http://wildfly-extras.github.io/wildfly-camel/#_crypto-cms)
* [digitalocean](http://wildfly-extras.github.io/wildfly-camel/#_digitalocean)
* [docker](http://wildfly-extras.github.io/wildfly-camel/#_docker)
* [elasticsearch5](http://wildfly-extras.github.io/wildfly-camel/#_elasticsearch5)
* [etcd](http://wildfly-extras.github.io/wildfly-camel/#_etcd)
* [flink](http://wildfly-extras.github.io/wildfly-camel/#_flink)
* [google-bigquery](http://wildfly-extras.github.io/wildfly-camel/#_google-bigquery)
* [google-calendar](http://wildfly-extras.github.io/wildfly-camel/#_google-calendar)
* [google-drive](http://wildfly-extras.github.io/wildfly-camel/#_google-drive)
* [google-mail](http://wildfly-extras.github.io/wildfly-camel/#_google-mail)
* [grpc](http://wildfly-extras.github.io/wildfly-camel/#_grpc)
* [guava-eventbus](http://wildfly-extras.github.io/wildfly-camel/#_guava-eventbus)
* [hazelcast](http://wildfly-extras.github.io/wildfly-camel/#_hazelcast)
* [headersmap](http://wildfly-extras.github.io/wildfly-camel/#_headersmap)
* [hipchat](http://wildfly-extras.github.io/wildfly-camel/#_hipchat)
* [iec60870](http://wildfly-extras.github.io/wildfly-camel/#_iec60870)
* [jclouds](http://wildfly-extras.github.io/wildfly-camel/#_jclouds)
* [jcr](http://wildfly-extras.github.io/wildfly-camel/#_jcr)
* [json-validator](http://wildfly-extras.github.io/wildfly-camel/#_json-validator)
* [jt400](http://wildfly-extras.github.io/wildfly-camel/#_jt400)
* [ldif](http://wildfly-extras.github.io/wildfly-camel/#_ldif)
* [leveldb](http://wildfly-extras.github.io/wildfly-camel/#_leveldb)
* [lumberjack](http://wildfly-extras.github.io/wildfly-camel/#_lumberjack)
* [master](http://wildfly-extras.github.io/wildfly-camel/#_master)
* [milo](http://wildfly-extras.github.io/wildfly-camel/#_milo)
* [mongodb-gridfs](http://wildfly-extras.github.io/wildfly-camel/#_mongodb-gridfs)
* [nagios](http://wildfly-extras.github.io/wildfly-camel/#_nagios)
* [olingo4](http://wildfly-extras.github.io/wildfly-camel/#_olingo4)
* [openstack-cinder](http://wildfly-extras.github.io/wildfly-camel/#_openstack-cinder)
* [openstack-glance](http://wildfly-extras.github.io/wildfly-camel/#_openstack-glance)
* [openstack-keystone](http://wildfly-extras.github.io/wildfly-camel/#_openstack-keystone)
* [openstack-neutron](http://wildfly-extras.github.io/wildfly-camel/#_openstack-neutron)
* [openstack-nova](http://wildfly-extras.github.io/wildfly-camel/#_openstack-nova)
* [openstack-swift](http://wildfly-extras.github.io/wildfly-camel/#_openstack-swift)
* [printer](http://wildfly-extras.github.io/wildfly-camel/#_printer)
* [pubnub](http://wildfly-extras.github.io/wildfly-camel/#_pubnub)
* [quickfix](http://wildfly-extras.github.io/wildfly-camel/#_quickfix)
* [reactor](http://wildfly-extras.github.io/wildfly-camel/#_reactor)
* [rmi](http://wildfly-extras.github.io/wildfly-camel/#_rmi)
* [shiro](http://wildfly-extras.github.io/wildfly-camel/#_shiro)
* [sip](http://wildfly-extras.github.io/wildfly-camel/#_sip)
* [sips](http://wildfly-extras.github.io/wildfly-camel/#_sips)
* [slack](http://wildfly-extras.github.io/wildfly-camel/#_slack)
* [spring-javaconfig](http://wildfly-extras.github.io/wildfly-camel/#_spring-javaconfig)
* [spring-ws](http://wildfly-extras.github.io/wildfly-camel/#_spring-ws)
* [stomp](http://wildfly-extras.github.io/wildfly-camel/#_stomp)
* [telegram](http://wildfly-extras.github.io/wildfly-camel/#_telegram)
* [thrift](http://wildfly-extras.github.io/wildfly-camel/#_thrift)
* [twilio](http://wildfly-extras.github.io/wildfly-camel/#_twilio)
* [xmpp](http://wildfly-extras.github.io/wildfly-camel/#_xmpp)
* [yammer](http://wildfly-extras.github.io/wildfly-camel/#_yammer)
* [zookeeper-master](http://wildfly-extras.github.io/wildfly-camel/#_zookeeper-master)

Additional data formats in the [supported set](http://wildfly-extras.github.io/wildfly-camel/#_data_formats) are:

* asn1
* fastjson
* thrift

Component upgrades include

* WildFly-11.0.0
* Camel-2.20.1
* Hawtio-1.5.5


In addition to that, we also resolved a number of other [tasks and bugs](https://github.com/wildfly-extras/wildfly-camel/blob/master/docs/Changelog.md).

For details please see the [5.0.0 Milestone](https://github.com/wildfly-extras/wildfly-camel/issues?q=milestone%3A5.0.0).

Enjoy
