package se.pp.gustafson.marten.logback.appender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class LogglyPoster extends Thread
{

    private final SloppyCircularBuffer<String> queue;
    private final URL endpoint;

    public LogglyPoster(final URL enpoint, final SloppyCircularBuffer<String> queue)
    {
        super.setName(getClass().getSimpleName());
        super.setDaemon(true);
        this.endpoint = enpoint;
        this.queue = queue;
    }

    @Override
    public void run()
    {
        try
        {
            while(!super.isInterrupted())
            {
                post(this.queue.dequeue());
            }
        }
        catch(final InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void post(final String event)
    {
        try
        {
            final HttpURLConnection connection = (HttpURLConnection)this.endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.connect();
            sendAndClose(event, connection.getOutputStream());
            connection.disconnect();
            final int result = connection.getResponseCode();
            if(2 != (result / 100))
            {
                final String message = readResponseBody(connection.getInputStream());
                System.err.println("Failed with code: " + result + "\n" + message);
            }
        }
        catch(final IOException e)
        {
            e.printStackTrace();
        }
    }

    private String readResponseBody(final InputStream input) throws IOException
    {
        try
        {
            final byte[] response = new byte[input.available()];
            input.read(response);
            return new String(response);
        }
        finally
        {
            input.close();
        }
    }

    private void sendAndClose(final String event, final OutputStream output) throws IOException
    {
        try
        {
            output.write(event.getBytes());
        }
        finally
        {
            output.close();
        }
    }
}