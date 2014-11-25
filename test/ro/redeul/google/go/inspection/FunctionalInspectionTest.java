package ro.redeul.google.go.inspection;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import org.junit.Ignore;
import ro.redeul.google.go.lang.psi.GoFile;

// 1.make sure correct code for golang will not have any compile errors or NPE from all inspections.
// 2.put test cases for not exist Inspection here.
public class FunctionalInspectionTest extends GoInspectionTestCase {
    //utf8 package without test from go sdk 1.3.3
    @Ignore("failing test")
    public void testUtf8() throws Exception{ doTest(); }

    @Override
    protected void detectProblems(GoFile file, InspectionResult result)
            throws IllegalAccessException, InstantiationException {
        InspectionToolRegistrar.getInstance().ensureInitialized();
        for(InspectionToolWrapper wrapper: InspectionToolRegistrar.getInstance().createTools()){
            InspectionProfileEntry tool = wrapper.getTool();
            if (tool instanceof AbstractWholeGoFileInspection){
                ((AbstractWholeGoFileInspection) tool).doCheckFile(file, result);
            }
        }

        result.removeNonCompileError();
    }
}
