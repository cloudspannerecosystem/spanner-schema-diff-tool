#!/bin/bash

# Replace placeholders in these variable definitions.
SPANNER_INSTANCE="test-spanner-consumer"
SPANNER_DATABASE="zhiwei-test"
OLD_SCHEMA_FILE=/tmp/original.ddl
UPDATED_SCHEMA_FILE=$1

# Exit immediately on command failure.
set -e

# Read schema into a file, removing comments and adding semicolons between the statements.
gcloud spanner databases ddl describe \
    --instance="${SPANNER_INSTANCE}" "${SPANNER_DATABASE}" \
    --format='value(format("{0};"))' > "${OLD_SCHEMA_FILE}"

echo "Output current schema to ${OLD_SCHEMA_FILE}"
echo "Generating diff to ${UPDATED_SCHEMA_FILE}..."

# Generate ALTER statements.
# --allowRecreateConstraints doesn't work well
java -jar target/spanner-ddl-diff-*-jar-with-dependencies.jar \
      --allowRecreateIndexes \
      --allowDropStatements \
      --allowRecreateConstraints \
      --originalDdlFile "${OLD_SCHEMA_FILE}" \
      --newDdlFile "${UPDATED_SCHEMA_FILE}" \
      --outputDdlFile /tmp/alter.ddl

