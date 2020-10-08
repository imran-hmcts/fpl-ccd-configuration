package uk.gov.hmcts.reform.fpl.model.docmosis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocmosisNoticeOfProceeding implements DocmosisData {
    private final String courtName;
    private final String familyManCaseNumber;
    private final String todaysDate;
    private final String applicantName;
    private final String orderTypes;
    private final String childrenNames;
    @JsonUnwrapped
    private final DocmosisJudgeAndLegalAdvisor judgeAndLegalAdvisor;
    @JsonUnwrapped
    private final DocmosisTemplateImages templateImages;
    @JsonUnwrapped
    private DocmosisHearingBooking hearingBooking;
}
