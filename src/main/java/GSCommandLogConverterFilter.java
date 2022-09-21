import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.record.controlflow.OperationExecutionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Plugin(
        name = "GS Command Log Converter filter",
        description = "Transform an OperationExecutionRecord to a GSCommandLogEntry.",
        outputPorts = {
                @OutputPort(name = GSCommandLogConverterFilter.OUTPUT_PORT_NAME_GS_CMD_LOG_ENTRIES,
                        description = "Outputs the converted operation as a GS command log entry",
                        eventTypes = {GSCommandLogEntry.class})
        })
public class GSCommandLogConverterFilter extends AbstractFilterPlugin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GSCommandLogConverterFilter.class);
    public static final String INPUT_PORT_NAME_RECORDS = "newRecord";
    public static final String OUTPUT_PORT_NAME_GS_CMD_LOG_ENTRIES = "entries";

    public GSCommandLogConverterFilter(Configuration configuration, IProjectContext projectContext)
    {
        super(configuration, projectContext);
    }

    @Override
    public Configuration getCurrentConfiguration()
    {
        return new Configuration();
    }

    @InputPort(
            name = GSCommandLogConverterFilter.INPUT_PORT_NAME_RECORDS,
            description = "Receives the record to be converted",
            eventTypes = {OperationExecutionRecord.class})
    public void newRecord(final OperationExecutionRecord record)
    {
        var endTimestamp = record.getTout();
        var startTimestamp = record.getTin();

        var endTimestamp_s = endTimestamp / (long) Math.pow(10, 9);
        var endTimestamp_ns = endTimestamp % (long) Math.pow(10, 9);
        var startTimestamp_s = startTimestamp / (long) Math.pow(10, 9);
        var startTimestamp_ns = startTimestamp % (long) Math.pow(10, 9);

        var startInstant = Instant.ofEpochSecond(startTimestamp_s, startTimestamp_ns);
        var endInstant = Instant.ofEpochSecond(endTimestamp_s, endTimestamp_ns);

        String operation = record.getOperationSignature();
        // Remove visibility and return type signature
        operation = operation.replaceFirst("\\S*\\s\\S*\\s", "");
        // Remove artefact namespace
        operation = operation.replaceFirst("tools\\.descartes\\.teastore.", "");
        // Remove parameter signature
        operation = operation.replaceFirst("\\(.*\\)", "");

        var splittedOperation = operation.split("\\.");
        String classAndMethodName = Arrays
                .stream(splittedOperation)
                .skip(splittedOperation.length - 2)
                .collect(Collectors.joining("_"));

        var uniqueCommandId = record.hashCode();

        var startCommandLogEntry = new GSCommandLogEntry(
                uniqueCommandId,
                startInstant,
                "CMD-START",
                classAndMethodName
        );

        var endCommandLogEntry = new GSCommandLogEntry(
                uniqueCommandId,
                endInstant,
                "CMD-ENDE",
                classAndMethodName
        );

        // Deliver start of command
        super.deliver(GSCommandLogConverterFilter.OUTPUT_PORT_NAME_GS_CMD_LOG_ENTRIES, startCommandLogEntry);

        // Deliver end of command
        super.deliver(GSCommandLogConverterFilter.OUTPUT_PORT_NAME_GS_CMD_LOG_ENTRIES, endCommandLogEntry);
    }
}