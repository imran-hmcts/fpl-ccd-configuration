package uk.gov.hmcts.reform.fpl.service.email.content;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.service.HearingBookingService;
import uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper;
import uk.gov.hmcts.reform.fpl.utils.FixedTimeConfiguration;

import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.utils.CoreCaseDataStoreLoader.populatedCaseDetails;

@ContextConfiguration(classes = {C2UploadedEmailContentProvider.class, EmailNotificationHelper.class,
    HearingBookingService.class, FixedTimeConfiguration.class})
class C2UploadedEmailContentProviderTest extends AbstractEmailContentProviderTest {


    @Autowired
    private C2UploadedEmailContentProvider c2UploadedEmailContentProvider;

    @Test
    void shouldReturnExpectedMapWithGivenCaseDetails() {
        Map<String, Object> expectedMap = ImmutableMap.<String, Object>builder()
            .put("caseUrl", buildCaseUrl(CASE_REFERENCE))
            .put("subjectLine", format("Smith, %s", CASE_REFERENCE))
            .put("hearingDetailsCallout", format("Smith, %s", CASE_REFERENCE))
            .put("reference", CASE_REFERENCE)
            .build();

        assertThat(c2UploadedEmailContentProvider.buildC2UploadNotification(populatedCaseDetails()))
            .isEqualTo(expectedMap);
    }

    @Test
    void shouldReturnExpectedPbaPaymentNotTakenNotification() {
        Map<String, Object> expectedMap = ImmutableMap.<String, Object>builder()
            .put("caseUrl", buildCaseUrl(CASE_REFERENCE))
            .build();

        assertThat(c2UploadedEmailContentProvider.buildC2UploadPbaPaymentNotTakenNotification(createCase()))
            .isEqualTo(expectedMap);
    }

    private static CaseDetails createCase() {
        return CaseDetails.builder()
            .id(Long.valueOf(CASE_REFERENCE))
            .build();
    }
}