# Cloud Spanner Schema diff tool

This tool compares two Cloud Spanner Schema (DDL) files, determines the
differences and attempts to generate the required `ALTER` statements to convert
one schema to the other.

This tool is intended to work in a CI/CD pipeline to update the schema of an
existing Cloud Spanner database to a newer schema version.

The tool can only make changes that are allowable by Cloud Spanner `ALTER`
statements:

*   Add tables.
*   Add indexes.
*   Add columns to a table.
*   Change length limits of `STRING` or `BYTES` columns (or `ARRAYS` of `STRING`
    or `BYTES` columns).
*   Add or remove `NOT NULL` constraints on columns.
*   Add or remove column `OPTIONS` clauses.
*   Modify `ON DELETE` rules on interleaved child tables.

If the tool cannot parse the DDL files, or the changes between the two DDL files
cannot be made using ALTER statements (eg, change of column type, change of
interleaving status), then the tool will fail.

The tool relies on the DDL file being valid - specifically having the `CREATE`
statements in the correct order in the file, so that child tables are created
after their parents, and indexes are created after the table being indexed.

### Note on dropped database objects.

By default, to prevent accidental data loss, the tool _does not_ generate
statements to drop existing tables, indexes or columns. This behavior can be
overridden using the command line arguments `--allowDropStatements`, which
allows the tool to additionally generate the following statements for any
differences found.

*   Drop Tables.
*   Drop Indexes.
*   Drop Columns in a Table.

### Note on modified indexes

Modifications to indexes are not possible via `ALTER` statements, but if the
`--allowRecreateIndexes` command line option is specified, the tool can modify
indexes by first dropping then recreating them. This is a slow operation
especially on large tables, so is disabled by default, and index differences
will cause the tool to fail.

## Usage:

### Prerequsites:

Install Maven and a JAVA JRE

### Compile and run from source:

```sh
mvn generate-resources compile exec:java \
  -Dexec.mainClass=com.google.cloud.solutions.spannerddl.diff.DdlDiff \
  -Dexec.args="\
      --allowRecreateIndexes
      --originalDdlFile original.ddl
      --newDdlFile new.ddl
      --outputDdlFile alter.ddl
      "
```

Where: * `original.ddl` is a file containing the original database schema. *
`new.ddl` is a file containing the new/updated database schema. * `alter.ddl` is
the output file containing the generated `ALTER` statements. *
`--allowRecreateIndexes` allows index modifications by dropping/creating them.

### Generate JAR with dependencies and run from JAR file:

```sh
mvn clean generate-resources compile assembly:assembly

java -jar target/spanner-ddl-diff-*-jar-with-dependencies.jar \
      --allowRecreateIndexes \
      --originalDdlFile original.ddl \
      --newDdlFile new.ddl \
      --outputDdlFile alter.ddl
```

## Example input and output

### Original schema DDL input file

```
create table test1 (
    col1 int64,
    col2 int64,
    col3 STRING(100),
    col4 ARRAY<STRING(100)>,
    col5 float64 not null,
    col6 timestamp)
    primary key (col1 desc);

create index index1 on test1 (col1);

create table test2 (
    col1 int64)
    primary key (col1);

create index index2 on test2 (col1);

create table test3 (
    col1 int64,
    col2 int64 )
    primary key (col1, col2),
    interleave in parent test2
    on delete cascade;

create table test4 (col1 int64, col2 int64) primary key (col1);
create index index3 on test4 (col2);
```

### New schema DDL input file

```
create table test1 (
    col1 int64,
    col2 int64 NOT NULL,
    col3 STRING(MAX),
    col4 ARRAY<STRING(200)>,
    col5 float64 not null,
    newcol7 BYTES(100))
    primary key (col1 desc);

create index index1 on test1 (col2);

create table test2 (
    col1 int64,
    newcol2 string(max))
    primary key (col1);

create index index2 on test2 (col1 desc);

create table test3 (
    col1 int64,
    col2 int64,
    col3 timestamp )
    primary key (col1, col2),
    interleave in parent test2;
```

