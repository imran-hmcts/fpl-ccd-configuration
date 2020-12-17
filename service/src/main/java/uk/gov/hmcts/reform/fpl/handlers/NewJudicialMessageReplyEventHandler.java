package uk.gov.hmcts.reform.fpl.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.events.NewJudicialMessageReplyEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.judicialmessage.JudicialMessage;
import uk.gov.hmcts.reform.fpl.model.notify.NewJudicialMessageReplyTemplate;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.JudicialMessageReplyContentProvider;

import static uk.gov.hmcts.reform.fpl.NotifyTemplates.JUDICIAL_MESSAGE_REPLY_TEMPLATE;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NewJudicialMessageReplyEventHandler {
    private final NotificationService notificationService;
    private final JudicialMessageReplyContentProvider newJudicialMessageReplyContentProvider;

    @EventListener
    public void notifyJudicialMessageRecipientOfReply(NewJudicialMessageReplyEvent event) {
        CaseData caseData = event.getCaseData();
        JudicialMessage newJudicialMessage = event.getJudicialMessage();

        NewJudicialMessageReplyTemplate notifyData =
            newJudicialMessageReplyContentProvider.buildNewJudicialMessageReplyTemplate(caseData, newJudicialMessage);

        notificationService.sendEmail(JUDICIAL_MESSAGE_REPLY_TEMPLATE, newJudicialMessage.getRecipient(),
            notifyData, caseData.getId());
    }
}
