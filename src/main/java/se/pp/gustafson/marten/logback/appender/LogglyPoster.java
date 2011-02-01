package se.pp.gustafson.marten.logback.appender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import javax.sound.midi.SysexMessage;

import ch.qos.logback.classic.spi.ILoggingEvent;

public final class LogglyPoster extends Thread
{

    private final LogglyAppender appender;

    public LogglyPoster(final LogglyAppender appender)
    {
        super.setName(getClass().getSimpleName());
        super.setDaemon(true);
        this.appender = appender;
    }

    @Override
    public void run()
    {
        try
        {
            while(!super.isInterrupted())
            {
                post(this.appender.queue.dequeue());
                Thread.yield();
            }
        }
        catch(final InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void post(final ILoggingEvent event)
    {
        try
        {
            final HttpURLConnection connection = (HttpURLConnection)this.appender.getEndpointUrl().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.connect();
            final OutputStream os = connection.getOutputStream();
            os.write(event.getFormattedMessage().getBytes());
            os.close();
            connection.disconnect();
            final int result = connection.getResponseCode();
            final InputStream response = connection.getInputStream();
            final byte[] rawResponse = new byte[response.available()];
            response.read(rawResponse);
            response.close();
            System.err.println(new String(rawResponse));
            System.err.println(connection.getResponseMessage());
            if(2 != (result / 100))
            {
                System.err.println("Failed with code: " + result);
            }
        }
        catch(IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}