### Generated output DDL file

The arguments to the tool included `--allowDropStatements` and
`--allowRecreateIndexes`, so removed objects are dropped and modified indexes
are dropped and recreated.

```
DROP INDEX index3;

DROP INDEX index1;

DROP INDEX index2;

DROP TABLE test4;

ALTER TABLE test1 DROP COLUMN col6;

ALTER TABLE test1 ADD COLUMN newcol7 BYTES(100);

ALTER TABLE test1 ALTER COLUMN col2 INT64 NOT NULL;

ALTER TABLE test1 ALTER COLUMN col3 STRING(MAX);

ALTER TABLE test1 ALTER COLUMN col4 ARRAY<STRING(200)>;

ALTER TABLE test2 ADD COLUMN newcol2 STRING(MAX);

ALTER TABLE test3 SET ON DELETE NO ACTION;

ALTER TABLE test3 ADD COLUMN col3 TIMESTAMP;

CREATE INDEX index1 ON test1 (col2 ASC);

CREATE INDEX index2 ON test2 (col1 DESC);
```

## Help Text:

```
usage: DdlDiff [--allowDropStatements] [--allowRecreateIndexes] [--help] --newDdlFile <FILE> --originalDdlFile <FILE> --outputDdlFile <FILE>

Compares original and new DDL files and creates a DDL file with DROP, CREATE
and ALTER statements which convert the original Schema to the new Schema.

Incompatible table changes (table hierarchy changes. column type changes) are
not supported and will cause this tool to fail.

To prevent accidental data loss, DROP statements are not generated for removed
tables, columns and indexes. This can be overridden using the
--allowDropStatements command line argument.

By default, changes to indexes will also cause a failure. The
--allowRecreateIndexes command line option enables index changes by generating
statements to drop and recreate the index.

    --allowDropStatements       Enables output of DROP commands to delete
                                columns, tables or indexes not used in the new
                                DDL file.
    --allowRecreateIndexes      Allows dropping and recreating secondary
                                indexes to apply changes
    --help                      Show help
    --newDdlFile <FILE>         File path to the new DDL definition
    --originalDdlFile <FILE>    File path to the original DDL definition
    --outputDdlFile <FILE>      File path to the output DDL to write
```

## Usage in a CI/CD pipeline.

In a CI/CD pipeline, the tool should be run as follows:

*   Read the existing database schema into a file using the
    [`gcloud spanner databases ddl describe`](https://cloud.google.com/sdk/gcloud/reference/spanner/databases/ddl/describe)
    command.
*   Run the tool comparing the existing schema to the updated schema, writing
    the `ALTER` statements to a temp file.
*   Apply the ALTER statements to the existing database using the
    [`gcloud spanner databases ddl update`](https://cloud.google.com/sdk/gcloud/reference/spanner/databases/ddl/update)
    command.

For example, the following script:

```sh
#!/bin/bash

# Replace placeholders in these variable definitions.
SPANNER_INSTANCE="my-instance"
SPANNER_DATABASE="my-database"
UPDATED_SCHEMA_FILE="updated.ddl"

# Exit immediately on command failure.
set -e

# Read schema into a file, removing comments and adding semicolons between the statements.
gcloud spanner databases ddl describe "${SPANNER_DATABASE}" --instance="${SPANNER_INSTANCE}" \
    | sed 's/--- |-/;/' \
    > /tmp/original.ddl

# Generate ALTER statements.
java -jar target/spanner-ddl-diff-*-jar-with-dependencies.jar \
      --allowRecreateIndexes \
      --originalDdlFile /tmp/original.ddl \
      --newDdlFile "${UPDATED_SCHEMA_FILE}" \
      --outputDdlFile /tmp/alter.ddl

# Apply alter statements to the database.
gcloud spanner databases ddl update "${SPANNER_DATABASE}" --instance="${SPANNER_INSTANCE}" \
    --ddl="$(cat /tmp/alter.ddl)"

```

## License

```
Copyright 2020 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
