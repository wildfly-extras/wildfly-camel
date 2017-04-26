<!--
  #%L
  Wildfly Camel :: Testsuite
  %%
  Copyright (C) 2013 - 2016 RedHat
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  -->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">

    <sch:title>WildFly-Camel Schematron Validation Test</sch:title>
    <sch:ns prefix="xs" uri="http://www.w3.org/2001/XMLSchema"/>
    <sch:ns prefix="p" uri="http://www.apache.org/camel/schematron"/>

    <sch:pattern>
        <sch:rule context="person | p:person">
            <sch:assert test="title | p:title">A person should have a title</sch:assert>
        </sch:rule>
    </sch:pattern>

</sch:schema>