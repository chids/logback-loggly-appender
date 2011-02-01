package se.pp.gustafson.marten.logback.appender;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.PreSerializationTransformer;

public final class LogglyAppender extends AppenderBase<ILoggingEvent>
{
    private final PreSerializationTransformer<ILoggingEvent> pst = new LoggingEventPreSerializationTransformer();
    private URL endpoint;

    @Override
    protected void append(final ILoggingEvent event)
    {
        try
        {
            System.err.println(event.getFormattedMessage());
            final HttpURLConnection connection = (HttpURLConnection)this.endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.connect();
            final OutputStream os = connection.getOutputStream();
            os.write(event.getFormattedMessage().getBytes());
            os.close();
            connection.disconnect();
        }
        catch(IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setEndpoint(final String endpoint) throws MalformedURLException
    {
        this.endpoint = new URL(endpoint);
    }
}