package ro.redeul.google.go.inspection;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.Processor;
import org.junit.Assert;
import ro.redeul.google.go.lang.psi.GoFile;
import ro.redeul.google.go.sdk.GoSdkUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bronze1man on 14-10-24.
 * Test every files and every inspections on go sdk to find more bugs
 * Most used for finding false error inspection.
 * TODO make this test possible in Travis CI
 */
public class GoSDKInspectionTest extends GoInspectionTestCase{
    public void testNothing() throws Exception{
    }
    /* TODO FIX ME
    public void testTime() throws Exception{
        doTestInPackage("time");
    }
    */
    protected void doTestInPackage(String subPath) throws Exception{
        final ArrayList<PsiFile> list = new ArrayList<PsiFile>();
        String FolderPath =  GetGoSdkRootPath()+"/"+subPath;
        final String DataPath = GetGoSdkRootPath();
        myFixture.setTestDataPath(DataPath);
        FileUtil.visitFiles(new File(FolderPath), new Processor<File>() {
            @Override
            public boolean process(File file) {
                String path = file.getPath();
                String ext = FileUtil.getExtension(path);
                if (!ext.equals("go")) {
                    return true;
                }
                PsiFile psi = myFixture.configureByFile(FileUtil.getRelativePath(DataPath, path,'/'));
                list.add(psi);
                return true;
            }
        });
        StringBuilder sb = new StringBuilder();
        for(PsiFile file:list){
            Document document = myFixture.getDocument(file);
            InspectionResult result = new InspectionResult(getProject());
            detectProblems((GoFile)file, result);
            List<ProblemDescriptor> problems = result.getProblems();
            for (ProblemDescriptor pd : problems) {
                TextRange range;
                if (pd instanceof ProblemDescriptorImpl) {
                    range = ((ProblemDescriptorImpl) pd).getTextRange();
                } else {
                    int start = pd.getStartElement().getTextOffset();
                    int end = pd.getEndElement()
                            .getTextOffset() + pd.getEndElement()
                            .getTextLength();
                    range = new TextRange(start, end);
                }
                String text = document.getText(range);

                sb.append(file.getVirtualFile().getName())
                        .append(":").append(pd.getLineNumber()).append(" ")
                        .append(text
                                        .replaceAll("\"?.*(, )?/\\*begin\\*/([^\\*/]*)/\\*end\\.[^\\*/]*\\*/(\\\\n)?\"?", "$2")
                        ).append(" => ").append(pd.getDescriptionTemplate()).append("\n");
            }
        }
        Assert.assertEquals("", sb.toString());
    }
    protected String GetGoSdkRootPath(){
        return "/usr/local/Cellar/go/1.3/libexec/src/pkg";
    }
    protected void detectProblems(GoFile file, InspectionResult result)
            throws IllegalAccessException, InstantiationException {
        new ConstantExpressionsInConstDeclarationsInspection().doCheckFile(file,result);
        new ConstDeclarationInspection().doCheckFile(file,result);
        new FmtUsageInspection().doCheckFile(file,result);
        new FunctionDuplicateArgumentInspection().doCheckFile(file,result);
        new FunctionCallInspection().doCheckFile(file,result);
        new RedeclareInspection().doCheckFile(file, result);
        new ShadowedDuringReturnInspection().doCheckFile(file,result);
    }
}
