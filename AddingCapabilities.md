# Adding capabilities

## Sync parser code with cloud spanner emulator

1) Clone the Cloud Spanner Emulator repo from

   `https://github.com/GoogleCloudPlatform/cloud-spanner-emulator`

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

5) Optionally, but recommended add test cases to verify that these unsupported
   operations fail during parsing.

## Run a test and fix bugs

The changes to the parser may have introduced new AST classes that the existing
parser code may not be expecting. Run a `mvn clean verify` in order to re-run
the tests. 

