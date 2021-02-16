import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.record.controlflow.OperationExecutionRecord;

import java.util.ArrayList;
import java.util.List;

@Plugin(
        name = "Collect to list filter",
        description = "Collects the incoming events in an internal list.",
        outputPorts = {
                @OutputPort(name = CollectToListFilter.OUTPUT_PORT_NAME_RELAYED_EVENTS,
                        description = "Relays the received events",
                        eventTypes = {Object.class})
        })
public class CollectToListFilter extends AbstractFilterPlugin
{
    public static final String OUTPUT_PORT_NAME_RELAYED_EVENTS = "relayed_events";
    public static final String INPUT_PORT_NAME_EVENTS = "events";

    private final List<Object> internalList = new ArrayList<>(10000);
    public List<Object> getInternalList()
    {
        return internalList;
    }

    public CollectToListFilter(Configuration configuration, IProjectContext projectContext)
    {
        super(configuration, projectContext);
    }

    @Override
    public Configuration getCurrentConfiguration()
    {
        return new Configuration();
    }

    @InputPort(
            name = CollectToListFilter.INPUT_PORT_NAME_EVENTS,
            description = "Receives incoming objects to be collected and forwarded",
            eventTypes = {Object.class})
    public void inputEvent(final Object event)
    {
        internalList.add(event);

        super.deliver(OUTPUT_PORT_NAME_RELAYED_EVENTS, event);
    }
}
