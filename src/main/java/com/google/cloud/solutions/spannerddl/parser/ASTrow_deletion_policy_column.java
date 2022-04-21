
package com.google.cloud.solutions.spannerddl.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Joiner;
import java.util.Arrays;


/** Abstract Syntax Tree parser object for "row_deletion_policy_column" token */
public class ASTrow_deletion_policy_column extends SimpleNode {
    private static final Logger LOG = LoggerFactory.getLogger(ASTrow_deletion_policy_column.class);

    public ASTrow_deletion_policy_column(int id) {
        super(id);
    }

    public ASTrow_deletion_policy_column(DdlParser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        return jjtGetFirstToken().toString();
    }
}
