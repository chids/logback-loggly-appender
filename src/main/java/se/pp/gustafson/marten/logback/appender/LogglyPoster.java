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
    private final int retries;
    private final long retryTimeout;

    public LogglyPoster(
            final URL enpoint,
            final SloppyCircularBuffer<String> queue,
            final int retries,
            final long retryTimeout)
    {
        super.setName(getClass().getSimpleName());
        super.setDaemon(true);
        this.retries = retries;
        this.retryTimeout = retryTimeout;
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
                final String message = this.queue.dequeue();
                for(int attempt = 0; attempt < this.retries; attempt++)
                {
                    if(post(message))
                    {
                        break;
                    }
                    sleepBeforeRetry(attempt);
                }
            }
        }
        catch(final InterruptedException e)
        {
            System.err.println(getClass().getSimpleName() + ": Interrupted, terminating");
        }
    }

    private void sleepBeforeRetry(final int attempt)
    {
        if(this.retryTimeout > 0)
        {
            try
            {
                final long sleepTime = this.retryTimeout + (this.retryTimeout * attempt);
                System.err.println(getClass().getSimpleName() + ": Failed, sleeping " + sleepTime + "ms before retry");
                Thread.sleep(sleepTime);
            }
            catch(final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean post(final String event)
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
            if(2 == (result / 100))
            {
                return true;
            }
            final String message = readResponseBody(connection.getInputStream());
            System.err.println("Failed with code: " + result + "\n" + message);
        }
        catch(final IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private static String readResponseBody(final InputStream input) throws IOException
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

    private static void sendAndClose(final String event, final OutputStream output) throws IOException
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