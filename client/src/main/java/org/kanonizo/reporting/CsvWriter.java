package org.kanonizo.reporting;

import com.scythe.instrumenter.InstrumentationProperties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public abstract class CsvWriter {

  protected static final DateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");
  private String[] headers;
  private ArrayList<String[]> rows = new ArrayList<String[]>();
  private File logFile;
  private FileOutputStream stream;

  public CsvWriter() {
    String logDir = InstrumentationProperties.LOG_DIR + "/" + getDir() + "/";
    String logFileName = logDir + getLogFileName();
    File dir = new File(logDir);
    logFile = new File(logFileName);
    try {
      if (!dir.exists()) {
        dir.mkdirs();
      }
      if (!logFile.exists()) {
        logFile.createNewFile();
      }
      stream = new FileOutputStream(logFile, false);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public abstract String getDir();

  protected abstract void prepareCsv();

  protected String getLogFileName() {
    return (InstrumentationProperties.LOG_FILENAME.equals("") ? FORMAT.format(Calendar.getInstance().getTime())
        : InstrumentationProperties.LOG_FILENAME) + ".csv";
  }

  public void write() {
    prepareCsv();
    for (String[] row : rows) {
      writeRow(row);
    }
    if (stream != null) {
      try {
        stream.close();
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

  }

  protected void setHeaders(String[] headers) {
    this.headers = headers;
    writeRow(headers);
  }

  protected void addRow(String[] row) {
    rows.add(row);
  }

  protected void writeRow(String[] row) {
    try {
      stream.write((Arrays.stream(row).reduce((a, b) -> a + "," + b).get() + "\n").getBytes());
      stream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
