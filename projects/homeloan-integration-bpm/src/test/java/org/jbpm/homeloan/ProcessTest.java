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
import org.jbpm.homeloan.creditreport.CreditQuery;
import org.jbpm.homeloan.prequalification.ApplicationType;
import org.jbpm.homeloan.prequalification.ObjectFactory;
import org.jbpm.test.JbpmJUnitTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is a sample file to test a process.
 */
public class ProcessTest extends JbpmJUnitTestCase {
    private static final String PROCESS_NAME = "HomeLoan.bpmn2";
    private static final String TEST_APPLICATION = "/application.xml";

    private static final String SSN_LOW_SCORE = "567-89-1234";
    private static final String SSN_HIGH_SCORE = "789-12-3456";

    private static StatefulKnowledgeSession ksession;

    @BeforeClass
    public static void setUpOnce() throws Exception {
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
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Set up the knowledge session with the process and handlers.
        ksession = createKnowledgeSession(PROCESS_NAME);
        ksession.getWorkItemManager().registerWorkItemHandler("CreditReportNode", new CreditReportNodeWorkItemHandler());
        ksession.getWorkItemManager().registerWorkItemHandler("PrequalificationNode", new PrequalificationNodeWorkItemHandler());
    }

    @Test
    public void happyFlow() {
        final Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("application", getTestApplication());
        parms.put("creditQuery", getCreditQuery(SSN_HIGH_SCORE));

        final ProcessInstance processInstance = ksession.startProcess("mortgages.HomeLoan", parms);

        // Check whether the process instance has completed successfully.
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
        assertNodeTriggered(processInstance.getId(), "Read application", "Credit Report Node", "Prequalification Node", "Communicate approval");
    }

    @Test
    public void lowCreditScore() {
        final Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("application", getTestApplication());
        parms.put("creditQuery", getCreditQuery(SSN_LOW_SCORE));

        final ProcessInstance processInstance = ksession.startProcess("mortgages.HomeLoan", parms);

        // Check whether the process instance has completed successfully.
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
        assertNodeTriggered(processInstance.getId(), "Read application", "Credit Report Node", "Prequalification Node", "Communicate rejection");
    }

    /**
     * Check whether a given URL hosts a WSDL (or at least some other resource).
     * 
     * @param wsdlLocation
     *            The URL on which the WSDL is supposed to be hosted.
     * @return Whether the WSDL is hosted at the given URL.
     * @throws Exception
     *             If the given URL is not well-formed, no connection could be set up or no response code could be retrieved.
     */
    private static boolean exists(final String wsdlLocation) throws Exception {
        HttpURLConnection.setFollowRedirects(false);
        final HttpURLConnection connection = (HttpURLConnection) new URL(wsdlLocation).openConnection();
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    /**
     * Retrieve a loan application for testing purposes, from the configured file location.
     * 
     * @return The test application, unmarshalled from XML.
     */
    private static ApplicationType getTestApplication() {
        ApplicationType application = new ObjectFactory().createApplicationType();
        try {
            final InputStream is = ProcessTest.class.getResourceAsStream(TEST_APPLICATION);
            application = JAXB.unmarshal(is, ApplicationType.class);
        } catch (final DataBindingException dbEx) {
            dbEx.printStackTrace();
        }
        return application;
    }

    private static CreditQuery getCreditQuery(final String ssn) {
        final CreditQuery query = new org.jbpm.homeloan.creditreport.ObjectFactory().createCreditQuery();
        query.setFirstName("John");
        query.setLastName("Doe");
        query.setDob("1970/01/01");
        query.setSsn(ssn);
        return query;
    }
}
