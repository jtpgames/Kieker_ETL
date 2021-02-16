import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.Property;
import kieker.analysis.plugin.reader.AbstractReaderPlugin;
import kieker.analysis.plugin.reader.filesystem.TextFileStreamProcessor;
import kieker.analysis.plugin.reader.util.IMonitoringRecordReceiver;
import kieker.common.configuration.Configuration;
import kieker.common.record.IMonitoringRecord;
import kieker.common.registry.reader.ReaderRegistry;

import java.io.File;
import java.io.FileInputStream;

@Plugin(
        name = "Pipe reader",
        description = "Reads records from a configured pipe",
        outputPorts = {@OutputPort(
                name = TeaStoreLogFileReader.OUTPUT_PORT_NAME_RECORDS,
                description = "Outputs any received record",
                eventTypes = {IMonitoringRecord.class})
        },
        configuration = {@Property(
                name = TeaStoreLogFileReader.CONFIG_PROPERTY_NAME_FILE_NAME,
                defaultValue = "kieker-pipe")
        })
public class TeaStoreLogFileReader extends AbstractReaderPlugin implements IMonitoringRecordReceiver
{
    public static final String OUTPUT_PORT_NAME_RECORDS = "logRecords";
    public static final String CONFIG_PROPERTY_NAME_FILE_NAME = "filename";

    private final ReaderRegistry<String> _stringRegistry = new ReaderRegistry<>();
    private final TextFileStreamProcessor _textFileStreamProcessor;
    private final String _inputFilePath;

    public TeaStoreLogFileReader(final Configuration configuration, final IProjectContext projectContext)
    {
        super(configuration, projectContext);

        _inputFilePath = configuration.getStringProperty(CONFIG_PROPERTY_NAME_FILE_NAME);

        _stringRegistry.register(0, "kieker.common.record.controlflow.OperationExecutionRecord");

        _textFileStreamProcessor = new TextFileStreamProcessor(
                false,
                _stringRegistry,
                this
        );
    }

    @Override
    public void terminate(boolean error)
    {

    }

    @Override
    public Configuration getCurrentConfiguration()
    {
        var config = new Configuration();
        config.setProperty(CONFIG_PROPERTY_NAME_FILE_NAME, _inputFilePath);

        return config;
    }

    @Override
    public boolean read()
    {
        File inputFile = new File(_inputFilePath);

        try
        {
            _textFileStreamProcessor.processInputChannel(new FileInputStream(inputFile));
        } catch (final Exception ex)
        {
            System.err.printf("Error reading %s: %s%n", inputFile, ex);
            return false;
        }

        return true;
    }

    @Override
    public boolean newMonitoringRecord(IMonitoringRecord record)
    {
        super.deliver(OUTPUT_PORT_NAME_RECORDS, record);

        return true;
    }

    @Override
    public void newEndOfFileRecord()
    {
    }
}