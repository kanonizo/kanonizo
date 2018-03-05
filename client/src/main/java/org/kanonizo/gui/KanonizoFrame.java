package org.kanonizo.gui;

import java.io.File;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kanonizo.Framework;

public class KanonizoFrame extends Application {

  private Framework fw;
  private double desiredScreenWidth;
  private double desiredScreenHeight;
  private double desiredX;
  private double desiredY;

  public void init() {
    this.fw = Framework.getInstance();
    Screen main = Screen.getPrimary();
    Rectangle2D bounds = main.getVisualBounds();
    desiredScreenWidth = bounds.getWidth() / 2;
    desiredScreenHeight = bounds.getHeight() / 2;
    desiredX = desiredScreenWidth - (desiredScreenWidth / 2);
    desiredY = desiredScreenHeight - (desiredScreenHeight / 2);
  }

  @Override
  public void start(Stage window) throws Exception {
    window.setTitle("Kanonizo Test Prioritisation");
    BorderPane mainLayout = new BorderPane();
    VBox top = new VBox();
    top.setSpacing(5);
    top.setPadding(new Insets(5, 10, 5, 10));

    MenuBar menuBar = getMenuBar();
    HBox rootsLayout = getProjectRootGUI(window);
    top.getChildren().addAll(menuBar, rootsLayout);

    mainLayout.setTop(top);

    window.setScene(new Scene(mainLayout, desiredScreenWidth, desiredScreenHeight));
    window.setX(desiredX);
    window.setY(desiredY);
    window.show();
  }

  private MenuBar getMenuBar() {
    MenuBar menuBar = new MenuBar();
    Menu fileMenu = new Menu("File");
    MenuItem newConfig = new MenuItem("New Configuration...");
    MenuItem openConfig = new MenuItem("Open Configuration...");
    MenuItem saveConfig = new MenuItem("Save Configuration...");
    MenuItem exit = new MenuItem("Exit");
    exit.setOnAction(e -> System.exit(0));
    fileMenu.getItems().addAll(newConfig, openConfig, saveConfig, exit);
    menuBar.getMenus().addAll(fileMenu);
    return menuBar;
  }

  private HBox getProjectRootGUI(Stage window) {
    HBox roots = new HBox();
    roots.setSpacing(10);
    Label rootLabel = new Label("Project Root:");
    roots.getChildren().add(rootLabel);
    final TextField rootLocation = new TextField(System.getProperty("user.home"));
    rootLocation.setEditable(false);
    rootLocation.setMinSize(desiredScreenWidth - 300, 20);
    roots.getChildren().add(rootLocation);
    Button openRoot = new Button("Open Project Root");
    openRoot.setOnAction(e -> {
      DirectoryChooser dc = new DirectoryChooser();
      File newRoot = dc.showDialog(window);
      if (newRoot != null) {
        fw.setRootFolder(newRoot);
        rootLocation.setText(newRoot.getAbsolutePath());
      }
    });
    roots.getChildren().add(openRoot);
    return roots;
  }
}
