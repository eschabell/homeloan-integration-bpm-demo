package org.jbpm.homeloan;

import java.util.HashMap;
import java.util.Map;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

/**
 * WorkItem handler for determining the prequalification.
 */
public class PrequalificationNodeWorkItemHandler implements WorkItemHandler {
    /** {@inheritDoc} */
    @Override
    public void executeWorkItem(final WorkItem item, final WorkItemManager itemMgr) {
        item.getParameter("creditScore");

        // TODO: Map to service input.

        // TODO: Call Web Service.

        // Signal that the work item is completed.
        final Map<String, Object> output = new HashMap<String, Object>();
        output.put("approved", Boolean.valueOf(true));
        output.put("approvedRate", Float.valueOf(123.45F));
        output.put("explanation", "");
        output.put("insuranceCost", Float.valueOf(100.00F));
        itemMgr.completeWorkItem(item.getId(), output);
    }

    /** {@inheritDoc} */
    @Override
    public void abortWorkItem(final WorkItem item, final WorkItemManager itemMgr) {
        System.err.println("Abort called on Prequalification Node");
    }
}
