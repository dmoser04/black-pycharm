import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;

public class BlackPycharmReformatCodeAction extends AnAction {

    public BlackPycharmReformatCodeAction() {
        super();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        // extract current open file, it could be file or folder or null it doesn't get focus
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        try {
            new BlackReformatter(project).doReformat(virtualFile);
        } catch (IOException | InterruptedException | RuntimeException e) {
            BlackPycharmErrorUtil.sendErrorNotification(e.getMessage());
        }

    }
}
