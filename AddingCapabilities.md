# Adding capabilities

## Sync parser code with cloud spanner emulator

1) Clone the Cloud Spanner Emulator repo from `https://github.com/GoogleCloudPlatform/cloud-spanner-emulator`

1) Generate the `ddl_keywords.jjt` file in backend/schema/parser module in order
to build the `ddl_keywords.jjt`

`bazel build backend/schema/parser:ddl_keywords_jjt`

1) Compare and synchronize the generated `ddl_keywords.jjt`
in `bazel-bin/bazel-bin/backend/schema/parser/` with
the [ddl_keywords.jjt](src%2Fmain%2Fjjtree-sources%2Fddl_keywords.jjt) in
this repo

3) Compare and synchronise following files
in [src/main/jjtree-sources](src%2Fmain%2Fjjtree-sources) with the emulator's
versions in
the [backend/schema/parser](https://github.com/GoogleCloudPlatform/cloud-spanner-emulator/tree/master/backend/schema/parser)
directory)

* `ddl_expression.jjt`
* `ddl_parser.jjt`
* `ddl_string_bytes_tokens.jjt`
* `ddl_whitespace.jjt` - Note that this file does not have
  the `ValidateStringLiteral()`
  and `ValidateBytesLiteral()` functions - this is intentional.

## Invalidate AST wrappers that have been added

1) Look at the differences in `ddk_parser.jjt`

2) Make notes of all the statements and parameters that have been added

4) Run `mvn clean compile`

This will compile the JJT files, and generate any missing AST classes
in `target/generated-sources/jjtree`

6) For each parser capability that has been added, do the following
(A new parser capability may be a new top-level statement, or a table or
column option):

* Copy the AST java file
  to `src/main/java/com/google/cloud/solutions/spannerddl/parser`
* Clean the parser prefix/suffix comments, and add an Apache II licence
  header
* Add the following code to the constructors: \
  `throw new UnsupportedOperationException("Not Implemented");`

Doing this for all the newly added parser classes ensures that the parser
will fail when parsing DDL with these statements, rather than generating
invalid output.

You do not necessarily have to do this for all classes, but for ones which
add new high level parser capabilities such as top level statements, table
options, etc.

5) Optionally, but recommended add test cases to the `DDLParserTest` class to
verify that these unsupported
operations fail during parsing.

## Run a test and fix bugs

The changes to the parser may have introduced new AST classes that the existing
parser code may not be expecting. Run a `mvn clean verify` in order to re-run
the tests.

## Implement the toString and equals methods for the new AST capability classes

The `toString()` and probably the `equals()` methods need to be implemented in
the new AST classes for implementing the diff capability.

For end-branches of the AST, the toString() method can be as simple as
regenerating the original tokens using the helper method:

```java
ASTTreeUtils.tokensToString(node.firstToken,node.lastToken);
```

This will iterate down the AST regenerating the tokens into a normalized form
(ie single spaces, capitalized reserved words etc).

Once you have a toString which generates a normalized form, the equals method
can simply do a string comparison... Lazy but it works!

However for more complex classes, like Tables, the implementation is necessarily
more complex, extracting the optional and repeated nodes (eg for Table, columns,
constraints, primary key, interleave etc), and then rebuilding the original
statement in the toString()

If you only implement some of the functionality, the `toString()` method is a
good place to put some validation - checking only for supported child nodes. See
the Table and colum definition classes for examples.

Once this is done, you can run some tests in the `DDLParserTest` class to verify
that the parser works, and that the toString() method regenerates the original
statement.

## Implement difference generation.

With a valid `equals()` method, the bulk of the work is handled
in `DDLDiff.build()` The DDL is split into its components, and Maps.difference()
is used to compare.

If you have new top-level types, then they should be added here.

If you have new table capabilities, then they can usually be added inline when
creating the table, or by ALTER statements. These are handled by extracting the
inline statements from the create table, and then assuming everything is an
ALTER, so the comparison is made on the ALTER statements (see the handling of
constraints, foreign keys and row deletion policies).

Then you can add code to generate the DDL statements for the differences -
adding new items, dropping old ones, and replacing existing ones. This is done
in `DdlDiff.generateDifferenceStatements()`. The order of things is very
important in this function as some statements are dependent on each other. in
general the order is:

* Drop DDL objects that have been removed in order of dependency
  * constraints
  * foreign keys
  * tables - in reverse order in which they were created
* Create new DDL objects in order of dependency
  * tables in order of which they were created
  * foreign keys
  * constraints
* Update existing objects, by dropping and recreating them.

Care must be taken when updating that a long running operation is not
triggered - eg for indexes, foreign keys. If this is possible then add a command
line option to prevent this step.

## Add diff generation tests

The easiest way to add tests is using the files in the `test/resources`
subdirectory. These have 3 files containing blocks with original, new and
expected diff statements. Adding new test cases should be straightforward, and
add as many test cases that you can think of.

Normally you will want tests for:

* Adding a DDL feature/object in the new DDL
* Removing a DDL feature/object in the new DDL
* Changing a DDL feature/object.
* Verifying that not changing the feature/object has no effect!

For a DDL object like a constraint that can be added inline in a Create Table or
by an Alter statement, you will need to add multiple versions of the add/remove
tests to handle each case.
