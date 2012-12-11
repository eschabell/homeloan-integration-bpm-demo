package org.jbpm.servicestub.prequalification;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jws.WebService;

import org.apache.commons.lang3.time.DateUtils;

/**
 * Stub Web Service for testing the Homeloan BPM integration.
 */
@WebService(endpointInterface = "org.jbpm.servicestub.prequalification.PreQualDecisionServicePortType")
public class PreQualDecisionServicePortImpl implements PreQualDecisionServicePortType {

    /**
     * Make a pre-qualification decision based on the incoming application plus credit report.
     * <p>
     * It takes a couple of the issues from the original service into account - but not all.<br>
     * Supported:
     * <ul>
     * <li>Bad credit check (score under 700, rating C, D or F);</li>
     * <li>Under age (age under 21).</li>
     * </ul>
     */
    @Override
    public PreQualificationDecisionResponseType preQualDecisionServiceOp(final PreQualificationDecisionRequestType request) {
        final PreQualificationDecisionResponseType response = new ObjectFactory().createPreQualificationDecisionResponseType();

        boolean approved = true;
        String explanation = null;

        // Credit score lower than 700?
        if (request.getCreditScore() < 700) {
            approved = false;
            explanation = "Not Acceptable Credit Rating";
        }
        // Applicant under the age of 21?
        final String dob = request.getApplication().getBorrowers().getBorrower().get(0).getDOB();
        Date dobDate = null;
        try {
            dobDate = new SimpleDateFormat("MM/dd/yyyy").parse(dob);
        } catch (final ParseException parseEx) {
            throw new IllegalArgumentException("Unable to determine DoB for applicant.", parseEx);
        }
        if (DateUtils.addYears(new Date(), -21).before(dobDate)) {
            approved = false;
            explanation = "Applicant is underage";
        }

        float approvedRate = 0.00F;
        BigDecimal insuranceCost = BigDecimal.ZERO;
        if (approved) {
            final BigDecimal ratio = request.getApplication().getAmount().divide(new BigDecimal(200000));
            approvedRate = 7.00F - ratio.floatValue();
            if (approvedRate < 3.5F) {
                approvedRate = 3.5F;
            }
            insuranceCost = BigDecimal.TEN.add(ratio).multiply(BigDecimal.TEN);
        }

        // Fill the response.
        response.setApproved(approved);
        response.setApprovedRate(approvedRate);
        response.setExplanation(explanation);
        response.setInsuranceCost(insuranceCost);

        return response;
    }
}
