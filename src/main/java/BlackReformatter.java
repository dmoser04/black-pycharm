import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BlackReformatter {

  private BlackPycharmConfig config;

  public BlackReformatter(final Project project) {
    this.config = BlackPycharmConfig.getInstance(project);
  }

  private byte[] toByteArray(InputStream inputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int read;
    byte[] bytes = new byte[1024];

    while ((read = inputStream.read(bytes)) != -1) {
      byteArrayOutputStream.write(bytes, 0, read);
    }

    return byteArrayOutputStream.toByteArray();
  }

  // --Commented out by Inspection START (20/01/2020 16:17):
  //  private byte[] getProcessStdout(Process p) throws IOException {
  //    return toByteArray(p.getInputStream());
  //  }
  // --Commented out by Inspection STOP (20/01/2020 16:17)

  private byte[] getProcessStderr(Process p) throws IOException {
    return toByteArray(p.getErrorStream());
  }

  private String[] getCommand(String path) {
    return new String[] {
      config.getExecutableName(), path, "--line-length", config.getMaxLineLength()
    };
  }

  private void reformatFile(String path) throws InterruptedException, IOException {

    // invoke Black
    Process blackProcess = Runtime.getRuntime().exec(getCommand(path));

    blackProcess.waitFor();

    if (blackProcess.exitValue() != 0) {
      // ToDo Address default encoding issue identified by FindBugs-IDEA
      //  Use an alternative API and specify a charset name or Charset object explicitly.
      String errorMsg = new String(getProcessStderr(blackProcess));
      throw new RuntimeException(errorMsg);
    }
  }

  protected void doReformat(VirtualFile virtualFile) throws IOException, InterruptedException {
    if (virtualFile == null || virtualFile.isDirectory()) {
      return;
    }

    String path = virtualFile.getPath();

    if (!path.endsWith(".py")) {
      return;
    }

    if (!virtualFile.isWritable()) {
      return;
    }

    // save changes so that IDE doesn't display message box
    FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
    Document document = fileDocumentManager.getDocument(virtualFile);
    assert document != null;
    fileDocumentManager.saveDocument(document);

    // reformat it using Black
    this.reformatFile(virtualFile.getPath());

    // unlock the file & refresh
    Application app = ApplicationManager.getApplication();
    app.runWriteAction(() -> virtualFile.refresh(false, false));
  }
}
