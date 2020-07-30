package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.fpl.controllers.cmo.ReviewCMOController;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.ReviewDecision;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;
import uk.gov.hmcts.reform.fpl.model.order.CaseManagementOrder;
import uk.gov.hmcts.reform.fpl.utils.TestDataHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.CMOStatus.SEND_TO_JUDGE;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

@ActiveProfiles("integration-test")
@WebMvcTest(ReviewCMOController.class)
@OverrideAutoConfiguration(enabled = true)
class ReviewCMOControllerAboutToStartTest extends AbstractControllerTest {

    ReviewCMOControllerAboutToStartTest() {
        super("review-cmo");
    }

    @Test
    void shouldReturnCorrectDataWhenMultipleCMOsReadyForApproval() {
        DocumentReference order = TestDataHelper.testDocumentReference();
        List<Element<CaseManagementOrder>> draftCMOs = List.of(
            element(buildCMO("Test hearing 21st August 2020", order)),
            element(buildCMO("Test hearing 9th April 2021", order)));

        CaseData caseData = CaseData.builder().draftUploadedCMOs(draftCMOs).build();

        AboutToStartOrSubmitCallbackResponse response = postAboutToStartEvent(asCaseDetails(caseData));

        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.EMPTY)
            .listItems(draftCMOs.stream().map(cmo -> DynamicListElement.builder()
                .code(cmo.getId())
                .label(cmo.getValue().getHearing())
                .build())
                .collect(Collectors.toList()))
            .build();

        CaseData responseData = extractCaseData(response);

        assertThat(responseData.getNumDraftCMOs()).isEqualTo("MULTI");
        assertThat(responseData.getCmoToReviewList()).isEqualTo(
            mapper.convertValue(dynamicList, new TypeReference<Map<String, Object>>() {}));
    }

    @Test
    void shouldReturnCorrectDataWhenOneDraftCMOReadyForApproval() {
        ReviewDecision expectedDecision = ReviewDecision.builder()
            .hearing("Test hearing 21st August 2020")
            .document(TestDataHelper.testDocumentReference())
            .build();

        List<Element<CaseManagementOrder>> draftCMOs = List.of(
            element(buildCMO(expectedDecision.getHearing(), expectedDecision.getDocument())));

        CaseData caseData = CaseData.builder().draftUploadedCMOs(draftCMOs).build();

        AboutToStartOrSubmitCallbackResponse response = postAboutToStartEvent(caseData);

        CaseData responseData = mapper.convertValue(response.getData(), CaseData.class);

        assertThat(responseData.getNumDraftCMOs()).isEqualTo("SINGLE");
        assertThat(responseData.getReviewCMODecision()).isEqualTo(expectedDecision);
    }

    @Test
    void shouldReturnCorrectDataWhenNoDraftCMOsReadyForApproval() {
        CaseData caseData = CaseData.builder().draftUploadedCMOs(List.of()).build();

        CaseData updatedCaseData = extractCaseData(postAboutToStartEvent(asCaseDetails(caseData)));

        assertThat(updatedCaseData.getNumDraftCMOs()).isEqualTo("NONE");
    }

    private static CaseManagementOrder buildCMO(String hearing, DocumentReference order) {
        return CaseManagementOrder.builder()
            .hearing(hearing)
            .order(order)
            .status(SEND_TO_JUDGE).build();
    }
}