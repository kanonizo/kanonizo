package org.kanonizo.typeadapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.kanonizo.algorithms.SearchAlgorithm;

import java.io.IOException;

public class AlgorithmAdapter extends TypeAdapter<SearchAlgorithm>
{
    @Override
    public void write(JsonWriter out, SearchAlgorithm searchAlgorithm) throws IOException
    {
        out.beginObject();
        out.name("name");
        out.value(searchAlgorithm.getClass().getName());
        out.endObject();
    }

    @Override
    public SearchAlgorithm read(JsonReader in) throws IOException
    {
        in.beginObject();
        String name = in.nextName();
        SearchAlgorithm sa = null;
        if (name.equals("name"))
        {
            String className = in.nextString();
            try
            {
                sa = (SearchAlgorithm) Class.forName(className).newInstance();
            }
            catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        in.endObject();
        return sa;
    }
}
