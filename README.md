# Cloud Spanner Schema diff tool

This tool compares two Cloud Spanner Schema (DDL) files, determines the
differences and attempts to generate the required `ALTER` statements to convert
one schema to the other.

This tool is intended to work in a CI/CD pipeline to update the schema of an
existing Cloud Spanner database to a newer schema version.

The tool can only make changes that are allowable by Cloud Spanner's `CREATE`,
`DROP` and `ALTER` statements:

* Add or remove tables.
* Add or remove indexes.
* Add or remove table columns.
* Add or remove _named_ `FOREIGN KEY` and `CHECK` constraints.
* Change length limits of `STRING` or `BYTES` columns (or `ARRAYS` of `STRING`
  or `BYTES` columns).
* Add or remove `NOT NULL` constraints on columns.
* Add or remove column `OPTIONS` clauses.
* Modify `ON DELETE` rules on interleaved child tables.

If the tool cannot parse the DDL files, or the changes between the two DDL files
cannot be made using ALTER statements (eg, change of column type, change of
interleaving status), then the tool will fail.

The tool relies on the DDL file being valid - specifically having the `CREATE`
statements in the correct order in the file, so that child tables are created
after their parents, and indexes are created after the table being indexed.

The tool has no concept of the "structure" of the database, only of the
statements in the DDL file. This has the following implications:

* The tool relies on the DDL files being valid - specifically having the
  `CREATE` statements in the correct order in the file, so that child tables
  are created _after_ their parents, and indexes are created _after_ the table
  being indexed.

* The tool relies on the expressions in `CHECK` constraints and generated
  columns being valid - it itself does noy understand SQL expressions and
  just performs text comparison.

* Tables and indexes must be created with a single `CREATE` statement (not by
  using `CREATE` then `ALTER` statements). The exception to this is when
  constraints and row deletion policies are created - the tool supports creating
  them in the
  table creation DDL statement, and also by using `ALTER` statements after the
  table has been created.

### Note on dropped database objects.

By default, to prevent accidental data loss, the tool _does not_ generate
statements to drop existing tables, indexes or columns. This behavior can be
overridden using the command line arguments `--allowDropStatements`, which
allows the tool to additionally generate the following statements for any
differences found:

* Drop Tables.
* Drop Indexes.
* Drop Columns in a Table.

Constraints and row deletion policies will always be dropped if they are not
present in the new DDL.

This also helps to prevent edge cases which are not handled by this tool.

Consider for example, a DDL object such as a check constraint, default value
calculation or a row deletion policy clause that has an expression referencing
a column that is going to be removed, and will be changed to reference a column
that is being added.

For this to work properly things need to happen in the following order:

1) the object with the expression referencing the existing column needs to be
dropped
2) the column dropped
3) the new column added
4) finally the object re-created with the new expression referencing the new
column.

As this tool does not _understand_ the contents of the expression, it cannot
know that this is required, so by not dropping the column, steps 1 and 2 are
not required.

### Note on modified indexes

Modifications to indexes are not possible via `ALTER` statements, but if the
`--allowRecreateIndexes` command line option is specified, the tool can modify
indexes by first dropping then recreating them. This is a slow operation
especially on large tables, so is disabled by default, and index differences
will cause the tool to fail.

### Note on constraints

`FOREIGN KEY` amd `CHECK` constraints _must_ be explicitly named, either within
a
`CREATE TABLE` statement, or using an `ALTER TABLE` statement, using the syntax:

```sql
CONSTRAINT fk_constraint_name FOREIGN KEY ...
CONSTRAINT ck_constraint_name CHECK ...
```

This is because the constraint needs to be referenced by its _name_ when it is
dropped.

Anonymous `FOREIGN KEY` or `CHECK` constraints of the form:

```sql
CREATE TABLE fk_dest
(
    key        INT64,
    source_key INT64,
    FOREIGN KEY (source_key) REFERENCES fk_source (key)
) PRIMARY KEY (key);
```

will be rejected when the DDL is parsed.

