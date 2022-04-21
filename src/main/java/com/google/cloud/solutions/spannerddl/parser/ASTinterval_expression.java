
package com.google.cloud.solutions.spannerddl.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Joiner;
import java.util.Arrays;


/** Abstract Syntax Tree parser object for "interval_expression" token */
public class ASTinterval_expression extends SimpleNode {
    private static final Logger LOG = LoggerFactory.getLogger(ASTinterval_expression.class);

    public ASTinterval_expression(int id) {
        super(id);
    }

    public ASTinterval_expression(DdlParser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        // eg INTERVAL num_days DAY
        // Apparently only supports DAY
        return "INTERVAL " + children[0] + " DAY";
    }
}
