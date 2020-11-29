package org.kanonizo.display.fx;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.Algorithm;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.annotations.ConditionalParameter;
import org.kanonizo.annotations.OptionProvider;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.display.fx.converters.ReadableConverter;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.gui.AlertUtils;
import org.kanonizo.gui.GuiUtils;
import org.kanonizo.gui.KanonizoFxApplication;
import org.kanonizo.instrumenters.InstrumenterType;
import org.kanonizo.util.Util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class KanonizoFrame implements Display, Initializable {

  @FXML
  private MenuItem exitButton;
  @FXML
  private ComboBox algorithmChoices;
  @FXML
  private TreeView<File> sourceTree;
  @FXML
  private TreeView<File> testTree;
  @FXML
  private ListView<File> libs;
  @FXML
  private Button selectRoot;
  @FXML
  private TextField rootFolderTextField;
  @FXML
  private GridPane paramLayout;
  @FXML
  private GridPane bottom;
  @FXML
  private ComboBox instrumenterChoices;
  @FXML
  private GridPane instParamLayout;
  @FXML
  private Button goButton;
  @FXML
  private StackPane mainLayout;
  @FXML
  private BorderPane borderPane;

  private KanonizoScene scene;
  private Framework fw;

  private static Logger logger = LogManager.getLogger(KanonizoFrame.class);
  private static final int ITEMS_PER_ROW = 2;

  public KanonizoFrame(KanonizoConfigurationModel configModel)
  {

  }

  @FXML
  public void exit() {
    System.exit(0);
  }


  public void selectRoot() {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setInitialDirectory(fw.getRootFolder());
    File newRoot = dc.showDialog(KanonizoFxApplication.stage);
    if (newRoot != null) {
      fw.setRootFolder(newRoot);
    }
  }

  private void addSourceListeners() {
    sourceTree.setRoot(GuiUtils.createDynamicFileTree(fw.getRootFolder()));
    sourceTree.getSelectionModel().selectedItemProperty().addListener((ov, old, nv) -> {
      if (old != nv && nv != null) {
        fw.setSourceFolder(nv.getValue());
      }
    });
    GuiUtils.setDefaultTreeNamer(sourceTree);
  }

  private void addTestListeners() {
    testTree.setRoot(GuiUtils.createDynamicFileTree(fw.getRootFolder()));
    testTree.getSelectionModel().selectedItemProperty().addListener((ov, old, nv) -> {
      if (old != nv && nv != null) {
        fw.setTestFolder(nv.getValue());
      }
    });
    GuiUtils.setDefaultTreeNamer(testTree);
  }

  private void addLibListeners() {
    ContextMenu menu = new ContextMenu();
    MenuItem addLib = new MenuItem();
    addLib.textProperty().set("Add Library");
    addLib.setOnAction((ActionEvent ev) -> {
      FileChooser fc = new FileChooser();
      fc.setInitialDirectory(fw.getRootFolder());
      fc.setSelectedExtensionFilter(new ExtensionFilter("Only jar files!", "jar"));
      List<File> jar = fc.showOpenMultipleDialog(KanonizoFxApplication.stage);
      if (jar != null) {
        for (File j : jar) {
          fw.addLibrary(j);
        }
      }
    });

    menu.getItems().add(addLib);
    libs.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> {
      if (ev.getButton().equals(MouseButton.SECONDARY)) {
        menu.show(libs, ev.getScreenX(), ev.getScreenY());
      } else if (menu.isShowing()) {
        menu.hide();
      }
    });
    GuiUtils.setDefaultListName(libs);
  }


  private void addParams(Object alg, GridPane paramLayout, boolean runPrerequisites) {
    List<Field> params = Arrays.asList(alg.getClass().getFields()).stream()
        .filter(f -> f.getAnnotation(Parameter.class) != null).collect(
            Collectors.toList());
    int row = 0;
    int col = -1;
    for (Field param : params) {
      if (col + 2 > ITEMS_PER_ROW * 2) {
        col = -1;
        row++;
      }
      Label paramLabel = new Label(Util.humanise(param.getName()) + ":");
      paramLabel.setAlignment(Pos.CENTER_LEFT);
      paramLabel.setTooltip(new Tooltip(Util.humanise(param.getName())));
      Control paramField = getParameterField(param, runPrerequisites);
      paramField.setTooltip(new Tooltip(param.getAnnotation(Parameter.class).description()));
      paramLayout.add(paramLabel, ++col, row, 1, 1);
      paramLayout.add(paramField, ++col, row, 1, 1);
      if (param.isAnnotationPresent(ConditionalParameter.class)) {
        String condition = param.getAnnotation(ConditionalParameter.class).condition();
        String[] listensTo = param.getAnnotation(ConditionalParameter.class).listensTo().split(",");
        for (String listen : listensTo) {
          Util.addPropertyChangeListener(listen, (e) -> {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("nashorn");
            try {
              Class<?> container = param.getDeclaringClass();
              engine.put("CallerClass", container);
              engine.eval("var " + container.getSimpleName() + " = CallerClass.static");
              boolean cond = (boolean) engine.eval(condition);
              paramField.setDisable(!cond);
              paramLabel.setDisable(!cond);
            } catch (ScriptException ex) {
              logger.error(ex);
            }
          });
        }
      }
    }
  }

  private Control getParameterField(Field param, boolean runPrerequisites) {
    Control parameterField = null;
    Class<?> type = param.getType();
    if (type.equals(boolean.class) || type.equals(Boolean.class)) {
      parameterField = new CheckBox();
      ((CheckBox) parameterField).selectedProperty().addListener((obs, old, nw) -> {
        try {
          Util.setParameter(param, nw.toString());
          if (runPrerequisites) {
            addErrors(fw.getAlgorithm());
          }
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      });
      try {
        ((CheckBox) parameterField).setSelected((Boolean) param.get(null));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    } else if (param.getType().equals(String.class) || param.getType().isPrimitive() || param
        .getType().isAssignableFrom(Number.class)) {
      parameterField = new TextField();
      ((TextField) parameterField).textProperty().addListener((obs, old, nw) -> {
        try {
          Util.setParameter(param, nw);
          if (runPrerequisites) {
            addErrors(fw.getAlgorithm());
          }
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      });
      try {
        ((TextField) parameterField).setText(param.get(null).toString());
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    } else if (param.getType().equals(File.class)) {

      try {
        Button control = new Button();
        File paramFile = (File) param.get(null);
        control.setText(paramFile == null ? "Select File" : paramFile.getName());
        control.setOnAction(ev -> {
          FileChooser fc = new FileChooser();
          File f = fc.showOpenDialog(KanonizoFxApplication.stage);
          try {
            Util.setParameter(param, f == null ? null : f.getAbsolutePath());
            if (runPrerequisites) {
              addErrors(fw.getAlgorithm());
            }
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
          control.setText(f == null ? "Select File" : f.getName());

        });
        parameterField = control;
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    } else if (param.getAnnotation(Parameter.class).hasOptions()) {
      String paramKey = param.getAnnotation(Parameter.class).key();
      Method[] methods = param.getDeclaringClass().getMethods();
      Optional<Method> optionProviderOpt = Arrays.asList(methods).stream()
          .filter(m -> m.getAnnotation(OptionProvider.class) != null && m.getAnnotation(
              OptionProvider.class).paramKey().equals(paramKey)).findFirst();
      if (!optionProviderOpt.isPresent()) {
        logger.error("Missing OptionProvider for key" + paramKey);
        return null;
      }
      Method optionProvider = optionProviderOpt.get();
      if (optionProvider.getReturnType() != List.class) {
        logger.error("OptionProvider must return a list");
        return null;
      }
      if (!Modifier.isStatic(optionProvider.getModifiers())) {
        logger.error("OptionProvider must be static");
        return null;
      }
      try {
        List<?> options = (List<?>) optionProvider.invoke(null, null);
        parameterField = new ComboBox();
        ((ComboBox) parameterField).getItems().addAll(options);
        ((ComboBox) parameterField).getSelectionModel().selectedItemProperty()
            .addListener((ov, old, nw) -> {
              try {
                param.set(null, nw);
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
            });
        ((ComboBox) parameterField).setConverter(new StringConverter() {

          @Override
          public String toString(Object object) {
            return object.getClass().getSimpleName();
          }

          @Override
          public Object fromString(String string) {
            String comparatorPackage = "org.kanonizo.algorithms.heuristics.comparators";
            try {
              return Class.forName(comparatorPackage + "." + string).newInstance();
            } catch (InstantiationException e) {
              e.printStackTrace();
            } catch (IllegalAccessException e) {
              e.printStackTrace();
            } catch (ClassNotFoundException e) {
              e.printStackTrace();
            }
            return null;
          }
        });
        try {
          ((ComboBox) parameterField).getSelectionModel().select(param.get(null));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    return parameterField;
  }


  @Override
  public void initialise() {
    Application.launch(KanonizoFxApplication.class, null);
  }

  @Override
  public void fireTestSuiteChange(TestSuite ts) {
    if (scene != null) {
      scene.setTestCases(ts.getTestCases());
    }
  }

  @Override
  public void reportProgress(double current, double max) {
  }

  @Override
  public Answer ask(String question) {
    return GuiUtils.ask(question);
  }

  @Override
  public void notifyTaskStart(String name, boolean progress) {

  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    assert sourceTree != null : "fx:id sourceTree was not injected";

    this.fw = Framework.getInstance();
    fw.addPropertyChangeListener(Framework.ROOT_FOLDER_PROPERTY_NAME,
        (e) -> {
          File newRoot = (File) e.getNewValue();
          rootFolderTextField.setText(newRoot.getAbsolutePath());
          sourceTree.setRoot(GuiUtils.createDynamicFileTree(newRoot));
          testTree.setRoot(GuiUtils.createDynamicFileTree(newRoot));
        }
    );
    fw.addPropertyChangeListener(Framework.ALGORITHM_PROPERTY_NAME,
        (e) -> {
          SearchAlgorithm alg = (SearchAlgorithm) e.getNewValue();
          //remove existing children
          paramLayout.getChildren().clear();
          addParams(alg, paramLayout, true);
          addErrors(alg);
        }
    );
    fw.addPropertyChangeListener(Framework.INSTRUMENTER_PROPERTY_NAME,
        (e) -> {
          org.kanonizo.framework.instrumentation.Instrumenter inst = (org.kanonizo.framework.instrumentation.Instrumenter) e
              .getNewValue();
          instParamLayout.getChildren().clear();
          addParams(inst, instParamLayout, false);
          addErrors(inst);
        }
    );
    fw.addPropertyChangeListener(Framework.LIBS_PROPERTY_NAME,
        (e) -> {
          libs.getItems().add((File) e.getNewValue());
        }
    );
    fw.setDisplay(this);
    Screen main = Screen.getPrimary();
    Rectangle2D bounds = main.getVisualBounds();
    rootFolderTextField.setText(fw.getRootFolder().getAbsolutePath());
    addSourceListeners();
    addTestListeners();
    addLibListeners();
    selectRoot.setOnAction(ev -> selectRoot());
    try {
      algorithmChoices.getItems().addAll(Algorithm.values());
      algorithmChoices.setConverter(new ReadableConverter());

      algorithmChoices.valueProperty().addListener((ov, t, t1) -> {
        SearchAlgorithm alg = (SearchAlgorithm) ov.getValue();
        fw.setAlgorithm(alg);
      });
      algorithmChoices.getSelectionModel().select(fw.getAlgorithm());
      instrumenterChoices.getItems().addAll(InstrumenterType.values());
      instrumenterChoices.setConverter(new ReadableConverter());
      instrumenterChoices.valueProperty().addListener((ov, t, t1) -> {
        org.kanonizo.framework.instrumentation.Instrumenter inst = (org.kanonizo.framework.instrumentation.Instrumenter) ov
            .getValue();
        fw.setInstrumenter(inst);
      });
      instrumenterChoices.getSelectionModel().select(fw.getInstrumenter());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Set<Label> activeErrors = new HashSet<>();

  private void addErrors(Object alg) {
    if (alg instanceof SearchAlgorithm) {
      bottom.getChildren().removeAll(activeErrors);
      activeErrors.clear();
      ProgressIndicator p = new ProgressIndicator();
      List<Method> prerequisites = Framework.getPrerequisites((SearchAlgorithm) alg);
      Task<Void> task = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
          boolean anyFail = false;
          int row = 2;
          // run all prerequisites, not terminating on first failure
          for (int i = 0; i < prerequisites.size(); i++) {
            Method requirement = prerequisites.get(i);
            try {
              boolean passed = (boolean) requirement.invoke(null, null);
              if (!passed) {
                anyFail = true;
                // find readable error message
                String error = requirement.getAnnotation(Prerequisite.class).failureMessage();
                Label er = new Label(error);
                activeErrors.add(er);
                // css styling to make errors red
                er.getStyleClass().add("error");
                int errorRow = row++;
                Platform.runLater(() -> bottom.add(er, 0, errorRow, 2, 1));
              }
            } catch (InvocationTargetException e) {
              logger.error(e);
            }
          }
          // if any pre-requisite failed, we can't run the algorithm
          if (anyFail && !goButton.isDisabled()) {
            goButton.setDisable(true);
          } else if (!anyFail) {
            goButton.setDisable(false);
          }
          return null;
        }
      };
      // show progress indicator over original layout
      VBox box = new VBox(p);
      box.setAlignment(Pos.CENTER);
      task.setOnSucceeded(e -> {
        mainLayout.getChildren().remove(box);
        borderPane.setDisable(false);
      });
      mainLayout.getChildren().add(box);
      borderPane.setDisable(true);
      new Thread(task).start();
    }
  }

  @FXML
  public void go() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("PrioritisationScreen.fxml"));
      loader.load();
      scene = loader.getController();
      scene.setCaller(this);
      KanonizoFxApplication.stage.setScene(new Scene(loader.getRoot()));
      scene.run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  public void save() {

    try {
      FileChooser fc = new FileChooser();
      File saveLocation = new File(System.getProperty("user.home") + File.separator + ".kanonizo");
      if (!saveLocation.exists()) {
        saveLocation.mkdir();
      }
      fc.setInitialDirectory(saveLocation);
      File toWrite = fc.showSaveDialog(KanonizoFxApplication.stage);
      new Thread(() -> {
        try {
          fw.write(toWrite);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }).start();

    } catch (Exception e) {
      AlertUtils.alert(AlertType.ERROR, e.getClass().getName(), e.getMessage());
    }
  }

  @FXML
  public void load() {
    try {
      FileChooser fc = new FileChooser();
      File loadLocation = new File(System.getProperty("user.home") + File.separator + ".kanonizo");
      if (!loadLocation.exists()) {
        loadLocation.mkdir();
      }
      fc.setInitialDirectory(loadLocation);
      File toRead = fc.showOpenDialog(KanonizoFxApplication.stage);
      if (toRead != null) {
        new Thread(() -> {
          try {
            Framework read = fw.read(toRead);
            this.fw.setSourceFolder(read.getSourceFolder());
            this.fw.setRootFolder(read.getRootFolder());
            this.fw.setTestFolder(read.getTestFolder());
            this.fw.setAlgorithm(read.getAlgorithm());
            this.fw.setInstrumenter(read.getInstrumenter());
            read.getLibraries().forEach(lib -> this.fw.addLibrary(lib));
            Platform.runLater(() -> {
              File root = fw.getRootFolder();
              rootFolderTextField.setText(root.getAbsolutePath());
              sourceTree.setRoot(GuiUtils.createDynamicFileTree(root));
              sourceTree.scrollTo(findIndexOfChild(fw.getSourceFolder(), sourceTree));
              testTree.setRoot(GuiUtils.createDynamicFileTree(root));
              testTree.scrollTo(findIndexOfChild(fw.getTestFolder(), testTree));
              libs.getItems().addAll(fw.getLibraries());
              algorithmChoices.getSelectionModel().select(fw.getAlgorithm());
              instrumenterChoices.getSelectionModel().select(fw.getInstrumenter());
            });
          } catch (Exception e) {
            e.printStackTrace();
          }
        }).start();
      }

    } catch (Exception e) {
      AlertUtils.alert(AlertType.ERROR, e.getClass().getName(), e.getMessage());
    }

  }

  private int findIndexOfChild(File folder, TreeView<File> view) {
    ObservableList<Node> children = view.getChildrenUnmodifiable();
    for (Node child : children) {

    }
    return -1;
  }
}
