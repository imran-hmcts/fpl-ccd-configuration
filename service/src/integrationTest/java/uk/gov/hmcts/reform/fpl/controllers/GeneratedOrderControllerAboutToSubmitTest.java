package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.enums.GeneratedEPOKey;
import uk.gov.hmcts.reform.fpl.enums.GeneratedOrderKey;
import uk.gov.hmcts.reform.fpl.enums.GeneratedOrderSubtype;
import uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType;
import uk.gov.hmcts.reform.fpl.enums.InterimOrderKey;
import uk.gov.hmcts.reform.fpl.enums.YesNo;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Judge;
import uk.gov.hmcts.reform.fpl.model.OrderTypeAndDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.order.generated.FurtherDirections;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.model.order.generated.InterimEndDate;
import uk.gov.hmcts.reform.fpl.service.DocmosisDocumentGeneratorService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderSubtype.FINAL;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderSubtype.INTERIM;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.BLANK_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.CARE_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType.SUPERVISION_ORDER;
import static uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle.HER_HONOUR_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle.HIS_HONOUR_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.NO;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.YES;
import static uk.gov.hmcts.reform.fpl.enums.ccd.fixedlists.InterimEndDateType.END_OF_PROCEEDINGS;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateTimeBaseUsingFormat;
import static uk.gov.hmcts.reform.fpl.utils.DocumentManagementStoreLoader.document;

@ActiveProfiles("integration-test")
@WebMvcTest(GeneratedOrderController.class)
@OverrideAutoConfiguration(enabled = true)
public class GeneratedOrderControllerAboutToSubmitTest extends AbstractControllerTest {

    private static final String LOCAL_AUTHORITY_CODE = "example";
    private static final byte[] PDF = {1, 2, 3, 4, 5};
    private Document document;

    @MockBean
    private DocmosisDocumentGeneratorService docmosisDocumentGeneratorService;

    @MockBean
    private UploadDocumentService uploadDocumentService;

    GeneratedOrderControllerAboutToSubmitTest() {
        super("create-order");
    }

    @BeforeEach
    void setUp() {
        document = document();
        DocmosisDocument docmosisDocument = new DocmosisDocument("order.pdf", PDF);

        given(docmosisDocumentGeneratorService.generateDocmosisDocument(anyMap(), any())).willReturn(docmosisDocument);
        given(uploadDocumentService.uploadPDF(any(), any())).willReturn(document);
    }

    @Test
    void aboutToSubmitShouldAddC21OrderToCaseDataAndRemoveTemporaryCaseDataOrderFields() {
        JudgeAndLegalAdvisor judgeAndLegalAdvisor = buildJudgeAndLegalAdvisor(NO);

        final CaseDetails caseDetails = buildCaseDetails(
            commonCaseDetailsComponents(BLANK_ORDER, null, judgeAndLegalAdvisor)
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .order(GeneratedOrder.builder()
                .title("Example Order")
                .details("Example order details here - Lorem ipsum dolor sit amet, consectetur adipiscing elit")
                .build()));

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

        GeneratedOrder expectedC21Order = commonExpectedOrderComponents(BLANK_ORDER.getLabel())
            .title("Example Order")
            .details("Example order details here - Lorem ipsum dolor sit amet, consectetur adipiscing elit")
            .build();

        aboutToSubmitAssertions(callbackResponse.getData(), expectedC21Order);
    }

    @Test
    void aboutToSubmitShouldNotHaveDraftAppendedToFilename() {
        JudgeAndLegalAdvisor judgeAndLegalAdvisor = buildJudgeAndLegalAdvisor(NO);

        final CaseDetails caseDetails = buildCaseDetails(
            commonCaseDetailsComponents(CARE_ORDER, FINAL, judgeAndLegalAdvisor)
                .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
        );

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

        final CaseData caseData = mapper.convertValue(callbackResponse.getData(), CaseData.class);

        assertThat(caseData.getOrderCollection().get(0).getValue().getDocument()).isEqualTo(expectedDocument());
    }

    @ParameterizedTest
    @EnumSource(GeneratedOrderSubtype.class)
    void aboutToSubmitShouldAddCareOrderToCaseDataAndRemoveTemporaryCaseDataOrderFields(
        GeneratedOrderSubtype subtype) {
        JudgeAndLegalAdvisor judgeAndLegalAdvisor = buildJudgeAndLegalAdvisor(NO);

        final CaseDetails caseDetails = buildCaseDetails(
            commonCaseDetailsComponents(CARE_ORDER, subtype, judgeAndLegalAdvisor)
                .orderFurtherDirections(FurtherDirections.builder().directionsNeeded("No").build())
                .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
                .interimEndDate(InterimEndDate.builder().type(END_OF_PROCEEDINGS).build())
        );

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

        final String expiryDate = subtype == INTERIM ? "End of the proceedings" : null;
        GeneratedOrder expectedCareOrder = commonExpectedOrderComponents(
            subtype.getLabel() + " " + "care order").expiryDate(expiryDate).build();

        aboutToSubmitAssertions(callbackResponse.getData(), expectedCareOrder);
    }

    @Test
    void aboutToSubmitShouldAddInterimSupervisionOrderToCaseDataAndRemoveTemporaryCaseDataOrderFields() {
        JudgeAndLegalAdvisor judgeAndLegalAdvisor = buildJudgeAndLegalAdvisor(NO);

        final CaseDetails caseDetails = buildCaseDetails(
            commonCaseDetailsComponents(SUPERVISION_ORDER, INTERIM, judgeAndLegalAdvisor)
            .orderFurtherDirections(FurtherDirections.builder().directionsNeeded("No").build())
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .interimEndDate(InterimEndDate.builder().type(END_OF_PROCEEDINGS).build())
        );

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

        GeneratedOrder expectedSupervisionOrder = commonExpectedOrderComponents(
            "Interim supervision order").expiryDate("End of the proceedings").build();

        aboutToSubmitAssertions(callbackResponse.getData(), expectedSupervisionOrder);
    }

