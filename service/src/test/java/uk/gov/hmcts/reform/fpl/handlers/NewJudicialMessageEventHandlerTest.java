package uk.gov.hmcts.reform.fpl.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fpl.events.NewJudicialMessageEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.JudicialMessageMetaData;
import uk.gov.hmcts.reform.fpl.model.event.MessageJudgeEventData;
import uk.gov.hmcts.reform.fpl.model.notify.NewJudicialMessageTemplate;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.NewJudicialMessageContentProvider;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.NEW_JUDICIAL_MESSAGE_ADDED_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.utils.CoreCaseDataStoreLoader.caseData;

@ExtendWith(MockitoExtension.class)
class NewJudicialMessageEventHandlerTest {
    @Mock
    private NotificationService notificationService;

    @Mock
    private NewJudicialMessageContentProvider newJudicialMessageContentProvider;

    @InjectMocks
    private NewJudicialMessageEventHandler newJudicialMessageEventHandler;

    @Test
    void shouldNotifyJudicialMessageRecipientWhenNewJudicialMessageCreated() {
        String recipient = "David@fpla.com";

        MessageJudgeEventData messageJudgeEventData = MessageJudgeEventData.builder()
            .judicialMessageMetaData(JudicialMessageMetaData.builder()
                .sender("Paul@fpla.com")
                .recipient(recipient)
                .build())
            .build();

        CaseData caseData = caseData().toBuilder().messageJudgeEventData(messageJudgeEventData).build();

        final NewJudicialMessageTemplate expectedParameters = NewJudicialMessageTemplate.builder().build();

        given(newJudicialMessageContentProvider.buildNewJudicialMessageTemplate(caseData))
            .willReturn(expectedParameters);

        newJudicialMessageEventHandler.notifyJudicialMessageRecipient(new NewJudicialMessageEvent(caseData));

        verify(notificationService).sendEmail(
            NEW_JUDICIAL_MESSAGE_ADDED_TEMPLATE,
            recipient,
            expectedParameters,
            caseData.getId());
    }
}
