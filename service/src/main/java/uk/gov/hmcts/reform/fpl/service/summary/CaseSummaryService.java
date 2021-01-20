package uk.gov.hmcts.reform.fpl.service.summary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseSummaryService {

    private final List<CaseSummaryFieldsGenerator> generators;
    private final ObjectMapper objectMapper;

    public CaseSummaryService(
        CaseSummaryOrdersRequestedGenerator caseSummaryOrdersRequestedGenerator,
        CaseSummaryDeadlineGenerator caseSummaryDeadlineGenerator,
        CaseSummaryJudgeInformationGenerator caseSummaryJudgeInformationGenerator,
        CaseSummaryMessagesGenerator caseSummaryMessagesGenerator,
        CaseSummaryNextHearingGenerator caseSummaryNextHearingGenerator,
        CaseSummaryPreviousHearingGenerator caseSummaryPreviousHearingGenerator,
        CaseSummaryFinalHearingGenerator caseSummaryFinalHearingGenerator,
        CaseSummaryPeopleInCaseGenerator caseSummaryPeopleInCaseGenerator,
        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.generators = List.of(
            caseSummaryOrdersRequestedGenerator,
            caseSummaryDeadlineGenerator,
            caseSummaryJudgeInformationGenerator,
            caseSummaryMessagesGenerator,
            caseSummaryNextHearingGenerator,
            caseSummaryPreviousHearingGenerator,
            caseSummaryFinalHearingGenerator,
            caseSummaryPeopleInCaseGenerator
        );
    }

    public Map<String, Object> generateSummaryFields(CaseData caseData,
                                                     CaseDetails caseDetails) {
        return generators.stream()
            .map(generator -> generator.generate(caseData))
            .flatMap(summary -> objectMapper.convertValue(summary, new TypeReference<Map<String, Object>>() {})
                .entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
