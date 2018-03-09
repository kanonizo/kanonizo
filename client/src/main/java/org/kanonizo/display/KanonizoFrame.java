package org.kanonizo.display;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.gui.GuiUtils;
import org.kanonizo.util.Util;

public class KanonizoFrame extends Application implements Display {

  private ProgressBar pb;
  private Framework fw;
  private double desiredScreenWidth;
  private double desiredScreenHeight;
  private double desiredX;
  private double desiredY;
  private ComboBox algorithmChoice;
  private TreeView<File> sourceTree;
  private TreeView<File> testTree;

  private static final int ITEMS_PER_ROW = 2;

  public void init() {
    this.fw = Framework.getInstance();
    pb = new ProgressBar();
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
    // top
    VBox top = new VBox();
    GuiUtils.setStandardSpacing(top);
    MenuBar menuBar = getMenuBar();
    HBox rootsLayout = getProjectRootGUI(window);
    top.getChildren().addAll(menuBar, rootsLayout);
    // center
    GridPane centre = new GridPane();
    GuiUtils.setStandardSpacing(centre);
    Label sourceLabel = new Label("Source Folder:");
    centre.add(sourceLabel,0,0);
    sourceTree = new TreeView<>(createNode(fw.getRootFolder()));
    sourceTree.getSelectionModel().selectedItemProperty().addListener((ov, old, nv) -> {
      if(old != nv && nv != null){
        fw.setSourceFolder(nv.getValue());
      }
    });
    GuiUtils.setDefaultTreeNamer(sourceTree);
    centre.add(sourceTree, 0,1);

    Label testLabel = new Label("Test Folder:");
    centre.add(testLabel, 1,0);
    testTree = new TreeView<>(createNode(fw.getRootFolder()));
    testTree.getSelectionModel().selectedItemProperty().addListener((ov, old, nv) -> {
      if(old != nv && nv != null){
        fw.setTestFolder(nv.getValue());
      }
    });
    GuiUtils.setDefaultTreeNamer(testTree);
    centre.add(testTree, 1,1);
    Button addLibs = new Button("Add Libraries");
    addLibs.setOnAction(ev -> {
      FileChooser fc = new FileChooser();
      fc.setInitialDirectory(fw.getRootFolder());
      fc.setSelectedExtensionFilter(new ExtensionFilter("Jar files only", ".jar"));
      List<File> jarFiles = fc.showOpenMultipleDialog(window);
      if(jarFiles != null){
        for(File f : jarFiles){
          Util.addToClassPath(f);
        }
      }
    });
    centre.add(addLibs, 2,0);
    mainLayout.setCenter(centre);
    // bottom
    GridPane bottom = new GridPane();
    Label algorithmLabel = new Label("Algorithm:");
    bottom.add(algorithmLabel, 0, 0);
    GuiUtils.setStandardSpacing(bottom);
    algorithmChoice = new ComboBox();
    algorithmChoice.getItems().addAll(Framework.getAvailableAlgorithms());
    algorithmChoice.setConverter(new StringConverter() {
      @Override
      public String toString(Object object) {
        return object.getClass().getAnnotation(Algorithm.class).readableName();
      }

      @Override
      public Object fromString(String string) {
        try {
          return Framework.getAvailableAlgorithms().stream().filter(
              alg -> alg.getClass().getAnnotation(Algorithm.class).readableName().equals(string))
              .findFirst().get();
        } catch (Exception e) {
          return null;
        }
      }
    });
    GridPane paramLayout = new GridPane();
    GuiUtils.setStandardSpacing(paramLayout);
    final List<Label> activeErrors = new ArrayList<>();
    algorithmChoice.valueProperty().addListener((ov, t, t1) -> {
      SearchAlgorithm alg = (SearchAlgorithm) ov.getValue();
      fw.setAlgorithm(alg);
      //remove existing children
      paramLayout.getChildren().clear();
      bottom.getChildren().removeAll(activeErrors);
      activeErrors.clear();
      addParams(alg, paramLayout);
      List<String> errors = Framework.runPrerequisites(alg);
      int row = 1;
      for (String error : errors) {
        Label er = new Label(error);
        activeErrors.add(er);
        er.getStyleClass().add("error");
        bottom.add(er, 0, row++, 2, 1);
      }
    });
    bottom.add(algorithmChoice, 1, 0);
    bottom.add(paramLayout, 2, 0);
    Button go = new Button("Go");
    go.getStyleClass().add("go");
    go.setOnAction(ev -> {
      try {
        fw.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    bottom.add(go, 3, 0, 1, 2);
    mainLayout.setBottom(bottom);
    mainLayout.setTop(top);

    window.setScene(new Scene(mainLayout, desiredScreenWidth, desiredScreenHeight));
    window.setX(desiredX);
    window.setY(desiredY);
    window.show();
  }

  private void addParams(SearchAlgorithm alg, GridPane paramLayout) {
    List<Field> params = Arrays.asList(alg.getClass().getFields()).stream()
        .filter(f -> f.getAnnotation(Parameter.class) != null).collect(
            Collectors.toList());
    int row = 0;
    int col = -1;
    for (Field param : params) {
      if (col + 2 > ITEMS_PER_ROW * 2) {
        col = 0;
        row++;
      }
      Label paramLabel = new Label(param.getName() + ":");
      Control paramField = getParameterField(param);
      if (paramField instanceof TextField) {
        ((TextField) paramField).textProperty().addListener((obs, old, nw) -> {
          try {
            Util.setParameter(param, nw);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        });
      } else if (paramField instanceof CheckBox) {
        ((CheckBox) paramField).selectedProperty().addListener((obs, old, nw) -> {
          try {
            Util.setParameter(param, nw.toString());
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        });
      }
      paramLayout.add(paramLabel, ++col, row);
      paramLayout.add(paramField, ++col, row);
    }
  }

  private Control getParameterField(Field param) {
    Control parameterField = null;
    Class<?> type = param.getType();
    if (type.equals(boolean.class) || type.equals(Boolean.class)) {
      parameterField = new CheckBox();
    } else {
      parameterField = new TextField();
    }
    return parameterField;
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
    File defaultDir = fw.getRootFolder();
    final TextField rootLocation = new TextField(defaultDir.getAbsolutePath());
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
        sourceTree.setRoot(createNode(newRoot));
        testTree.setRoot(createNode(newRoot));
      }
    });
    roots.getChildren().add(openRoot);
    return roots;
  }

  private TreeItem<File> createNode(final File f) {
    return new TreeItem<File>(f) {
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
              children.add(createNode(childFile));
            }

            return children;
          }
        }

        return FXCollections.emptyObservableList();
      }
    };
  }

  @Override
  public void initialise() {
    Application.launch(KanonizoFrame.class, null);
  }

  @Override
  public void fireTestCaseSelected(TestCase tc) {

  }

  @Override
  public void fireTestSuiteChange(TestSuite ts) {

  }

  @Override
  public void reportProgress(double current, double max) {
    pb.setProgress(current / max);
  }

  @Override
  public int ask(String question) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Confirm an Action");
    alert.setContentText(question);
    ButtonType yes = new ButtonType("Yes");
    ButtonType no = new ButtonType("No");
    alert.getButtonTypes().setAll(yes,no);
    Optional<ButtonType> result = alert.showAndWait();
    if(result.get() == yes){
      return 0;
    } else if (result.get() == no){
      return 1;
    }
    return -1;
  }
}
