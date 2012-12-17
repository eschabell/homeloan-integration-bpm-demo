package org.jbpm.homeloan;

import java.net.HttpURLConnection;
import java.net.URL;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.test.JbpmJUnitTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a sample file to test a process.
 */
public class ProcessTest extends JbpmJUnitTestCase {
    private StatefulKnowledgeSession ksession;

    @Before
    public void setupTestCase() throws Exception {
        // Make sure the ESB services are running before starting the tests.
        boolean found = false;
        try {
            found = exists(CreditReportNodeWorkItemHandler.ENDPOINT_ADDRESS + "?wsdl")
                    && exists(PrequalificationNodeWorkItemHandler.ENDPOINT_ADDRESS + "?wsdl");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if (!found) {
            throw new IllegalStateException("ESB services not found! Please start them before running the tests.");
        }

        // Set up the knowledge session with the process.
        ksession = createKnowledgeSession("homeloan.bpmn2");
        ksession.getWorkItemManager().registerWorkItemHandler("CreditReportNode", new CreditReportNodeWorkItemHandler());
        ksession.getWorkItemManager().registerWorkItemHandler("PrequalificationNode", new PrequalificationNodeWorkItemHandler());
    }

    @Test
    public void testProcess() {
        final ProcessInstance processInstance = ksession.startProcess("mortgages.homeloan");

        // Check whether the process instance has completed successfully.
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
        assertNodeTriggered(processInstance.getId(), "Read application", "Credit Report Node", "Prequalification Node", "Communicate rejection");
    }

    private static boolean exists(final String wsdlLocation) throws Exception {
        HttpURLConnection.setFollowRedirects(false);
        final HttpURLConnection connection = (HttpURLConnection) new URL(wsdlLocation).openConnection();
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }
}
