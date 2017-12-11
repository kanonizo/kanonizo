package org.kanonizo.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.scythe.instrumenter.InstrumentationProperties;

public abstract class CsvWriter {

  protected static final DateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");
  private String[] headers;
  private ArrayList<String[]> rows = new ArrayList<String[]>();

  public abstract String getDir();

  protected abstract void prepareCsv();

  protected String getLogFileName() {
    return (InstrumentationProperties.LOG_FILENAME.equals("") ? FORMAT.format(Calendar.getInstance().getTime())
        : InstrumentationProperties.LOG_FILENAME) + ".csv";
  }

  public void write() {
    prepareCsv();
    String logDir = InstrumentationProperties.LOG_DIR + "/" + getDir() + "/";
    String logFile = logDir + getLogFileName();
    File dir = new File(logDir);
    File file = new File(logFile);
    FileOutputStream stream = null;
    try {
      if (!dir.exists()) {
        dir.mkdirs();
      }
      if (!file.exists()) {
        file.createNewFile();
      }
      stream = new FileOutputStream(file);
      for (int i = 0; i < headers.length - 1; i++) {
        stream.write((headers[i] + ",").getBytes());
      }
      stream.write(headers[headers.length - 1].getBytes());
      stream.write("\n".getBytes());
      for (String[] row : rows) {
        for (int i = 0; i < row.length - 1; i++) {
          stream.write((row[i] + ",").getBytes());
        }
        stream.write(row[row.length - 1].getBytes());
        stream.write("\n".getBytes());
      }
      stream.flush();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  protected void setHeaders(String[] headers) {
    this.headers = headers;
  }

  protected void addRow(String[] row) {
    rows.add(row);
  }
}
