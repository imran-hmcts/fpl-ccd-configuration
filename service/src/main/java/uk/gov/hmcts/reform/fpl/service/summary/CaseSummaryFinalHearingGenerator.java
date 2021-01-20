package uk.gov.hmcts.reform.fpl.service.summary;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.summary.SyntheticCaseSummary;
import uk.gov.hmcts.reform.fpl.service.time.Time;

import java.util.Map;

@Component
public class CaseSummaryFinalHearingGenerator implements CaseSummaryFieldsGenerator {

    private final Time time;

    public CaseSummaryFinalHearingGenerator(Time time) {
        this.time = time;
    }

    @Override
    public SyntheticCaseSummary generate(CaseData caseData) {
        return SyntheticCaseSummary.builder()
            .caseSummaryHasFinalHearing("Yes")
            .caseSummaryFinalHearingDate(time.now().toLocalDate())
            .build();
    }
}
