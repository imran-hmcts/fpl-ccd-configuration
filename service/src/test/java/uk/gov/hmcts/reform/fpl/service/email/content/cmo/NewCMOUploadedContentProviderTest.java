package uk.gov.hmcts.reform.fpl.service.email.content.cmo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.notify.cmo.CMOReadyToSealTemplate;
import uk.gov.hmcts.reform.fpl.service.email.content.AbstractEmailContentProviderTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.HearingType.CASE_MANAGEMENT;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;

@ContextConfiguration(classes = {NewCMOUploadedContentProvider.class})
class NewCMOUploadedContentProviderTest extends AbstractEmailContentProviderTest {

    @Autowired
    private NewCMOUploadedContentProvider contentProvider;

    private static final LocalDate SOME_DATE = LocalDate.of(2020, 2, 20);
    private static final Long CASE_NUMBER = 12345L;

    @Test
    void shouldCreateTemplateWithExpectedParameters() {
        List<Element<Respondent>> respondents = wrapElements(
            Respondent.builder()
                .party(RespondentParty.builder()
                    .lastName("Vlad")
                    .build())
                .build());
        String familyManCaseNumber = "123456";

        JudgeAndLegalAdvisor judge = JudgeAndLegalAdvisor.builder()
            .judgeTitle(JudgeOrMagistrateTitle.HER_HONOUR_JUDGE)
            .judgeLastName("Simmons")
            .build();

        HearingBooking hearing = HearingBooking.builder()
            .type(CASE_MANAGEMENT)
            .startDate(LocalDateTime.of(SOME_DATE, LocalTime.of(0, 0)))
            .judgeAndLegalAdvisor(judge)
            .build();

        CMOReadyToSealTemplate template = contentProvider.buildTemplate(hearing, CASE_NUMBER, judge,
            respondents, familyManCaseNumber);

        CMOReadyToSealTemplate expected = new CMOReadyToSealTemplate()
            .setJudgeName("Simmons")
            .setJudgeTitle("Her Honour Judge")
            .setRespondentLastName("Vlad")
            .setSubjectLineWithHearingDate("Vlad, 123456, Case management hearing, 20 February 2020")
            .setCaseUrl(caseUrl(CASE_NUMBER.toString()));

        assertThat(template).isEqualToComparingFieldByField(expected);
    }
}