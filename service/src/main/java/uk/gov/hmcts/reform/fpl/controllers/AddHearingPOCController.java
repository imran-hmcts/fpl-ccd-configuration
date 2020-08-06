package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.enums.ProceedingType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingPreferences;
import uk.gov.hmcts.reform.fpl.model.Judge;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisNoticeOfHearing;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisNoticeOfProceeding;
import uk.gov.hmcts.reform.fpl.service.HearingBookingService;
import uk.gov.hmcts.reform.fpl.service.NoticeOfProceedingsTemplateDataGenerationService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.ValidateGroupService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisDocumentGeneratorService;
import uk.gov.hmcts.reform.fpl.service.docmosis.NoticeOfHearingGenerationService;
import uk.gov.hmcts.reform.fpl.utils.ElementUtils;
import uk.gov.hmcts.reform.fpl.validation.groups.HearingDatesGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates.NOTICE_OF_HEARING;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.NO;
import static uk.gov.hmcts.reform.fpl.service.HearingBookingService.HEARING_DETAILS_KEY;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.DATE;
import static uk.gov.hmcts.reform.fpl.utils.JudgeAndLegalAdvisorHelper.buildAllocatedJudgeLabel;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.YES;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.reform.fpl.enums.HearingOptionsPOCType.EDIT_DRAFT;
import static uk.gov.hmcts.reform.fpl.enums.HearingOptionsPOCType.EDIT_ADJOURNED;

