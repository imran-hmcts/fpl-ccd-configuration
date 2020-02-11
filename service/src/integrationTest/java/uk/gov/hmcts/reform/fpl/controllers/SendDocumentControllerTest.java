package uk.gov.hmcts.reform.fpl.controllers;

import com.launchdarkly.client.LDClient;
import com.launchdarkly.client.LDUser;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeRole;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.service.DocmosisCoverDocumentsService;
import uk.gov.hmcts.reform.fpl.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.EMAIL;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.POST;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocument;

@ActiveProfiles("integration-test")
@WebMvcTest(SendDocumentController.class)
@OverrideAutoConfiguration(enabled = true)
class SendDocumentControllerTest extends AbstractControllerTest {

    private static final byte[] PDF = {1, 2, 3, 4, 5};
    private final String serviceAuthToken = RandomStringUtils.randomAlphanumeric(10);

    @MockBean
    private DocmosisCoverDocumentsService docmosisCoverDocumentsService;

    @MockBean
    private DocumentDownloadService documentDownloadService;

    @MockBean
    private LDClient ldClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private SendLetterApi sendLetterApi;

    SendDocumentControllerTest() {
        super("send-document");
    }

    @BeforeEach
    void setupStoppedClock() {
        given(ldClient.boolVariation(anyString(), any(LDUser.class), anyBoolean())).willReturn(true);
        given(documentDownloadService.downloadDocument(anyString())).willReturn(PDF);
        given(authTokenGenerator.generate()).willReturn(serviceAuthToken);
        given(docmosisCoverDocumentsService.createCoverDocuments(anyString(), anyLong(), any())).willReturn(
            DocmosisDocument.builder().bytes(PDF).build());
    }

    @Test
    void shouldSendDocumentToRepresentativesWithPostServingPreferences() {
        Representative representative1 = representative("John Smith", POST);
        Representative representative2 = representative("Alex Brown", EMAIL);
        Representative representative3 = representative("Emma White", DIGITAL_SERVICE);

        DocumentReference documentToBeSend = testDocument();

        CaseDetails caseDetails = buildCaseData(documentToBeSend, representative1, representative2, representative3);

        postAboutToSubmitEvent(caseDetails);

        verify(documentDownloadService).downloadDocument(documentToBeSend.getBinaryUrl());
        verify(sendLetterApi).sendLetter(eq(serviceAuthToken), any(LetterWithPdfsRequest.class));
    }

    @Test
    void shouldNotSendDocumentWhenFeatureToggleIsOff() {
        given(ldClient.boolVariation(anyString(), any(LDUser.class), anyBoolean())).willReturn(false);
        DocumentReference documentToBeSend = testDocument();
        CaseDetails caseDetails = buildCaseData(documentToBeSend);

        postAboutToSubmitEvent(caseDetails);

        verify(documentDownloadService, never()).downloadDocument(documentToBeSend.getBinaryUrl());
        verify(sendLetterApi, never()).sendLetter(eq(serviceAuthToken), any(LetterWithPdfsRequest.class));
    }

    private static CaseDetails buildCaseData(DocumentReference documentReference, Representative... representatives) {
        return CaseDetails.builder()
            .id(1234567890123456L)
            .data(Map.of(
                "familyManCaseNumber", "number",
                "documentToBeSent", documentReference,
                "representatives", wrapElements(representatives)))
            .build();
    }

    private static Representative representative(String name, RepresentativeServingPreferences servingPreferences) {
        return Representative.builder()
            .fullName(name)
            .positionInACase("Position")
            .role(RepresentativeRole.REPRESENTING_PERSON_1)
            .servingPreferences(servingPreferences)
            .address(Address.builder().build())
            .build();
    }

}
