package se.pp.gustafson.marten.logback.appender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class LogglyPoster extends Thread
{
    private int numRetries = 3;
    private long timeoutInMillis = 3000l;
    private final SloppyCircularBuffer<String> queue;
    private final URL endpoint;
    private String strEndpoint;

    public LogglyPoster(final URL endpoint, String strEndpoint, final SloppyCircularBuffer<String> queue)
    {
        super.setName(getClass().getSimpleName());
        super.setDaemon(true);
        this.endpoint = endpoint;
        this.strEndpoint = strEndpoint;
        this.queue = queue;
    }
    
     public LogglyPoster(final URL enpoint,
                         final String strEndpoint,
                         final SloppyCircularBuffer<String> queue,
                         final int numTries,
                         final long timeoutInMillis)
    {
        super.setName(getClass().getSimpleName());
        super.setDaemon(true);
        this.endpoint = enpoint;
        this.strEndpoint = strEndpoint;
        this.queue = queue;
        this.numRetries = numTries;
        this.timeoutInMillis = timeoutInMillis;
    }   

    @Override
    public void run()
    {
        try
        {
            while(!super.isInterrupted())
            {
                post(this.queue.dequeue());
                Thread.yield();
            }
        }
        catch(final InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void post(final String event)
    {
        int retryNo = 0;
        int numRetries = this.numRetries;
        
        do {
            try
            {
                final HttpURLConnection connection = (HttpURLConnection)this.endpoint.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.connect();
                sendAndClose(event, connection.getOutputStream());
                connection.disconnect();
                final int result = connection.getResponseCode();

                // for any http code not in the 200 range (200..300) print an error
                if(2 != (result / 100))
                {
                    final String message = readResponseBody(connection.getInputStream());
                    System.err.println("Failed with HTTP error code: " + result + "\n" + message);
                }
                else
                {
                    // success - exit the for loop
                    return;
                }
            }
            catch(final IOException e)
            {
                e.printStackTrace();
                System.err.println("URL: "+this.strEndpoint+" Payload: "+event);
            }
            
            if (timeoutInMillis > 0) {
                try {
                    int thresholdOffset = (int)(Math.random() * timeoutInMillis);
                    int dir = (int)(Math.random() * 2);
                    if (dir == 0) {
                        dir = -1;
                    }
                    
                    System.out.println("sleeping for "+(timeoutInMillis + (dir * thresholdOffset))+" milliseconds");
                    Thread.sleep(timeoutInMillis + (dir * thresholdOffset));
                } catch (InterruptedException ie) {
                    
                }
            }
            
            System.err.println("retry #"+(retryNo + 1));
            retryNo++;
        }
        while (retryNo < numRetries);
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