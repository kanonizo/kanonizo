package org.kanonizo.typeadapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.instrumenters.InstrumenterType;
import org.kanonizo.instrumenters.NullInstrumenter;

import java.io.IOException;

public class InstrumenterAdapter extends TypeAdapter<Instrumenter>
{
    @Override
    public void write(JsonWriter out, Instrumenter instrumenter) throws IOException
    {
        out.beginObject();
        out.name("class");
        out.value(instrumenter.commandLineSwitch())
        out.value(instrumenter.getClass().getName());
        out.endObject();
    }

    @Override
    public Instrumenter read(JsonReader in) throws IOException
    {
        try
        {
            in.beginObject();
            String cl = in.nextName();
            if (cl.equals("class"))
            {
                String instrumenterComamndLineSwitch = in.nextString();
                return InstrumenterType.fromCommandLineSwitch(instrumenterComamndLineSwitch).getInstrumenterFactory().from();
            }
            return new NullInstrumenter();
        }
        finally
        {
            in.endObject();
        }
    }
}
