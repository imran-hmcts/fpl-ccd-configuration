package uk.gov.hmcts.reform.fpl.service.summary;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.summary.SyntheticCaseSummary;
import uk.gov.hmcts.reform.fpl.service.time.Time;

import java.util.Map;

@Component
public class CaseSummaryDeadlineGenerator implements CaseSummaryFieldsGenerator {

    @Override
    public SyntheticCaseSummary generate(CaseData caseData) {
        return SyntheticCaseSummary.builder()
            .timeline26WeekDate(caseData.getDateSubmitted().plusWeeks(26))
            .build();
    }
}
