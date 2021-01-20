package uk.gov.hmcts.reform.fpl.service.summary;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.summary.SyntheticCaseSummary;
import uk.gov.hmcts.reform.fpl.service.time.Time;

import java.util.Map;

@Component
public class CaseSummaryPreviousHearingGenerator implements CaseSummaryFieldsGenerator {

    private final Time time;

    public CaseSummaryPreviousHearingGenerator(Time time) {
        this.time = time;
    }

    @Override
    public SyntheticCaseSummary generate(CaseData caseData) {

        return SyntheticCaseSummary.builder()
            .caseSummaryHasPreviousHearing("Yes")
            .caseSummaryPreviousHearingType("HearingType")
            .caseSummaryPreviousHearingDate(time.now().toLocalDate())
            .caseSummaryPreviousHearingCMO("Put a file link here")
            .build();
    }
}
