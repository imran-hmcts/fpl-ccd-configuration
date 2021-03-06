package uk.gov.hmcts.reform.fpl.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.DocumentStatus.ATTACHED;
import static uk.gov.hmcts.reform.fpl.enums.DocumentStatus.INCLUDED_IN_SWET;
import static uk.gov.hmcts.reform.fpl.utils.CoreCaseDataStoreLoader.callbackRequest;

@ActiveProfiles("integration-test")
@WebMvcTest(UploadDocumentsController.class)
@OverrideAutoConfiguration(enabled = true)
class UploadDocumentsControllerTest extends AbstractControllerTest {

    @MockBean
    private UploadDocumentsService uploadDocumentsService;

    UploadDocumentsControllerTest() {
        super("upload-documents");
    }

    @Test
    void shouldNotReturnErrorsWhenUploadedDocumentsAreValid() {
        CaseDetails caseDetails = callbackRequest().getCaseDetails();

        AboutToStartOrSubmitCallbackResponse callbackResponse = postMidEvent(caseDetails);

        assertThat(callbackResponse.getErrors()).isEmpty();
    }

    @Test
    void shouldReturnErrorsWhenUploadedDocumentsAreNotValid() {
        CaseDetails caseDetails = createCaseDetails();
        AboutToStartOrSubmitCallbackResponse callbackResponse = postMidEvent(caseDetails);

        assertThat(callbackResponse.getErrors()).containsExactly(
            "You must give additional document 1 a name.",
            "You must upload a file for additional document 1.",
            "Check document 1. Attach the document or change the status from 'Attached'.",
            "Check document 3. Attach the SWET or change the status from 'Included in SWET'.",
            "Check document 5. Attach the document or change the status from 'Attached'.");
    }

    private CaseDetails createCaseDetails() {
        return CaseDetails.builder()
            .data(Map.of(
                "documents_socialWorkChronology_document", Map.of(
                    "documentStatus", ATTACHED.getLabel()
                ),
                "documents_socialWorkAssessment_document", Map.of(
                    "documentStatus", INCLUDED_IN_SWET.getLabel()
                ),
                "documents_socialWorkEvidenceTemplate_document", Map.of(
                    "documentStatus", ATTACHED.getLabel()
                ),
                "documents_socialWorkOther", List.of(
                    Map.of(
                        "id", UUID.randomUUID(),
                        "value", Map.of("documentTitle", "")
                    ))))
            .build();
    }

}