@Api
@RestController
@RequestMapping("/callback/add-hearing-poc")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AddHearingPOCController {
    private final ObjectMapper mapper;
    private final ValidateGroupService validateGroupService;
    private final NoticeOfHearingGenerationService noticeOfHearingGenerationService;
    private final DocmosisDocumentGeneratorService docmosisDocumentGeneratorService;
    private final UploadDocumentService uploadDocumentService;
    private final NoticeOfProceedingsTemplateDataGenerationService noticeOfProceedingsTemplateDataGenerationService;
    private final HearingBookingService hearingBookingService;

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = mapper.convertValue(callbackRequest.getCaseDetails(), CaseDetails.class);
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);
        Map<String, Object> data = caseDetails.getData();

        if (caseData.getAllocatedJudge() != null) {
            data.put("judgeAndLegalAdvisor", setAllocatedJudgeLabel(caseData.getAllocatedJudge()));
        }

        if (hasExistingHearingBookings(caseData.getHearingDetails())) {
            data.put("hasExistingHearings", YES.getValue());
            data.put("hearingDateList", buildDraftHearingDateList(caseData.getHearingDetails()));

            if (!getAdjournedHearings(caseData.getHearingDetails()).isEmpty()) {
                data.put("hasAdjournedHearings", YES.getValue());
                data.put("adjournedHearingDateList", buildAdjournedHearingDateList(caseData.getHearingDetails()));
            }
        }

        if (caseData.getHearingDetails() == null) {
            data.put("isFirstHearing", YES.getValue());
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    @PostMapping("/populate-existing-hearings/mid-event")
    public AboutToStartOrSubmitCallbackResponse populateExistingDraftHearing
        (@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        // Editing a draft hearing
        if (EDIT_DRAFT.equals(caseData.getUseExistingHearing())) {
            UUID hearingBookingId = mapper.convertValue(caseDetails.getData().get("hearingDateList"), UUID.class);

            caseDetails.getData().put("hearingDateList",
                ElementUtils.asDynamicList(caseData.getHearingDetails(),
                    hearingBookingId, hearingBooking -> hearingBooking.toLabel(DATE)));

            HearingBooking hearingBooking = findHearingBooking(hearingBookingId, caseData.getHearingDetails());

            Optional<Element<HearingBooking>> firstHearingElement =
                hearingBookingService.getFirstHearingElement(caseData.getHearingDetails());

            if (firstHearingElement.isPresent() && firstHearingElement.get().getId().equals(hearingBookingId)) {
                caseDetails.getData().put("isFirstHearing", YES.getValue());
                caseDetails.getData().put("sendNoticeOfHearing", NO.getValue());
            } else {
                caseDetails.getData().remove("isFirstHearing");
            }

            caseDetails.getData().remove("adjournedHearingDateList");

            populateHearingBooking(caseDetails, hearingBooking);
        }
        // Editing an adjourned hearing
        else if (EDIT_ADJOURNED.equals(caseData.getUseExistingHearing())) {
            UUID hearingBookingId = mapper.convertValue(caseDetails.getData().get("adjournedHearingDateList"), UUID.class);

            caseDetails.getData().put("adjournedHearingDateList",
                ElementUtils.asDynamicList(caseData.getHearingDetails(),
                    hearingBookingId, hearingBooking -> hearingBooking.toLabel(DATE)));

            HearingBooking hearingBooking = findHearingBooking(hearingBookingId, caseData.getHearingDetails());

            Optional<Element<HearingBooking>> firstHearingElement =
                hearingBookingService.getFirstHearingElement(caseData.getHearingDetails());

            if (firstHearingElement.isPresent() && firstHearingElement.get().getId().equals(hearingBookingId)) {
                caseDetails.getData().put("isFirstHearing", YES.getValue());
                caseDetails.getData().put("sendNoticeOfHearing", NO.getValue());
            } else {
                caseDetails.getData().remove("isFirstHearing");
            }

            caseDetails.getData().remove("hearingDateList");

            populateHearingBooking(caseDetails, hearingBooking);
        }
        
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    @PostMapping("/validate-hearing-dates/mid-event")
    public AboutToStartOrSubmitCallbackResponse validateHearingDatesMidEvent(
        @RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = mapper.convertValue(callbackRequest.getCaseDetails(), CaseDetails.class);
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        if (caseData.getHearingPreferences() != null) {
            caseDetails.getData().put("hearingNeedsLabel",
                buildHearingPreferencesLabel(caseData.getHearingPreferences()));
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .errors(validateGroupService.validateGroup(caseData, HearingDatesGroup.class))
            .build();
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = mapper.convertValue(callbackRequest.getCaseDetails(), CaseDetails.class);
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

        HearingBooking hearingBooking = buildHearingBooking(caseData);

        if (caseData.getSendNoticeOfHearing() != null
            && isSendingNoticeOfHearing(caseData.getSendNoticeOfHearing())) {
            DocmosisNoticeOfHearing dnof = noticeOfHearingGenerationService.getTemplateData(caseData, hearingBooking);
            DocmosisDocument docmosisDocument = docmosisDocumentGeneratorService.generateDocmosisDocument(dnof,
                NOTICE_OF_HEARING);
            Document document = uploadDocumentService.uploadPDF(docmosisDocument.getBytes(),
                NOTICE_OF_HEARING.getDocumentTitle(now()));

            hearingBooking.setNoticeOfHearing(DocumentReference.buildFromDocument(document));
        }

        List<Element<HearingBooking>> hearingBookingElements;

        // Editing previous hearing
        if ((caseData.getUseExistingHearing() != null) && EDIT_DRAFT.equals(caseData.getUseExistingHearing())
            || EDIT_ADJOURNED.equals(caseData.getUseExistingHearing())) {

            DynamicList hearingList;

            if (EDIT_DRAFT.equals(caseData.getUseExistingHearing())) {
                hearingList = mapper.convertValue(caseDetails.getData().get("hearingDateList"), DynamicList.class);
            } else {
                hearingList =
                    mapper.convertValue(caseDetails.getData().get("adjournedHearingDateList"), DynamicList.class);
            }

            UUID editedHearingId = hearingList.getValueCode();

            hearingBookingElements = caseData.getHearingDetails().stream()
                .map(hearingBookingElement -> {
                    if (hearingBookingElement.getId().equals(editedHearingId)) {
                        hearingBookingElement = Element.<HearingBooking>builder()
                            .id(hearingBookingElement.getId())
                            .value(hearingBooking)
                            .build();
                    }
                    return hearingBookingElement;
                }).collect(Collectors.toList());
        } else {
            hearingBookingElements = appendHearingBooking(caseData.getHearingDetails(), hearingBooking);
        }

        caseDetails.getData().put(HEARING_DETAILS_KEY, hearingBookingElements);

        if (caseData.getProceedingType() != null
            && isSendingNoticeOfProceeding(caseData.getProceedingType())) {

            CaseData updatedCaseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

            DocmosisNoticeOfProceeding templateData = noticeOfProceedingsTemplateDataGenerationService
                .getTemplateData(updatedCaseData);

            List<DocmosisTemplates> templateTypes = getProceedingTemplateTypes(caseData.getProceedingType());

            List<Document> uploadedDocuments = generateAndUploadDocuments(templateData, templateTypes);

            List<Element<DocumentBundle>> noticeOfProceedingCaseData =
                createNoticeOfProceedingsCaseData(uploadedDocuments);

            caseDetails.getData().put("noticeOfProceedingsBundle", noticeOfProceedingCaseData);
        }

        removeHearingProperties(caseDetails);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    private HearingBooking findHearingBooking(UUID id, List<Element<HearingBooking>> hearingBookings) {
        Optional<Element<HearingBooking>> hearingBookingElement = ElementUtils.findElement(id, hearingBookings);

        if (hearingBookingElement.isPresent()) {
            return hearingBookingElement.get().getValue();
        }

        return HearingBooking.builder().build();
    }

    private DynamicList buildDraftHearingDateList(List<Element<HearingBooking>> hearingBookings) {
        List<DynamicListElement> dynamicListElements = new ArrayList<>();

        for (int i = 0; i < hearingBookings.size(); i++) {
            HearingBooking hearingBooking = hearingBookings.get(i).getValue();

            if (isNull(hearingBooking.getIsAdjourned())) {

                DynamicListElement dynamicListElement = DynamicListElement.builder()
                    .label(hearingBooking.toLabel(DATE))
                    .code(hearingBookings.get(i).getId())
                    .build();

                dynamicListElements.add(dynamicListElement);
            }
        }

        if (dynamicListElements.isEmpty()) {
            return null;
        }

        return DynamicList.builder()
            .listItems(dynamicListElements)
            .value(dynamicListElements.get(0))
            .build();
    }

    private DynamicList buildAdjournedHearingDateList(List<Element<HearingBooking>> hearingBookings) {
        List<DynamicListElement> dynamicListElements = new ArrayList<>();

        for (int i = 0; i < hearingBookings.size(); i++) {
            HearingBooking hearingBooking = hearingBookings.get(i).getValue();

            if(YES.getValue().equals(hearingBooking.getIsAdjourned())) {
                DynamicListElement dynamicListElement = DynamicListElement.builder()
                    .label(hearingBooking.toLabel(DATE))
                    .code(hearingBookings.get(i).getId())
                    .build();

                dynamicListElements.add(dynamicListElement);
            }
        }

        if (dynamicListElements.isEmpty()) {
            dynamicListElements.add(DynamicListElement.builder().build());
        }

        return DynamicList.builder()
            .listItems(dynamicListElements)
            .value(dynamicListElements.get(0))
            .build();
    }

    private List<Element<HearingBooking>> appendHearingBooking(List<Element<HearingBooking>> currentHearingBookings,
                                                               HearingBooking hearingBooking) {
        Element<HearingBooking> hearingBookingElement = Element.<HearingBooking>builder()
            .id(UUID.randomUUID())
            .value(hearingBooking)
            .build();

        if (!hasExistingHearingBookings(currentHearingBookings)) {
            return List.of(hearingBookingElement);
        }

        currentHearingBookings.add(hearingBookingElement);
        return currentHearingBookings;
    }

    private HearingBooking buildHearingBooking(CaseData caseData) {
        return HearingBooking.builder()
            .type(caseData.getHearingType())
            .venue(caseData.getHearingVenue())
            .venueCustomAddress(caseData.getHearingVenueCustom())
            .hearingNeedsBooked(caseData.getHearingNeedsBooked())
            .hearingNeedsDetails(caseData.getHearingNeedsDetails())
            .startDate(caseData.getHearingStartDate())
            .endDate(caseData.getHearingEndDate())
            .judgeAndLegalAdvisor(caseData.getJudgeAndLegalAdvisor())
            .build();
    }

    private JudgeAndLegalAdvisor setAllocatedJudgeLabel(Judge allocatedJudge) {
        String assignedJudgeLabel = buildAllocatedJudgeLabel(allocatedJudge);

        return JudgeAndLegalAdvisor.builder()
            .allocatedJudgeLabel(assignedJudgeLabel)
            .build();
    }

    private boolean isSendingNoticeOfHearing(String sendNoticeOfHearing) {
        return sendNoticeOfHearing.equals(YES.getValue());
    }

    private boolean isSendingNoticeOfProceeding(List<ProceedingType> proceedingTypes) {
        return !proceedingTypes.isEmpty();
    }

    private boolean hasExistingHearingBookings(List<Element<HearingBooking>> hearingBookings) {
        return isNotEmpty(hearingBookings);
    }

    private String buildHearingPreferencesLabel(HearingPreferences hearingPreferences) {
        StringBuilder stringBuilder = new StringBuilder();

        if (hearingPreferences.getInterpreter() == null && hearingPreferences.getWelsh() == null
            && hearingPreferences.getIntermediary() == null && hearingPreferences.getDisabilityAssistance() == null
            && hearingPreferences.getExtraSecurityMeasures() == null) {
            return stringBuilder.toString();
        } else {
            stringBuilder.append("Court services already requested").append("\n").append("\n");
        }

        if (hearingPreferences.getInterpreter() != null) {
            stringBuilder.append("• Interpreter").append("\n");
        }

        if (hearingPreferences.getWelsh() != null) {
            stringBuilder.append("• Spoken or written welsh").append("\n");
        }

        if (hearingPreferences.getIntermediary() != null) {
            stringBuilder.append("• Intermediary").append("\n");
        }

        if (hearingPreferences.getDisabilityAssistance() != null) {
            stringBuilder.append("• Facilities or assistance for a disability").append("\n");
        }

        if (hearingPreferences.getExtraSecurityMeasures() != null) {
            stringBuilder.append("• Separate waiting room or other security measures").append("\n");
        }

        return stringBuilder.toString();
    }

    private List<DocmosisTemplates> getProceedingTemplateTypes(List<ProceedingType> proceedingTypes) {
        ImmutableList.Builder<DocmosisTemplates> templateTypes = ImmutableList.builder();

        if (proceedingTypes
            .contains(ProceedingType.NOTICE_OF_PROCEEDINGS_FOR_PARTIES)) {
            templateTypes.add(DocmosisTemplates.C6);
        }

        if (proceedingTypes
            .contains(ProceedingType.NOTICE_OF_PROCEEDINGS_FOR_NON_PARTIES)) {
            templateTypes.add(DocmosisTemplates.C6A);
        }

        return templateTypes.build();
    }

    private List<Document> generateAndUploadDocuments(DocmosisNoticeOfProceeding templatePlaceholders,
                                                      List<DocmosisTemplates> templates) {
        List<DocmosisDocument> docmosisDocuments = templates.stream()
            .map(template -> docmosisDocumentGeneratorService.generateDocmosisDocument(templatePlaceholders, template))
            .collect(Collectors.toList());

        return docmosisDocuments.stream()
            .map(document -> uploadDocumentService.uploadPDF(document.getBytes(), document.getDocumentTitle()))
            .collect(Collectors.toList());
    }

    private List<Element<DocumentBundle>> createNoticeOfProceedingsCaseData(List<Document> uploadedDocuments) {
        return uploadedDocuments.stream()
            .map(document -> Element.<DocumentBundle>builder()
                .id(UUID.randomUUID())
                .value(DocumentBundle.builder()
                    .document(DocumentReference.builder()
                        .filename(document.originalDocumentName)
                        .url(document.links.self.href)
                        .binaryUrl(document.links.binary.href)
                        .build())
                    .build())
                .build())
            .collect(Collectors.toList());
    }

    private void populateHearingBooking(CaseDetails caseDetails, HearingBooking hearingBooking) {
        caseDetails.getData().put("hearingType", hearingBooking.getType());
        caseDetails.getData().put("hearingVenue", hearingBooking.getVenue());
        caseDetails.getData().put("hearingVenueCustom", hearingBooking.getVenueCustomAddress());
        caseDetails.getData().put("hearingNeedsBooked", hearingBooking.getHearingNeedsBooked());
        caseDetails.getData().put("hearingNeedsDetails", hearingBooking.getHearingNeedsDetails());
        caseDetails.getData().put("hearingStartDate", hearingBooking.getStartDate());
        caseDetails.getData().put("hearingEndDate", hearingBooking.getEndDate());
        caseDetails.getData().put("judgeAndLegalAdvisor", hearingBooking.getJudgeAndLegalAdvisor());
    }

    private void removeHearingProperties(CaseDetails caseDetails) {
        caseDetails.getData().remove("hearingType");
        caseDetails.getData().remove("hearingVenue");
        caseDetails.getData().remove("hearingVenueCustom");
        caseDetails.getData().remove("hearingNeedsBooked");
        caseDetails.getData().remove("hearingNeedsDetails");
        caseDetails.getData().remove("hearingStartDate");
        caseDetails.getData().remove("hearingEndDate");
        caseDetails.getData().remove("sendNoticeOfHearing");
        caseDetails.getData().remove("judgeAndLegalAdvisor");
        caseDetails.getData().remove("hasExistingHearings");
        caseDetails.getData().remove("hearingDateList");
        caseDetails.getData().remove("useExistingHearing");
        caseDetails.getData().remove("isFirstHearing");
        caseDetails.getData().remove("proceedingType");
        caseDetails.getData().remove("sendNoticeOfProceedings");
    }

    private List<Element<HearingBooking>> getAdjournedHearings(List<Element<HearingBooking>> hearingBookings) {
        return hearingBookings.stream()
            .filter(hearingBookingElement -> YES.getValue().equals(hearingBookingElement.getValue().getIsAdjourned()))
            .collect(Collectors.toList());
    }
}
