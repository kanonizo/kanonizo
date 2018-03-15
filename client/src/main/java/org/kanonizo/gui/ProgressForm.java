package org.kanonizo.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressForm {

  private final Stage dialogStage;
  private final ProgressBar pb = new ProgressBar();

  public ProgressForm(String name) {
    dialogStage = new Stage();
    dialogStage.initStyle(StageStyle.UTILITY);
    dialogStage.setResizable(false);
    dialogStage.initModality(Modality.APPLICATION_MODAL);
    dialogStage.setTitle(name);
    dialogStage.setWidth(200);
    dialogStage.setWidth(50);

    // PROGRESS BAR
    final Label label = new Label();
    label.setText(name);

    pb.setProgress(-1F);

    final GridPane hb = new GridPane();
    hb.setPadding(new Insets(10,5,10,5));
    hb.setVgap(5);
    hb.setHgap(5);
    hb.setAlignment(Pos.CENTER);
    hb.add(label, 0, 0);
    hb.add(pb, 0, 1);

    Scene scene = new Scene(hb);
    dialogStage.setScene(scene);
  }

  public void show(){
    Platform.runLater(() -> dialogStage.show());
  }

  public void updateProgress(double current, double max){
    Platform.runLater(() -> pb.setProgress(current / max));
  }

  public Stage getDialogStage() {
    return dialogStage;
  }
}

