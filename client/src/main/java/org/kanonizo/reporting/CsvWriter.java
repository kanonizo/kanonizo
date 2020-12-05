package org.kanonizo.reporting;

import com.scythe.instrumenter.InstrumentationProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public abstract class CsvWriter
{
    protected static final DateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");
    private String[] headers;
    private ArrayList<Object[]> rows = new ArrayList<>();
    private FileOutputStream outputStream;

    public CsvWriter(Path logDirectory, String logFilename)
    {
        Path fullPathToLogFile = logDirectory.resolve(getDir()).resolve(getLogFileName(logFilename));
        File dir = fullPathToLogFile.getParent().toFile();
        File logFile = fullPathToLogFile.toFile();
        try
        {
            if (!dir.exists())
            {
                dir.mkdirs();
            }
            if (!logFile.exists())
            {
                logFile.createNewFile();
            }
            outputStream = new FileOutputStream(logFile, false);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    protected abstract String getDir();

    protected abstract void prepareCsv();

    private String getLogFileName(String logFilename)
    {
        return isEmpty(logFilename) ? FORMAT.format(Calendar.getInstance().getTime()) : logFilename;
    }

    public void write()
    {
        prepareCsv();
        for (Object[] row : rows)
        {
            writeRow(row);
        }
        if (outputStream != null)
        {
            try
            {
                outputStream.close();
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }

    }

    protected void setHeaders(String... headers)
    {
        this.headers = headers;
        writeRow(headers);
    }

    protected void addRow(Object... row)
    {
        rows.add(row);
    }

    protected void writeRow(Object... row)
    {
        try
        {
            String collectedRow = Arrays.stream(row).map(Object::toString).collect(Collectors.joining(","));
            outputStream.write((collectedRow + "\n").getBytes());
            outputStream.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
