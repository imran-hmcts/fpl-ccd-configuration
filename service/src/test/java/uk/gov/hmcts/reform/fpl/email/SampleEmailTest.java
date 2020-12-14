package uk.gov.hmcts.reform.fpl.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.reform.fpl.model.notify.TestNotifyData;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.utils.captor.ResultsCaptor;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class SampleEmailTest extends EmailTemplateTest {

    private static final String TEST_TEMPLATE_ID = "b7beba34-95e8-4619-9476-d16d60c9706b";

    @Autowired
    NotificationService underTest;

    @SpyBean
    NotificationClient client;

    @Test
    void testEmail() throws NotificationClientException {
        ResultsCaptor<SendEmailResponse> resultsCaptor = new ResultsCaptor<>();

        doAnswer(resultsCaptor).when(client).sendEmail(any(), any(), any(), any());

        underTest.sendEmail(
            TEST_TEMPLATE_ID,
            "test@example.com",
            TestNotifyData.builder()
                .fieldB("microsoft-edge:https://www.google.com")
                .fieldA("https://www.google.com")
                .build(),
            "testCaseID" + UUID.randomUUID());

        SendEmailResponse response = resultsCaptor.getResult();

        assertThat(response.getBody()).isEqualTo(line("# This is a title")
            + line()
            + line("Apply now (normal link) at https://www.google.com ")
            + line("Apply now (edge link) at microsoft-edge:https://www.google.com ")
            + line()
            + line("Thanks,")
            + "Test");
    }
}
