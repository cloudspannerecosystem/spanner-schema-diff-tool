
package com.google.cloud.solutions.spannerddl.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Joiner;
import java.util.Arrays;


// TODO Support row deletion policy clause in ALTER statements
/** Abstract Syntax Tree parser object for "row_deletion_policy_expression" token */
public class ASTrow_deletion_policy_expression extends SimpleNode {
    private static final Logger LOG = LoggerFactory.getLogger(ASTrow_deletion_policy_expression.class);

    public ASTrow_deletion_policy_expression(int id) {
        super(id);
    }

    public ASTrow_deletion_policy_expression(DdlParser p, int id) {
        super(p, id);
    }

    private String getRdpFunction() {
        return ((ASTrow_deletion_policy_function) children[0]).toString();
    }

    private String getRdpColumn() {
        return ((ASTrow_deletion_policy_column) children[1]).toString();
    }

    private String getRdpIntervalExpr() {
        return ((ASTinterval_expression) children[2]).toString();
    }
    @Override
    public String toString() {
        // eg OLDER_THAN(ExpiredDate, INTERVAL 0 DAY);
        return String.format("%s(%s, %s)", getRdpFunction(), getRdpColumn(), getRdpIntervalExpr());
    }
}
