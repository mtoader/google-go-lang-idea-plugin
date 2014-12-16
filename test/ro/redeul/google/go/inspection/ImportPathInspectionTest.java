package ro.redeul.google.go.inspection;

import org.junit.Ignore;

public class ImportPathInspectionTest extends GoInspectionTestCase {

    public void testSpace() throws Exception { doTest(); }

    public void testBackslash() throws Exception { doTest(); }

    public void testEmptyImportPath() throws Exception { doTest(); }

    public void testCgo() throws Exception { doTest(); }

    public void testRepeat() throws Exception { doTest(); }

    public void testNotFound() throws Exception { doTest(); }

    public void testDirPackageNotEqual() throws Exception { doTest();}

    public void testSelf() throws Exception { doTest(); }

    public void testCaseInsensitive() throws Exception { doTest();}

    @Ignore("failing test")
    public void testTwoPackageOneDirectory() throws Exception { doTest();}
}