Modifications to existing `FOREIGN KEY` constraints are not possible via `ALTER`
statements, but if the `--allowRecreateForeignKeys` command line option is
specified, the tool can modify foreign keys by first dropping then recreating
them. This is a slow operation - especially on large tables - because the
foreign key relationship is
[backed by hidden indexes](https://cloud.google.com/spanner/docs/foreign-keys/overview#backing-indexes)
which would also be dropped and recreated. Therefore this option is disabled by
default, and FOREIGN KEY differences will cause the tool to fail.

## Unsupported Spanner DDL features

This tool by neccessity will lag behind the implementation of new DDL features
in Spanner. If you need this tool to support a specific feature, please log an
issue, or [implement it yourself](AddingCapabilities.md) and submit a Pull
Request.

As of the last edit of this file, the features known not to be supported are:

* Change Streams (create, drop, alter)
* `ALTER DATABASE` statements
* Default column values (`DEFAULT` clause)
* Views (create, drop, alter)

## Usage:

### Prerequisites:

Install a [JAVA development kit](https://jdk.java.net/) (supporting Java 8
or above) and [Apache Maven](https://maven.apache.org/)

There are 2 options for running the tool: either compiling and running from
source, or by building a runnable JAR with all dependencies included and then
running that.

The following commands direct the tool to read the original.ddl file, compare it
to the new.ddl file, and generate an alter.ddl file. The options specify that
modified indexes and foreign keys will be dropped and recreated, but removed
tables, columns and indexes will not be dropped.

### Compile and run directly from source:

```sh
mvn generate-resources compile exec:java \
  -Dexec.mainClass=com.google.cloud.solutions.spannerddl.diff.DdlDiff \
  -Dexec.args="\
      --allowRecreateIndexes
      --allowRecreateForeignKeys
      --originalDdlFile original.ddl
      --newDdlFile new.ddl
      --outputDdlFile alter.ddl
      "
```

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

```sql
create table test1
(
    col1 int64,
    col2 int64,
    col3 STRING(100),
    col4 ARRAY<STRING(100)>,
    col5 float64 not null,
    col6 timestamp
) primary key (col1 desc);

create index index1 on test1 (col1);

create table test2
(
    col1 int64
) primary key (col1);

create index index2 on test2 (col1);

create table test3
(
    col1 int64,
    col2 int64
) primary key (col1, col2),
    interleave in parent test2
    on
delete
cascade;

create table test4
(
    col1 int64,
    col2 int64
) primary key (col1);
create index index3 on test4 (col2);
```

### New schema DDL input file

```sql
create table test1
(
    col1    int64,
    col2    int64   NOT NULL,
    col3    STRING( MAX),
    col4    ARRAY<STRING(200)>,
    col5    float64 not null,
    newcol7 BYTES(100)
) primary key (col1 desc);

create index index1 on test1 (col2);

create table test2
(
    col1    int64,
    newcol2 string( max)
) primary key (col1);

create index index2 on test2 (col1 desc);

create table test3
(
    col1 int64,
    col2 int64,
    col3 timestamp
) primary key (col1, col2),
    interleave in parent test2;
```

### Generated output DDL file

The arguments to the tool included `--allowDropStatements` and
`--allowRecreateIndexes`, so removed objects are dropped and modified indexes
are dropped and recreated.

```sql
DROP INDEX index3;

DROP INDEX index1;

DROP INDEX index2;

DROP TABLE test4;

ALTER TABLE test1 DROP COLUMN col6;

ALTER TABLE test1
    ADD COLUMN newcol7 BYTES(100);

ALTER TABLE test1 ALTER COLUMN col2 INT64 NOT NULL;

ALTER TABLE test1 ALTER COLUMN col3 STRING(MAX);

ALTER TABLE test1 ALTER COLUMN col4 ARRAY<STRING(200)>;

ALTER TABLE test2
    ADD COLUMN newcol2 STRING(MAX);

ALTER TABLE test3 SET ON DELETE NO ACTION;

ALTER TABLE test3
    ADD COLUMN col3 TIMESTAMP;

CREATE INDEX index1 ON test1 (col2 ASC);

CREATE INDEX index2 ON test2 (col1 DESC);
```

## Help Text:

```
usage: DdlDiff [--allowDropStatements] [--allowRecreateForeignKeys] [--allowRecreateIndexes] [--help] --newDdlFile <FILE> --originalDdlFile <FILE> --outputDdlFile <FILE>
Compares original and new DDL files and creates a DDL file with DROP, CREATE 
and ALTER statements which convert the original Schema to the new Schema.

Incompatible table changes (table hierarchy changes. column type changes) are
 not supported and will cause this tool to fail.

To prevent accidental data loss, DROP statements are not generated for removed
tables, columns and indexes. This can be overridden using the
 --allowDropStatements command line argument.

By default, changes to indexes will also cause a failure. The
--allowRecreateIndexes command line option enables index changes by
generating statements to drop and recreate the index.

By default, changes to foreign key constraints will also cause a failure. The
--allowRecreateForeignKeys command line option enables foreign key changes by
generating statements to drop and recreate the constraint.

    --allowDropStatements         Enables output of DROP commands to delete
                                  columns, tables or indexes not used in the
                                  new DDL file.
    --allowRecreateForeignKeys    Allows dropping and recreating Foreign Keys
                                  (and their backing Indexes) to apply changes.
    --allowRecreateIndexes        Allows dropping and recreating secondary
                                  Indexes to apply changes.
    --help                        Show help.
    --newDdlFile <FILE>           File path to the new DDL definition.
    --originalDdlFile <FILE>      File path to the original DDL definition.
    --outputDdlFile <FILE>        File path to the output DDL to write.
```

## Usage in a CI/CD pipeline.

In a CI/CD pipeline, the tool should be run as follows:

* Read the existing database schema into a file using the
  [`gcloud spanner databases ddl describe`](https://cloud.google.com/sdk/gcloud/reference/spanner/databases/ddl/describe)
  command.
* Run the tool comparing the existing schema to the updated schema, writing
  the `ALTER` statements to a temp file.
* Apply the ALTER statements to the existing database using the
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
gcloud spanner databases ddl describe \
    --instance="${SPANNER_INSTANCE}" "${SPANNER_DATABASE}" \
    --format='value(format("{0};\
    "))' > /tmp/original.ddl

# Generate ALTER statements.
java -jar target/spanner-ddl-diff-*-jar-with-dependencies.jar \
      --allowRecreateIndexes \
      --allowRecreateForeignKeys \
      --originalDdlFile /tmp/original.ddl \
      --newDdlFile "${UPDATED_SCHEMA_FILE}" \
      --outputDdlFile /tmp/alter.ddl

# Apply alter statements to the database.
gcloud spanner databases ddl update "${SPANNER_DATABASE}" --instance="${SPANNER_INSTANCE}" \
    --ddl-file=/tmp/alter.ddl

```

## License

```
Copyright 2023 Google LLC

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

