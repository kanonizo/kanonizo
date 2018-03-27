package org.kanonizo.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kanonizo.display.fx.KanonizoFrame;

public class KanonizoFxApplication extends Application {
  public static Stage stage;
  @Override
  public void start(Stage stage) throws Exception {
    KanonizoFxApplication.stage = stage;
    stage.setTitle("Kanonizo Test Case Prioritisation");
    StackPane layout = FXMLLoader.load(KanonizoFrame.class.getResource("MainScreen.fxml"));
    stage.setScene(new Scene(layout));
    stage.show();
  }
}
