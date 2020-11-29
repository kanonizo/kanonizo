package org.kanonizo.typeadapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;

public class FileTypeAdapter extends TypeAdapter<File>
{

    @Override
    public void write(JsonWriter out, File file) throws IOException
    {
        out.beginObject();
        out.name("path");
        if (file == null)
        {
            out.value((String) null);
        }
        else
        {
            out.value(file.getAbsolutePath());
        }
        out.endObject();
    }

    @Override
    public File read(JsonReader in) throws IOException
    {
        in.beginObject();
        String name = in.nextName();
        File f = null;
        if (name.equals("path"))
        {
            String fileName = in.nextString();
            if (fileName == null)
            {
                f = null;
            }
            else
            {
                f = new File(fileName);
            }
        }
        in.endObject();
        return f;
    }
}
