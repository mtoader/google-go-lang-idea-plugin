package ro.redeul.google.go.formatter;

import ro.redeul.google.go.GoFormatterTestCase;

/**
 * Top level file formatter test cases.
 * <br/>
 * <p/>
 * Created on Dec-29-2013 22:27
 *
 * @author <a href="mailto:mtoader@gmail.com">Mihai Toader</a>
 */
public class GoStatementsFormatterTest extends GoFormatterTestCase {

    @Override
    protected String getRelativeTestDataPath() {
        return super.getRelativeTestDataPath() + "statements/";
    }

    public void testBlockEmpty() throws Exception { _test(); }
    public void testBlockWithComments() throws Exception { _test(); }

    public void testShortVar() throws Exception { _test(); }

    public void testShortVarAlignComments() throws Exception { _test(); }

    public void testShortVarCommentGroups() throws Exception { _test(); }

    public void testAssignment() throws Exception { _test(); }

    public void testIncDec() throws Exception { _test(); }

    public void testSend() throws Exception { _test(); }

    public void testExpression() throws Exception { _test(); }

    public void testBreakFallthroughContinueAndGoto() throws Exception { _test(); }

    public void testReturn() throws Exception { _test(); }

    public void testLabeledStatement() throws Exception { _test(); }

    public void testLabeledStatementWithComments() throws Exception { _test(); }

    public void testGo() throws Exception { _test(); }

    public void testDefer() throws Exception { _test(); }

    public void testSelect_empty() throws Exception { _test(); }

    public void testSelect_simple() throws Exception { _test(); }

    public void testConstVarAndTypeDeclarations() throws Exception { _test(); }

    public void testIf_simple() throws Exception { _test(); }
    public void testIf_else() throws Exception { _test(); }
    public void testIf_withSimpleStmtAndComment() throws Exception { _test(); }

    public void testFor_normal() throws Exception { _test(); }
    public void testFor_withClauses() throws Exception { _test(); }
    public void testFor_withRanges() throws Exception { _test(); }

    public void testSwitch_expr() throws Exception { _test(); }
    public void testSwitch_type() throws Exception { _test(); }
    public void testSwitch_typeWithWhitespace() throws Exception { _test(); }
    public void testSwitch_multipleCaseValues() throws Exception { _test(); }
}
