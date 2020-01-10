package uk.gov.hmcts.reform.fpl.model;

import ccd.sdk.types.ComplexType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@ComplexType(name = "RiskAndHarm")
public class Risks {
    private final String neglect;
    private final String sexualAbuse;
    private final String physicalHarm;
    private final String emotionalHarm;
    private final List<String> neglectOccurrences;
    private final List<String> sexualAbuseOccurrences;
    private final List<String> physicalHarmOccurrences;
    private final List<String> emotionalHarmOccurrences;
}
