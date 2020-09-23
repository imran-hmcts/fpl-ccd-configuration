package uk.gov.hmcts.reform.fpl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.State;
import uk.gov.hmcts.reform.fpl.model.CaseData;

import java.util.Map;

import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CaseConverterTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CaseConverter caseConverter;

    @ParameterizedTest
    @EnumSource(State.class)
    void shouldConvertCaseDetailsToCaseDataAndSetIdAndState(State state) {
        final CaseDetails caseDetails = CaseDetails
            .builder()
            .data(Map.of())
            .id(nextLong())
            .state(state.getValue())
            .build();

        CaseData caseData = CaseData.builder()
            .caseName("test")
            .build();

        CaseData expectedCaseData = caseData.toBuilder()
            .id(caseDetails.getId())
            .state(state)
            .build();

        when(objectMapper.convertValue(anyMap(), eq(CaseData.class))).thenReturn(caseData);

        CaseData actualCaseData = caseConverter.convert(caseDetails);

        assertThat(actualCaseData).isEqualTo(expectedCaseData);

        verify(objectMapper).convertValue(caseDetails.getData(), CaseData.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "Unknown")
    void shouldConvertCaseDetailsToCaseDataAndDontSetStateIfNotRecognised(String state) {
        final CaseDetails caseDetails = CaseDetails
            .builder()
            .data(Map.of())
            .id(nextLong())
            .state(state)
            .build();

        CaseData caseData = CaseData.builder()
            .caseName("test")
            .build();

        CaseData expectedCaseData = caseData.toBuilder()
            .id(caseDetails.getId())
            .state(null)
            .build();

        when(objectMapper.convertValue(anyMap(), eq(CaseData.class))).thenReturn(caseData);

        CaseData actualCaseData = caseConverter.convert(caseDetails);

        assertThat(actualCaseData).isEqualTo(expectedCaseData);

        verify(objectMapper).convertValue(caseDetails.getData(), CaseData.class);
    }

    @Test
    void shouldReturnNullIfCaseDataIsNull() {
        CaseData actualCaseData = caseConverter.convert(null);

        assertThat(actualCaseData).isNull();
    }

}
