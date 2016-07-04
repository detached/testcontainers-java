package org.testcontainers.containers.output;

import java.util.Arrays;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

/**
 * @author <a href="mailto:simon@w3is.de">Simon Weis</a>
 * @since 03.07.16.
 */
public class LogOnFailureWatcher extends TestWatcher {
    private Container container;
    private Integer maxLogLines;
    private ToStringConsumer toStringConsumer = new ToStringConsumer();

    public LogOnFailureWatcher(Container container) {
        this.container = container;

        container.followOutput(toStringConsumer);
    }

    public LogOnFailureWatcher(Container container, int maxLogLines) {
        this(container);
        this.maxLogLines = maxLogLines;
    }

    @Override
    protected void failed(Throwable e, Description description) {
        String containerLog = toStringConsumer.toUtf8String();
        String finalLogMessage = maxLogLines == null ? containerLog : cutLogLines(containerLog, maxLogLines);

        Logger logger = LoggerFactory.getLogger(container.getContainerName());
        logger.error(finalLogMessage);
    }

    private String cutLogLines(String containerLog, int max) {
        String[] logLines = containerLog.split("\\r?\\n");
        String[] latestLines;

        if (logLines.length <= max) {
            latestLines = logLines;
        } else {
            latestLines = Arrays.copyOfRange(logLines, logLines.length - max, logLines.length);
        }

        return String.join("\n", latestLines);
    }
}
