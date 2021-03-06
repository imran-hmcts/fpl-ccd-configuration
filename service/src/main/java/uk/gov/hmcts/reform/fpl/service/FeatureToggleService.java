package uk.gov.hmcts.reform.fpl.service;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.LDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FeatureToggleService {

    private final LDClient ldClient;
    private final String ldUserKey;
    private final String environment;

    private static final String LOCAL_AUTHORITY_NAME_KEY = "localAuthorityName";

    @Autowired
    public FeatureToggleService(LDClient ldClient, @Value("${ld.user_key}") String ldUserKey,
                                @Value("${fpl.env}") String environment) {
        this.ldClient = ldClient;
        this.ldUserKey = ldUserKey;
        this.environment = environment;
    }

    public boolean isSummaryTabOnEventEnabled() {
        return ldClient.boolVariation("summary-tab-on-event-update", createLDUser(), false);
    }

    public boolean isCtscEnabled(String localAuthorityName) {
        return ldClient.boolVariation("CTSC",
            createLDUser(Map.of(LOCAL_AUTHORITY_NAME_KEY, LDValue.of(localAuthorityName))), false);
    }

    public boolean isCtscReportEnabled() {
        return ldClient.boolVariation("CTSC",
            createLDUser(Map.of("report", LDValue.of(true))), false);
    }

    public boolean isAllowCaseCreationForUsersNotOnboardedToMOEnabled(String localAuthorityName) {
        return ldClient.boolVariation("allow-case-creation-for-users-not-onboarded-to-mo",
            createLDUser(Map.of(LOCAL_AUTHORITY_NAME_KEY, LDValue.of(localAuthorityName))), false);
    }

    public boolean isRestrictedFromCaseSubmission(String localAuthorityName) {
        return ldClient.boolVariation("restrict-case-submission",
            createLDUser(Map.of(LOCAL_AUTHORITY_NAME_KEY, LDValue.of(localAuthorityName))), false);
    }

    public boolean isSendLAEmailsToSolicitorEnabled(String localAuthorityName) {
        return ldClient.boolVariation("send-la-emails-to-solicitor",
            createLDUser(Map.of(LOCAL_AUTHORITY_NAME_KEY, LDValue.of(localAuthorityName))), false);
    }

    public boolean isAddHearingsInPastEnabled() {
        return ldClient.boolVariation("add-hearings-in-past", createLDUser(), false);
    }

    public boolean isApplicationDocumentsEventEnabled() {
        return ldClient.boolVariation("application-documents-event", createLDUser(), false);
    }

    public boolean isEpoOrderTypeAndExclusionEnabled() {
        return ldClient.boolVariation("epo-order-type-and-exclusion", createLDUser(), false);
    }

    public boolean isDraftOrdersEnabled() {
        return ldClient.boolVariation("draft-orders", createLDUser(), false);
    }

    public boolean isSummaryTabEnabled() {
        return ldClient.boolVariation("summary-tab-update", createLDUser(), false);
    }

    public boolean isSummaryTabFirstCronRunEnabled() {
        return ldClient.boolVariation("summary-tab-first-run", createLDUser(), false);
    }

    private LDUser createLDUser() {
        return createLDUser(Map.of());
    }

    private LDUser createLDUser(Map<String, LDValue> values) {
        LDUser.Builder builder = new LDUser.Builder(ldUserKey)
            .custom("timestamp", String.valueOf(System.currentTimeMillis()))
            .custom("environment", environment);

        values.forEach(builder::custom);
        return builder.build();
    }

}
