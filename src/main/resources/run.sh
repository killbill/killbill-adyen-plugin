#
# Copyright 2020-2023 Equinix, Inc
# Copyright 2014-2023 The Billing Project, LLC
#
# The Billing Project licenses this file to you under the Apache License, version 2.0
# (the "License"); you may not use this file except in compliance with the
# License.  You may obtain a copy of the License at:
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations
# under the License. 
#
MVN_DOWNLOAD="mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get -DremoteRepositories=https://repo.maven.apache.org/maven2"
JOOQ_VERSION=3.14.0
$MVN_DOWNLOAD -Dartifact=org.jooq:jooq:$JOOQ_VERSION:jar
$MVN_DOWNLOAD -Dartifact=org.jooq:jooq-meta:$JOOQ_VERSION:jar
$MVN_DOWNLOAD -Dartifact=org.jooq:jooq-codegen:$JOOQ_VERSION:jar
$MVN_DOWNLOAD -Dartifact=javax.xml.bind:jaxb-api:2.3.1:jar
$MVN_DOWNLOAD -Dartifact=io.r2dbc:r2dbc-spi:0.8.6.RELEASE:jar


REACTIVE_STREAM_VERSION=1.0.2
$MVN_DOWNLOAD -Dartifact=org.reactivestreams:reactive-streams:$REACTIVE_STREAM_VERSION:jar
MYSQL_VERSION=8.0.21
$MVN_DOWNLOAD -Dartifact=mysql:mysql-connector-java:$MYSQL_VERSION:jar

M2_REPOS=~/.m2/repository
JOOQ="$M2_REPOS/org/jooq"
MYSQL="$M2_REPOS/mysql/mysql-connector-java/$MYSQL_VERSION/mysql-connector-java-$MYSQL_VERSION.jar"
REACTIVE_STREAMS="$M2_REPOS/org/reactivestreams/reactive-streams/$REACTIVE_STREAM_VERSION/reactive-streams-$REACTIVE_STREAM_VERSION.jar"
jaxb="$M2_REPOS/javax/xml/bind/jaxb-api/2.3.1/jaxb-api-2.3.1.jar"
r2dbc="$M2_REPOS/io/r2dbc/r2dbc-spi/0.8.6.RELEASE/r2dbc-spi-0.8.6.RELEASE.jar"

JARS="$JOOQ/jooq/$JOOQ_VERSION/jooq-$JOOQ_VERSION.jar:$JOOQ/jooq-meta/$JOOQ_VERSION/jooq-meta-$JOOQ_VERSION.jar:$JOOQ/jooq-codegen/$JOOQ_VERSION/jooq-codegen-$JOOQ_VERSION.jar:$REACTIVE_STREAMS:$MYSQL:$jaxb:$r2dbc:.";

java -cp $JARS org.jooq.codegen.GenerationTool ./src/main/resources/gen.xml