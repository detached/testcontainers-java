package org.testcontainers.containers.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.function.Consumer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

/**
 * @author <a href="mailto:simon.weis@1und1.de">Simon Weis</a>
 * @since 03.07.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class LogOnFailureWatcherTest {

    private static final String LOG_LINE = "Test log line";
    private static final int AMOUNT_LOG_LINES = 3;
    private static final int LOG_LINES_LIMIT = 2;
    private static final int SMALL_CONTAINER_LOG = 1;

    @Mock
    private Container container;

    @Mock
    private Appender<ILoggingEvent> appender;

    @Captor
    private ArgumentCaptor<Consumer<OutputFrame>> consumerArgumentCaptor;

    @Captor
    private ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor;

    @Before
    public void setUp() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        when(appender.getName()).thenReturn("MockAppender");
        root.addAppender(appender);

        when(container.getContainerName()).thenReturn("MockContainer");
    }

    @Test
    public void testLogLinesOnFailedTest() throws Exception {

        LogOnFailureWatcher logOnFailureWatcher = new LogOnFailureWatcher(container);

        fillContainerLog(AMOUNT_LOG_LINES, container);

        logOnFailureWatcher.failed(null, null);

        verifyLogContainsLines(AMOUNT_LOG_LINES);
    }

    @Test
    public void testLogOnlyLastLinesOnFailedTest() throws Exception {
        LogOnFailureWatcher logOnFailureWatcher = new LogOnFailureWatcher(container, LOG_LINES_LIMIT);

        fillContainerLog(AMOUNT_LOG_LINES, container);

        logOnFailureWatcher.failed(null, null);

        verifyLogContainsLines(LOG_LINES_LIMIT);
    }

    @Test
    public void testContainerLogContainsLessLinesThanMaximum() throws Exception {
        LogOnFailureWatcher logOnFailureWatcher = new LogOnFailureWatcher(container, LOG_LINES_LIMIT);

        fillContainerLog(SMALL_CONTAINER_LOG, container);

        logOnFailureWatcher.failed(null, null);

        verifyLogContainsLines(SMALL_CONTAINER_LOG);
    }

    private void fillContainerLog(int amount, Container container) {
        verify(container).followOutput(consumerArgumentCaptor.capture());
        Consumer<OutputFrame> consumer = consumerArgumentCaptor.getValue();

        for (int i = 0; i < amount; i++) {
            consumer.accept(new OutputFrame(OutputFrame.OutputType.STDOUT,
                                            (LOG_LINE + "\n").getBytes()));
        }
    }

    private void verifyLogContainsLines(int amount) {
        verify(appender).doAppend(loggingEventArgumentCaptor.capture());

        ILoggingEvent loggingEvent = loggingEventArgumentCaptor.getValue();
        String[] logLines = loggingEvent.getFormattedMessage().split("\\r?\\n");

        assertEquals(Level.ERROR, loggingEvent.getLevel());
        assertEquals(amount, logLines.length);
        assertTrue(Arrays.stream(logLines).allMatch(s -> s.equals(LOG_LINE)));
    }
}