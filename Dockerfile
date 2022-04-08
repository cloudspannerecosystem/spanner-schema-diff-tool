FROM maven:3.8-openjdk-11

WORKDIR /app

COPY ./target/spanner-ddl-diff-1.0-jar-with-dependencies.jar .

ENTRYPOINT java -jar spanner-ddl-diff-*-jar-with-dependencies.jar \
      --allowDropStatements \
      --allowRecreateIndexes \
      --allowRecreateConstraints \
      --originalDdlFile ./mount/original.ddl \
      --newDdlFile ./mount/target.ddl \
      --outputDdlFile ./mount/alter.ddl \
