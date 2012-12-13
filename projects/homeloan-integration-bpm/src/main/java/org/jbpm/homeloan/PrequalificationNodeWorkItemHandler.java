package org.jbpm.homeloan;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;
import javax.xml.ws.BindingProvider;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.homeloan.prequalification.ObjectFactory;
import org.jbpm.homeloan.prequalification.PreQualDecisionServicePortType;
import org.jbpm.homeloan.prequalification.PreQualDecisionServiceService;
import org.jbpm.homeloan.prequalification.PreQualificationDecisionRequestType;
import org.jbpm.homeloan.prequalification.PreQualificationDecisionResponseType;

/**
 * WorkItem handler for determining the prequalification.
 */
public class PrequalificationNodeWorkItemHandler implements WorkItemHandler {
    // This is the service endpoint on the ESB.
    static final String ENDPOINT_ADDRESS = "http://localhost:8080/homeloan-origination-esb/ebws/homeloan-origination-demo/PreQualDecisionService";

    // TODO: Get the application as a whole from file for now; put it in the process context later.
    private static final String TEST_APPLICATION = "/PreQualificationDecisionRequest.xml";

    /** {@inheritDoc} */
    @Override
    public void executeWorkItem(final WorkItem item, final WorkItemManager itemMgr) {
        // Get the input.
        PreQualificationDecisionRequestType preQualDecisionRequest = new ObjectFactory().createPreQualificationDecisionRequestType();
        try {
            final InputStream is = PrequalificationNodeWorkItemHandler.class.getResourceAsStream(TEST_APPLICATION);
            System.out.println("InputStream = " + is);
            preQualDecisionRequest = JAXB.unmarshal(is, PreQualificationDecisionRequestType.class);
            System.out.println("Application = " + preQualDecisionRequest.getApplication());
        } catch (final DataBindingException dbEx) {
            dbEx.printStackTrace();
        }
        final int creditScore = Integer.parseInt((String) item.getParameter("creditScore"));

        // Map to service input.
        preQualDecisionRequest.setCreditScore(creditScore);

        // Call Web Service.
        final PreQualDecisionServicePortType port = new PreQualDecisionServiceService().getPreQualDecisionServicePortType();
        ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT_ADDRESS);
        final PreQualificationDecisionResponseType preQualDecisionResponse = port.preQualDecisionServiceOp(preQualDecisionRequest);

        // Signal that the work item is completed.
        final Map<String, Object> output = new HashMap<String, Object>();
        output.put("approved", preQualDecisionResponse.isApproved());
        output.put("approvedRate", preQualDecisionResponse.getApprovedRate());
        output.put("explanation", preQualDecisionResponse.getExplanation());
        output.put("insuranceCost", preQualDecisionResponse.getInsuranceCost());
        itemMgr.completeWorkItem(item.getId(), output);
    }

    /** {@inheritDoc} */
    @Override
    public void abortWorkItem(final WorkItem item, final WorkItemManager itemMgr) {
        System.err.println("Abort called on Prequalification Node");
    }
}
