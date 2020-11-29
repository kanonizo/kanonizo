package org.kanonizo.gui;

import java.io.File;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.kanonizo.display.Display;

import static org.kanonizo.display.Display.Answer.INVALID;
import static org.kanonizo.display.Display.Answer.NO;
import static org.kanonizo.display.Display.Answer.YES;

public class GuiUtils {

  public static final int GRID_PANE_H_GAP = 10;
  public static final int GRID_PANE_V_GAP = 10;
  public static final int INSETS_TOP = 5;
  public static final int INSETS_BOTTOM = 5;
  public static final int INSETS_LEFT = 10;
  public static final int INSETS_RIGHT = 10;

  public static void setStandardSpacing(Pane node) {
    node.setPadding(new Insets(INSETS_TOP, INSETS_RIGHT, INSETS_BOTTOM, INSETS_LEFT));
    if (node instanceof HBox) {
      ((HBox) node).setSpacing(10);
    } else if (node instanceof VBox) {
      ((VBox) node).setSpacing(10);
    } else if (node instanceof GridPane) {
      GridPane pane = (GridPane) node;
      pane.setHgap(GRID_PANE_H_GAP);
      pane.setVgap(GRID_PANE_V_GAP);
    }
  }

  public static void setDefaultTreeNamer(TreeView<File> tree) {
    tree.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {
      @Override
      public TreeCell<File> call(TreeView<File> param) {
        return new TextFieldTreeCell<>(new StringConverter<File>() {

          @Override
          public String toString(File object) {
            return object.getAbsolutePath()
                .substring(object.getParentFile().getAbsolutePath().length() + 1);
          }

          @Override
          public File fromString(String string) {
            return new File(string);
          }
        });
      }
    });
  }

  public static void setDefaultListName(ListView<File> lv){
    lv.setCellFactory(new Callback<ListView<File>, ListCell<File>>() {
      @Override
      public ListCell<File> call(ListView<File> param) {
        return new TextFieldListCell<>(new StringConverter<File>() {

          @Override
          public String toString(File object) {
            return object.getAbsolutePath()
                .substring(object.getParentFile().getAbsolutePath().length() + 1);
          }

          @Override
          public File fromString(String string) {
            return new File(string);
          }
        });
      }
    });
  }

  public static TreeItem<File> createDynamicFileTree(File parent) {
    return new TreeItem<File>(parent) {
      private boolean isLeaf;
      private boolean isFirstTimeChildren = true;
      private boolean isFirstTimeLeaf = true;

      @Override
      public ObservableList<TreeItem<File>> getChildren() {
        if (isFirstTimeChildren) {
          isFirstTimeChildren = false;
          super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
      }

      @Override
      public boolean isLeaf() {
        if (isFirstTimeLeaf) {
          isFirstTimeLeaf = false;
          File f = getValue();
          isLeaf = f.isFile();
        }

        return isLeaf;
      }

      private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
        File f = TreeItem.getValue();
        if (f != null && f.isDirectory()) {
          File[] files = f.listFiles((fn) -> !fn.getName().startsWith("."));
          if (files != null) {
            ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

            for (File childFile : files) {
              children.add(createDynamicFileTree(childFile));
            }

            return children;
          }
        }

        return FXCollections.emptyObservableList();
      }
    };

  }

  public static Display.Answer ask(String question){
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Confirm an Action");
    alert.setContentText(question);
    ButtonType yes = new ButtonType("Yes");
    ButtonType no = new ButtonType("No");
    alert.getButtonTypes().setAll(yes, no);
    Optional<ButtonType> result = alert.showAndWait();
    if (result.get() == yes) {
      return YES;
    } else if (result.get() == no) {
      return NO;
    }
    return INVALID;
  }
}
