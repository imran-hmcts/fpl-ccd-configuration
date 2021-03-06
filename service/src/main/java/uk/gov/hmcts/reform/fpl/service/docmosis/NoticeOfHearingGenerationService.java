package uk.gov.hmcts.reform.fpl.service.docmosis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.config.HmctsCourtLookupConfiguration;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingVenue;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisHearingBooking;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisNoticeOfHearing;
import uk.gov.hmcts.reform.fpl.service.CaseDataExtractionService;
import uk.gov.hmcts.reform.fpl.service.HearingVenueLookUpService;

import java.time.LocalDate;

import static uk.gov.hmcts.reform.fpl.enums.DocmosisImages.COURT_SEAL;
import static uk.gov.hmcts.reform.fpl.enums.DocmosisImages.CREST;
import static uk.gov.hmcts.reform.fpl.enums.HearingType.OTHER;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.DATE;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateToString;
import static uk.gov.hmcts.reform.fpl.utils.JudgeAndLegalAdvisorHelper.getSelectedJudge;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NoticeOfHearingGenerationService {
    private final CaseDataExtractionService dataService;
    private final HearingVenueLookUpService hearingVenueLookUpService;
    private final HmctsCourtLookupConfiguration hmctsCourtLookupConfiguration;

    public DocmosisNoticeOfHearing getTemplateData(CaseData caseData, HearingBooking hearingBooking) {
        HearingVenue venue = hearingVenueLookUpService.getHearingVenue(hearingBooking);

        String hearingVenue = venue.getAddress() != null
            ? hearingVenueLookUpService.buildHearingVenue(venue) : hearingBooking.getCustomPreviousVenue();

        JudgeAndLegalAdvisor judgeAndLegalAdvisor = getSelectedJudge(hearingBooking.getJudgeAndLegalAdvisor(),
            caseData.getAllocatedJudge());

        return DocmosisNoticeOfHearing.builder()
            .familyManCaseNumber(caseData.getFamilyManCaseNumber())
            .courtName(hmctsCourtLookupConfiguration.getCourt(caseData.getCaseLocalAuthority()).getName())
            .children(dataService.getChildrenDetails(caseData.getChildren1()))
            .hearingBooking(DocmosisHearingBooking.builder()
                .hearingDate(dataService.getHearingDateIfHearingsOnSameDay(hearingBooking).orElse(""))
                .hearingTime(dataService.getHearingTime(hearingBooking))
                .hearingType(getHearingType(hearingBooking))
                .hearingVenue(hearingVenue)
                .preHearingAttendance(dataService.extractPrehearingAttendance(hearingBooking))
                .build())
            .judgeAndLegalAdvisor(dataService.getJudgeAndLegalAdvisor(judgeAndLegalAdvisor))
            .postingDate(formatLocalDateToString(LocalDate.now(), DATE))
            .additionalNotes(hearingBooking.getAdditionalNotes())
            .courtseal(COURT_SEAL.getValue())
            .crest(CREST.getValue())
            .build();
    }

    private String getHearingType(HearingBooking hearingBooking) {
        return hearingBooking.getType() != OTHER ? hearingBooking.getType().getLabel().toLowerCase() :
            hearingBooking.getTypeDetails();
    }

}
