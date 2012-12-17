package org.jbpm.homeloan;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.homeloan.prequalification.ApplicationType;
import org.jbpm.homeloan.prequalification.ObjectFactory;
import org.jbpm.test.JbpmJUnitTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a sample file to test a process.
 */
public class ProcessTest extends JbpmJUnitTestCase {
    private static final String TEST_APPLICATION = "/application.xml";

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
        ksession = createKnowledgeSession("HomeLoan.bpmn2");
        ksession.getWorkItemManager().registerWorkItemHandler("CreditReportNode", new CreditReportNodeWorkItemHandler());
        ksession.getWorkItemManager().registerWorkItemHandler("PrequalificationNode", new PrequalificationNodeWorkItemHandler());
    }

    @Test
    public void testProcess() {
        ApplicationType application = new ObjectFactory().createApplicationType();
        try {
            final InputStream is = ProcessTest.class.getResourceAsStream(TEST_APPLICATION);
            application = JAXB.unmarshal(is, ApplicationType.class);
        } catch (final DataBindingException dbEx) {
            dbEx.printStackTrace();
        }
        final Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("application", application);

        final ProcessInstance processInstance = ksession.startProcess("mortgages.HomeLoan", parms);

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
