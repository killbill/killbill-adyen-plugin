In order to generate the Jooq classes:

M2_REPOS=~/.m2/repository;
JOOQ="$M2_REPOS/org/jooq";
MYSQL="$M2_REPOS/mysql/mysql-connector-java/5.1.22/mysql-connector-java-5.1.22.jar";
JARS="$JOOQ/jooq/3.3.1/jooq-3.3.1.jar:$JOOQ/jooq-meta/3.3.1/jooq-meta-3.3.1.jar:$JOOQ/jooq-codegen/3.3.1/jooq-codegen-3.3.1.jar:$MYSQL:.";

java -cp $JARS org.jooq.util.GenerationTool  src/main/resources/gen.xml