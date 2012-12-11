package org.jbpm.servicestub.creditreport;

import javax.jws.WebService;

/**
 * Stub Web Service for testing the Homeloan BPM integration.
 */
@WebService(endpointInterface = "org.jbpm.servicestub.creditreport.CreditReportPortType")
public class CreditReportPortImpl implements CreditReportPortType {

    /**
     * Create a credit report from a given credit query.
     * <p>
     * The behaviour of this method is the same as that for the original BPEL process:
     * <ul>
     * <li>Copy all info from the input;</li>
     * <li>Take first three numbers from SSN to set credit score;</li>
     * <li>Delay the call for 1 second if the credit score is an odd number.</li>
     * </ul>
     */
    @Override
    public CreditReport creditReport(final CreditQuery cq) {
        final CreditReport cr = new ObjectFactory().createCreditReport();

        // Copy the input.
        cr.setFirstName(cq.getFirstName());
        cr.setLastName(cq.getLastName());
        cr.setDob(cq.getDob());
        cr.setSsn(cq.getSsn());

        // Set the credit score.
        cr.setScore(cq.getSsn().substring(0, 3));

        // If applicable, delay the execution.
        if (Integer.parseInt(cr.getScore()) % 2 == 1) {
            try {
                Thread.sleep(1000L);
            } catch (final InterruptedException intEx) {
                System.err.println("Unable to delay credit report service call: behaviour may differ from the expected.");
            }
        }

        return cr;
    }
}
