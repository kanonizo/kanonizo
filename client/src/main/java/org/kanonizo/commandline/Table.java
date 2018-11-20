package org.kanonizo.commandline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;

public class Table {

  public static char SEP_CHAR = '-';
  public static char TABLE_BORDER = '|';
  private int tableWidth = -1;
  private int[] colWidths;
  private String[] headers;
  private List<String[]> rows = new ArrayList<>();

  public Table(int... colWidths) {
    this.colWidths = colWidths;
    headers = new String[colWidths.length];
  }

  public void setHeaders(String... headers) {
    if (headers.length != this.headers.length) {
      throw new IllegalArgumentException("This table was constructed with " + this.headers.length
          + " columns, and you are trying to set it to have " + headers.length + " headers!");
    }
    System.arraycopy(headers, 0, this.headers, 0, headers.length);
  }

  public void addRow(String... values) {
    if (values.length != colWidths.length) {
      throw new IllegalArgumentException("This table was constructed with " + this.colWidths.length
          + " columns, and you are trying to add a row with " + values.length + " columns!");
    }
    rows.add(values);
  }

  private void printSep() {
    if(tableWidth == -1) {
      tableWidth =
          IntStream.of(colWidths).sum() + colWidths.length + 1;
    }
    System.out.println(StringUtils.repeat(SEP_CHAR, tableWidth));
  }

  public void print() {
    printSep();
    StringBuilder formatString = new StringBuilder();
    formatString.append(TABLE_BORDER);
    for (int width : colWidths) {
      formatString.append("%-" + width + "s"+TABLE_BORDER);
    }
    System.out.println(String.format(formatString.toString(), headers));
    printSep();
    for (String[] row : rows) {
      boolean multiRow = false;
      List<Integer> overflowCols = new ArrayList<>();
      // if any column exceeds its max width
      for (int i = 0; i <row.length; i++){
        if (row[i].length() > colWidths[i]){
          multiRow = true;
          overflowCols.add(i);
        }
      }
      if(!multiRow){
        System.out.println(String.format(formatString.toString(), row));
      } else {
        boolean first = true;
        while(overflowCols.size() > 0){
          StringBuilder rowString = new StringBuilder();
          rowString.append(TABLE_BORDER);
          for (int i = 0; i < colWidths.length; i++){

            if(overflowCols.contains(i)){
              String chunk = row[i];
              int cutOff = Math.min(colWidths[i], chunk.length());
              if(chunk.length() > colWidths[i]){
                cutOff = Math.min(cutOff, chunk.substring(0,cutOff).lastIndexOf(" "));
              }
              chunk = chunk.substring(0, cutOff).trim();
              row[i] = row[i].substring(cutOff);
              rowString.append(chunk);
              rowString.append(StringUtils.repeat(" ", colWidths[i] - chunk.length()));
              rowString.append(TABLE_BORDER);
              if(row[i].length() == 0){
                overflowCols.remove((Object)i);
              }

            } else {
              if(first) {
                rowString.append(row[i]);
                rowString.append(StringUtils.repeat(" ", colWidths[i] - row[i].length()));
                rowString.append(TABLE_BORDER);
              } else {
                rowString.append(StringUtils.repeat(" ", colWidths[i]));
                rowString.append(TABLE_BORDER);
              }
            }
          }
          System.out.println(rowString.toString());
          first=false;
        }


      }
    }
    printSep();
  }
}
