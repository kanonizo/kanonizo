package org.kanonizo.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class AlertUtils {
  public static void alert(AlertType type, String title, String message){
    Alert al = new Alert(type);
    al.setTitle(title);
    al.setContentText(message);
    al.show();
  }
}