    @Test
    void aboutToSubmitShouldAddFinalSupervisionOrderToCaseDataAndRemoveTemporaryCaseDataOrderFields() {
        JudgeAndLegalAdvisor judgeAndLegalAdvisor = buildJudgeAndLegalAdvisor(NO);

        final CaseDetails caseDetails = buildCaseDetails(
            commonCaseDetailsComponents(SUPERVISION_ORDER, FINAL, judgeAndLegalAdvisor)
            .orderFurtherDirections(FurtherDirections.builder().directionsNeeded("No").build())
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .orderMonths(14));

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

        LocalDateTime orderExpiration = now().plusMonths(14);
        GeneratedOrder expectedSupervisionOrder = commonExpectedOrderComponents(
            "Final supervision order")
            .expiryDate(
                formatLocalDateTimeBaseUsingFormat(orderExpiration, "h:mma, d MMMM y"))
            .build();

        aboutToSubmitAssertions(callbackResponse.getData(), expectedSupervisionOrder);
    }

    @Test
    void aboutToSubmitShouldMigrateJudgeAndLegalAdvisorWhenUsingAllocatedJudge() {
        JudgeAndLegalAdvisor judgeAndLegalAdvisor = buildJudgeAndLegalAdvisor(YES);

        final CaseDetails caseDetails = buildCaseDetails(
            commonCaseDetailsComponents(SUPERVISION_ORDER, FINAL, judgeAndLegalAdvisor)
                .orderFurtherDirections(FurtherDirections.builder().directionsNeeded("No").build())
                .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
                .orderMonths(14)
                .allocatedJudge(Judge.builder()
                    .judgeTitle(HIS_HONOUR_JUDGE)
                    .judgeLastName("Robinson")
                    .build()));

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);
        CaseData caseData = mapper.convertValue(callbackResponse.getData(), CaseData.class);

        GeneratedOrder generatedOrder = caseData.getOrderCollection().get(0).getValue();
        JudgeAndLegalAdvisor migratedJudge = generatedOrder.getJudgeAndLegalAdvisor();

        assertThat(migratedJudge.getJudgeTitle()).isEqualTo(HIS_HONOUR_JUDGE);
        assertThat(migratedJudge.getJudgeLastName()).isEqualTo("Robinson");
        assertThat(migratedJudge.getLegalAdvisorName()).isEqualTo("Peter Parker");
    }


    private JudgeAndLegalAdvisor buildJudgeAndLegalAdvisor(YesNo useAllocatedJudge) {
        return JudgeAndLegalAdvisor.builder()
            .judgeTitle(HER_HONOUR_JUDGE)
            .judgeLastName("Judy")
            .useAllocatedJudge(useAllocatedJudge.getValue())
            .legalAdvisorName("Peter Parker")
            .build();
    }

    private CaseDetails buildCaseDetails(CaseData.CaseDataBuilder builder) {
        return CaseDetails.builder()
            .data(mapper.convertValue(builder.build(), new TypeReference<>() {}))
            .build();
    }

    private CaseData.CaseDataBuilder commonCaseDetailsComponents(GeneratedOrderType orderType,
                                                                 GeneratedOrderSubtype subtype,
                                                                 JudgeAndLegalAdvisor judgeAndLegalAdvisor) {
        return CaseData.builder().orderTypeAndDocument(
            OrderTypeAndDocument.builder()
                .type(orderType)
                .subtype(subtype)
                .document(DocumentReference.builder().build())
                .build())
            .judgeAndLegalAdvisor(judgeAndLegalAdvisor)
            .familyManCaseNumber("12345L")
            .dateOfIssue(dateNow());
    }

    private GeneratedOrder.GeneratedOrderBuilder commonExpectedOrderComponents(String fullType) {
        return GeneratedOrder.builder()
            .type(fullType)
            .dateOfIssue(formatLocalDateTimeBaseUsingFormat(now(), "d MMMM yyyy"))
            .document(expectedDocument())
            .date(formatLocalDateTimeBaseUsingFormat(now(), "h:mma, d MMMM yyyy"))
            .judgeAndLegalAdvisor(
                JudgeAndLegalAdvisor.builder()
                    .judgeTitle(HER_HONOUR_JUDGE)
                    .judgeLastName("Judy")
                    .legalAdvisorName("Peter Parker")
                    .build()
            );
    }

    private DocumentReference expectedDocument() {
        return DocumentReference.builder()
            .binaryUrl(document.links.binary.href)
            .filename("file.pdf")
            .url(document.links.self.href)
            .build();
    }

    private void aboutToSubmitAssertions(Map<String, Object> data, GeneratedOrder expectedOrder) {
        List<String> keys = stream(GeneratedOrderKey.values()).map(GeneratedOrderKey::getKey).collect(toList());
        keys.addAll(stream(GeneratedEPOKey.values()).map(GeneratedEPOKey::getKey).collect(toList()));
        keys.addAll(stream(InterimOrderKey.values()).map(InterimOrderKey::getKey).collect(toList()));

        assertThat(data).doesNotContainKeys(keys.toArray(String[]::new));

        List<Element<GeneratedOrder>> orders = mapper.convertValue(data.get("orderCollection"),
            new TypeReference<>() {});

        assertThat(orders.get(0).getValue()).isEqualTo(expectedOrder);
    }
}