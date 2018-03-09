package org.kanonizo.gui;

import java.io.File;
import javafx.geometry.Insets;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class GuiUtils {
    public static void setStandardSpacing(Pane node){
      node.setPadding(new Insets(5,10,5,10));
      if(node instanceof HBox){
        ((HBox)node).setSpacing(10);
      } else if (node instanceof VBox){
        ((VBox)node).setSpacing(10);
      } else if (node instanceof GridPane){
        GridPane pane = (GridPane) node;
        pane.setHgap(10);
        pane.setVgap(10);
      }
    }

    public static void setDefaultTreeNamer(TreeView<File> tree){
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
}
