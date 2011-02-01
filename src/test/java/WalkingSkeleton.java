import static org.junit.Assert.fail;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class WalkingSkeleton
{
    @Test
    public void configuredWithFile() throws Exception
    {
        final URL url = getClass().getClassLoader().getResource("logback-loggly.xml");
        try
        {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(this.context);
            this.context.reset();
            configurator.doConfigure(new File(url.getFile()));
        }
        catch(final JoranException e)
        {
            fail(e.getMessage());
        }
        final Logger root = this.context.getLogger(getClass());
        root.error(InetAddress.getLocalHost().getHostAddress() + " - whoho");
        root.error(InetAddress.getLocalHost().getHostAddress() + " - foo");
        root.error(InetAddress.getLocalHost().getHostAddress() + " - bar!");
        System.in.read();
    }

    @After
    public void tearDown()
    {
        this.context = null;
    }

    @Before
    public void setUp()
    {
        this.context = (LoggerContext)LoggerFactory.getILoggerFactory();
    }

    private LoggerContext context;

}