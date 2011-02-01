package se.pp.gustafson.marten.logback.appender;

import java.net.MalformedURLException;
import java.net.URL;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public final class LogglyAppender extends AppenderBase<ILoggingEvent>
{
    private URL endpoint;
    private LogglyPoster poster;
    private int eventQueueSize;
    SloppyCircularBuffer<ILoggingEvent> queue;

    @Override
    protected void append(final ILoggingEvent event)
    {
        this.queue.enqueue(event);
    }

    @Override
    public void start()
    {
        super.start();
        if(this.endpoint == null)
        {
            System.err.println("No endpoint, can't start");
        }
        else
        {
        this.queue = new SloppyCircularBuffer<ILoggingEvent>(Math.max(1, this.eventQueueSize));
        this.poster = new LogglyPoster(this);
        this.poster.start();
        }
    }

    @Override
    public void stop()
    {
        this.poster.interrupt();
        super.stop();
    }

    public void setEndpoint(final String endpoint) throws MalformedURLException
    {
        this.endpoint = new URL(endpoint);
    }

    public URL getEndpointUrl()
    {
        return this.endpoint;
    }

    public void setQueueSize(final int maxSize)
    {
        this.eventQueueSize = maxSize;
    }
}