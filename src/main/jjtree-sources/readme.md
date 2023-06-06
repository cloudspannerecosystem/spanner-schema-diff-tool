This directory contains the jjt grammar for parsing Spanner DDL.

The file `DdlParser.head` is the header for the combined jjt file containing the
Java parser code.

The remaining `ddl_*.jjt` files come from the cloud-spanner-emulator: \
https://github.com/GoogleCloudPlatform/cloud-spanner-emulator/tree/master/backend/schema/parser
\
except for `ddl_keywords.jjt` which is generated during emulator compile time
from ZetaSQL.

Updating DDL grammar requires copying the updated `.jjt` files from the
emulator, regenerating the `ddl_keywords.jjt` using the emulator and copying
that as well, and then updating the AST\*.java files to handle parsing the
updated grammar.

Note that the file `ddl_string_bytes_tokens.jjt` should not have the lines
that call `ValidateBytesLiteral()` and `ValidateStringBytesLiteral()`

```shell
# Build ddl_keywords.jjt
sudo apt install bazel-5.4.0
git clone https://github.com/GoogleCloudPlatform/cloud-spanner-emulator.git
cd cloud-spanner-emulator
bazel build backend/schema/parser:ddl_keywords_jjt

# generated file in bazel-bin/backend/schema/parser/ddl_keywords.jjt


# Compare JJT files

diff bazel-bin/backend/schema/parser/ddl_keywords.jjt ../spanner-schema-diff-tool/src/main/jjtree-sources/ddl_keywords.jjt
diff backend/schema/parser/ddl_expression.jjt ../spanner-schema-diff-tool/src/main/jjtree-sources/ddl_expression.jjt
diff backend/schema/parser/ddl_parser.jjt ../spanner-schema-diff-tool/src/main/jjtree-sources/ddl_parser.jjt
diff backend/schema/parser/ddl_string_bytes_tokens.jjt ../spanner-schema-diff-tool/src/main/jjtree-sources/ddl_string_bytes_tokens.jjt
diff backend/schema/parser/ddl_whitespace.jjt ../spanner-schema-diff-tool/src/main/jjtree-sources/ddl_whitespace.jjt
```

