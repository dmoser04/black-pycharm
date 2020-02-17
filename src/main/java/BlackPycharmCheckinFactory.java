import com.intellij.CommonBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Collection;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlackPycharmCheckinFactory extends CheckinHandlerFactory {
  private static final String BLACK_FORMAT = "BLACK_FORMAT";

  @NotNull
  private static String commitButtonMessage(
      @Nullable CommitExecutor executor, @NotNull CheckinProjectPanel panel) {
    return StringUtil.trimEnd(
        executor != null ? executor.getActionText() : panel.getCommitActionName(), "...");
  }

  private static boolean enabled(@NotNull CheckinProjectPanel panel) {
    return PropertiesComponent.getInstance(panel.getProject()).getBoolean(BLACK_FORMAT, false);
  }

  @Override
  @NotNull
  public CheckinHandler createHandler(
      @NotNull final CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
    return new CheckinHandler() {
      @Override
      public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox checkBox = new JCheckBox("Black format");
        return new RefreshableOnComponent() {
          @Override
          @NotNull
          public JComponent getComponent() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(checkBox, BorderLayout.WEST);
            return panel;
          }

          @Override
          public void refresh() {}

          @Override
          public void saveState() {
            PropertiesComponent.getInstance(panel.getProject())
                .setValue(BLACK_FORMAT, Boolean.toString(checkBox.isSelected()));
          }

          @Override
          public void restoreState() {
            checkBox.setSelected(enabled(panel));
          }
        };
      }

      @Override
      public CheckinHandler.ReturnResult beforeCheckin(
          @Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
        if (enabled(panel)) {
          final Ref<Boolean> success = Ref.create(true);
          FileDocumentManager.getInstance().saveAllDocuments();
          Collection<VirtualFile> files = panel.getVirtualFiles();
          BlackReformatter formatter = new BlackReformatter(panel.getProject());
          int nFailures = 0;
          for (VirtualFile file : files) {
            try {
              formatter.doReformat(file);
            } catch (IOException | InterruptedException | RuntimeException e) {
              BlackPycharmErrorUtil.sendErrorNotification(e.getMessage());
              nFailures++;
              success.set(false);
            }
          }
          if (!success.get()) {
            BlackPycharmErrorUtil.displayErrorMessage(
                panel.getProject(),
                String.format("Failed to format %s files. See Event Log for details", nFailures));
            return showErrorMessage(executor);
          }
        }
        return super.beforeCheckin();
      }

      @NotNull
      private ReturnResult showErrorMessage(@Nullable CommitExecutor executor) {
        String[] buttons =
            new String[] {
              "&Details...",
              commitButtonMessage(executor, panel),
              CommonBundle.getCancelButtonText()
            };
        int answer =
            Messages.showDialog(
                panel.getProject(),
                "<html><body>'black' returned non-zero code on some of the files.<br/>"
                    + "Would you like to commit anyway?</body></html>\n",
                "Black Format",
                null,
                buttons,
                0,
                1,
                UIUtil.getWarningIcon());
        if (answer == Messages.OK) {
          ToolWindowManager.getInstance(panel.getProject()).getToolWindow("Event Log").show(null);
          return ReturnResult.CLOSE_WINDOW;
        }
        if (answer == Messages.NO) {
          return ReturnResult.COMMIT;
        }
        return ReturnResult.CANCEL;
      }
    };
  }
}
