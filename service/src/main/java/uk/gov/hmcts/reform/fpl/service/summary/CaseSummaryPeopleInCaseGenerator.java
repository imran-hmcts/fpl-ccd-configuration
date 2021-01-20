package uk.gov.hmcts.reform.fpl.service.summary;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.summary.SyntheticCaseSummary;

import java.util.Map;

import static uk.gov.hmcts.reform.fpl.utils.PeopleInCaseHelper.getFirstRespondentLastName;

@Component
public class CaseSummaryPeopleInCaseGenerator implements CaseSummaryFieldsGenerator {
    @Override
    public SyntheticCaseSummary generate(CaseData caseData) {
        return SyntheticCaseSummary.builder()
            .caseSummaryNumberOfChildren(caseData.getChildren1().size())
            .caseSummaryLASolicitorName(caseData.getSolicitor().getName())
            .caseSummaryLASolicitorEmail(caseData.getSolicitor().getEmail())
            .caseSummaryFirstRespondentLastName(getFirstRespondentLastName(caseData.getRespondents1()))
            .caseSummaryFirstRespondentLegalRep("Soccorro LLC -TODO")
            //.caseSummaryCafcassGuardian(caseData.getAllProceedings()) // SOMEWHERE THERE
            .caseSummaryCafcassGuardian("MR cafcass TODO") // SOMEWHERE THERE
            .build();
    }
}
