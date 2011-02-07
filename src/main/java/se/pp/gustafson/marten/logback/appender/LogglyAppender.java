package se.pp.gustafson.marten.logback.appender;

import java.net.MalformedURLException;
import java.net.URL;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public final class LogglyAppender extends AppenderBase<ILoggingEvent>
{
    private URL endpoint;
    private LogglyPoster poster;
    private int eventQueueSize;
    private SloppyCircularBuffer<String> queue;
    private Layout<ILoggingEvent> layout;

    @Override
    protected void append(final ILoggingEvent event)
    {
        this.queue.enqueue(this.layout.doLayout(event));
    }

    @Override
    public void start()
    {
        if(this.endpoint == null)
        {
            super.addError("No endpoint set for appender [" + super.name + "].");
        }
        else if(this.layout == null)
        {
            super.addError("No layout set for appender [" + super.name + "].");
        }
        else
        {
            final int queueSize = Math.max(1, this.eventQueueSize);
            this.queue = new SloppyCircularBuffer<String>(queueSize);
            this.poster = new LogglyPoster(this.endpoint, this.queue);
            this.poster.start();
            super.start();
            super.addInfo("Appender [" + super.name + "] started with a queue size of " + queueSize);
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

    public void setQueueSize(final int maxSize)
    {
        this.eventQueueSize = maxSize;
    }

    public Layout<ILoggingEvent> getLayout()
    {
        return this.layout;
    }

    public void setLayout(final Layout<ILoggingEvent> layout)
    {
        this.layout = layout;
    }
}