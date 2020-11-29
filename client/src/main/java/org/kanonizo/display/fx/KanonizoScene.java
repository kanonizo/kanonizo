package org.kanonizo.display.fx;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.display.Display;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.gui.GuiUtils;
import org.kanonizo.gui.KanonizoFxApplication;
import org.kanonizo.gui.ProgressForm;
import org.kanonizo.listeners.TestCaseSelectionListener;
import org.kanonizo.util.Util;

public class KanonizoScene implements Display, Initializable, TestCaseSelectionListener {

  private KanonizoFrame caller;
  private ObservableList<TestCase> orderedTests = FXCollections.observableArrayList();
  @FXML
  private TableView testCases;
  @FXML
  private ProgressBar pb;
  @FXML
  private Label taskLabel;
  private Task<Void> runnerTask;
  private KanonizoThread runnerThread;
  private ProgressForm form;

  private static Logger logger = LogManager.getLogger(KanonizoScene.class);

  public void setCaller(KanonizoFrame caller) {
    this.caller = caller;
  }

  public void addTestCase(TestCase tc) {
    orderedTests.add(tc);
    Platform.runLater(() -> {
      testCases.setItems(orderedTests);
      testCases.scrollTo(tc);
    });

  }

  public void setTestCases(List<TestCase> testCases) {
    orderedTests.clear();
    orderedTests.addAll(testCases);
    Platform.runLater(() -> {
      this.testCases.setItems(orderedTests);
      this.testCases.scrollTo(orderedTests.size());
    });

  }


  @Override
  public void initialise() {

  }

  @Override
  public void fireTestSuiteChange(TestSuite ts) {
    setTestCases(ts.getTestCases());
  }

  public void reportProgress(double current, double max) {
    Platform.runLater(() -> {
      if (form != null) {
        form.updateProgress(current, max);
        if (current / max == 1) {
          form.getDialogStage().close();
          form = null;
        }
      } else {
        pb.setProgress(current / max);

      }
    });
  }

  @Override
  public Answer ask(String question) {
    return GuiUtils.ask(question);
  }

  @Override
  public void notifyTaskStart(String name, boolean progress) {
    Platform.runLater(() -> {
      if (progress) {
        form = new ProgressForm(name);
        form.show();
      } else {
        setTaskLabel(name);
      }
    });
  }

  public void run() {
    runnerTask = new Task<Void>() {
      public Void call() {
        try {
          Framework.getInstance().run();
        } catch (Exception e) {
          logger.error(e);
          e.printStackTrace(Util.getSysErr());
          Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setContentText(e.getMessage());
            alert.setTitle(e.getClass().getName());
            alert.show();
          });
        }
        return null;
      }
    };
    runnerThread = new KanonizoThread(runnerTask);
    runnerThread.start();
  }

  @FXML
  public void interrupt() {
    orderedTests.clear();
    if (runnerThread != null) {
      runnerThread.interrupt();
    }
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
      loader.load();
      loader.setController(caller);
      Framework.getInstance().setDisplay(caller);
      Platform.runLater(() -> KanonizoFxApplication.stage.setScene(new Scene(loader.getRoot())));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    TableColumn<TestCase, Integer> id = new TableColumn<>("ID");
    id.setSortable(false);
    id.setCellValueFactory(
        col -> new ReadOnlyObjectWrapper<>(testCases.getItems().indexOf(col.getValue()) + 1));
    TableColumn<TestCase, String> className = new TableColumn<>("Class Name");
    className.setSortable(false);
    className.setMinWidth(200);
    className.setCellValueFactory(new PropertyValueFactory<>("testClassName"));

    TableColumn<TestCase, String> testName = new TableColumn<>("Test Name");
    testName.setSortable(false);
    testName.setMinWidth(200);
    testName.setCellValueFactory(new PropertyValueFactory<>("methodName"));
    testCases.getColumns().addAll(id, className, testName);
    Framework.getInstance().setDisplay(this);
    Framework.getInstance().addSelectionListener(this);
  }

  private void setTaskLabel(String text) {
    taskLabel.setText(text);
  }

  @Override
  public void testCaseSelected(TestCase tc) {
    addTestCase(tc);
  }

  private class KanonizoThread extends Thread {

    private boolean interrupted = false;

    public KanonizoThread(Runnable run) {
      super(run);
      SearchAlgorithm alg = Framework.getInstance().getAlgorithm();
      alg.getStoppingConditions()
          .removeIf(cond -> cond.getClass().equals(ThreadInterruptedStoppingCondition.class));

      alg.addStoppingCondition(new ThreadInterruptedStoppingCondition(this));
    }

    public void interrupt() {
      super.interrupt();
      this.interrupted = true;
    }
  }

  private class ThreadInterruptedStoppingCondition implements StoppingCondition {

    private KanonizoThread thread;

    public ThreadInterruptedStoppingCondition(KanonizoThread thread) {
      this.thread = thread;
    }

    @Override
    public boolean shouldFinish(SearchAlgorithm algorithm) {
      return thread.interrupted;
    }
  }
}
