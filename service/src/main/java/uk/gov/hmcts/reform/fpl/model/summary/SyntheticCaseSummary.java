package uk.gov.hmcts.reform.fpl.model.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Jacksonized
public class SyntheticCaseSummary {
    String caseSummaryOrdersRequested;

    LocalDate timeline26WeekDate;

    String caseSummaryHasUnresolvedMessages;

    String caseSummaryHasNextHearing;
    String caseSummaryNextHearingType;
    LocalDate caseSummaryNextHearingDate;
    String caseSummaryNextHearingJudge;
    String caseSummaryNextHearingEmailAddress;
    String caseSummaryNextHearingCMO;

    String caseSummaryHasPreviousHearing;
    String caseSummaryPreviousHearingType;
    LocalDate caseSummaryPreviousHearingDate;
    String caseSummaryPreviousHearingCMO;


    String caseSummaryHasFinalHearing;
    LocalDate caseSummaryFinalHearingDate;

    String caseSummaryAllocatedJudgeName;
    String caseSummaryAllocatedJudgeEmail;


    Integer caseSummaryNumberOfChildren;
    String caseSummaryLASolicitorName;
    String caseSummaryLASolicitorEmail;
    String caseSummaryFirstRespondentLastName;
    String caseSummaryFirstRespondentLegalRep;
    String caseSummaryCafcassGuardian;


}
