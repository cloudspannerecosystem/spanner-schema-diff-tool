This directory contains the jjt grammar for parsing Spanner DDL. 

The file `DdlParser.head` is the header for the combined jjt file containing
the Java parser code. 

The remaining `ddl_*.jjt` files come from the cloud-spanner-emulator:\
https://github.com/GoogleCloudPlatform/cloud-spanner-emulator/tree/master/backend/schema/parser \
except for `ddl_keywords.jjt` which is generated during emulator compile time
from ZetaSQL.

Updating DDL grammar requires copying the updated `.jjt` files from the
emulator, regenerating the `ddl_keywords.jjt` using the emulator and copying
that as well, and then updating the AST*.java files to handle parsing the 
updated grammar. 
  
