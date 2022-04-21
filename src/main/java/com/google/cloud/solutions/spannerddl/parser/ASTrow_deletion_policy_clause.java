
package com.google.cloud.solutions.spannerddl.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Joiner;
import java.util.Arrays;


// TODO Support row deletion policy clause in ALTER statements
/** Abstract Syntax Tree parser object for "row_deletion_policy_clause" token */
public class ASTrow_deletion_policy_clause extends SimpleNode {
    private static final Logger LOG = LoggerFactory.getLogger(ASTrow_deletion_policy_clause.class);

    public ASTrow_deletion_policy_clause(int id) {
        super(id);
    }

    public ASTrow_deletion_policy_clause(DdlParser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        // eg ROW DELETION POLICY (OLDER_THAN(ExpiredDate, INTERVAL 0 DAY));
        return String.format("ROW DELETION POLICY (%s)", ((ASTrow_deletion_policy_expression) children[0]));
    }
}
