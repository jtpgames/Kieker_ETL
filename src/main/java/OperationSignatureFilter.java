import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.Property;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.record.controlflow.OperationExecutionRecord;

@Plugin(
        name = "Operation signature filter",
        description = "Filters the incoming operation execution records by signature.",
        outputPorts = {
                @OutputPort(name = OperationSignatureFilter.OUTPUT_PORT_NAME_FILTERED_EVENTS,
                        description = "Outputs the filtered events",
                        eventTypes = {OperationExecutionRecord.class})
        },
        configuration = {
                @Property(name = OperationSignatureFilter.CONFIG_PROPERTY_NAME_OPERATION,
                        description = "The string to be contained within the signature, e.g,. the method name",
                        defaultValue = "")
        })
public class OperationSignatureFilter extends AbstractFilterPlugin
{
    public static final String OUTPUT_PORT_NAME_FILTERED_EVENTS = "filteredEvents";
    public static final String INPUT_PORT_NAME_OPERATIONS = "operations";

    public static final String CONFIG_PROPERTY_NAME_OPERATION = "operation";

    private String _stringToBeInTheSignature = "";

    public OperationSignatureFilter(Configuration configuration, IProjectContext projectContext)
    {
        super(configuration, projectContext);

        _stringToBeInTheSignature = configuration.getStringProperty(CONFIG_PROPERTY_NAME_OPERATION);
    }

    @Override
    public Configuration getCurrentConfiguration()
    {
        final Configuration configuration = new Configuration();
        configuration.setProperty(CONFIG_PROPERTY_NAME_OPERATION, _stringToBeInTheSignature);

        return configuration;
    }

    @InputPort(
            name = OperationSignatureFilter.INPUT_PORT_NAME_OPERATIONS,
            description = "Receives incoming objects to be collected and forwarded",
            eventTypes = {OperationExecutionRecord.class})
    public void newRecord(final OperationExecutionRecord record)
    {
        if (_stringToBeInTheSignature.isEmpty())
        {
            super.deliver(OUTPUT_PORT_NAME_FILTERED_EVENTS, record);
        }
        else
        {
            if (record.getOperationSignature().contains(_stringToBeInTheSignature))
            {
                super.deliver(OUTPUT_PORT_NAME_FILTERED_EVENTS, record);
            }
        }
    }
}