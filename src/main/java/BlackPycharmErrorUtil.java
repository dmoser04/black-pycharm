import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

public class BlackPycharmErrorUtil {
  public static void displayErrorMessage(Project project, String message) {
    StatusBar statusBar = WindowManager.getInstance()
        .getStatusBar(project);

    JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder("BlackPycharm: " + message,
            MessageType.ERROR, null)
        .setFadeoutTime(7500)
        .createBalloon()
        .show(RelativePoint.getSouthEastOf(statusBar.getComponent()),
            Balloon.Position.atRight);
  }

  public static void sendErrorNotification(final String message){
    Notification notification = new Notification("Black", "Black Format Error", message, NotificationType.ERROR);
    Notifications.Bus.notify(notification);
  }
}
