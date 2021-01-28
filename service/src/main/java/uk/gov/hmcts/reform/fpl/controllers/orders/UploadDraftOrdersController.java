package uk.gov.hmcts.reform.fpl.controllers.orders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.controllers.CallbackController;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingFurtherEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.event.UploadDraftOrdersData;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrder;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrdersBundle;
import uk.gov.hmcts.reform.fpl.service.cmo.DraftOrderService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.reform.fpl.utils.CaseDetailsHelper.removeTemporaryFields;

@Api
@RestController
@RequestMapping("/callback/upload-draft-orders")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class UploadDraftOrdersController extends CallbackController {

    private static final int MAX_ORDERS = 10;
    private final DraftOrderService service;
    private final ObjectMapper mapper;

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        UploadDraftOrdersData pageData = service.getInitialData(caseData);

        caseDetails.getData().putAll(mapper.convertValue(pageData, new TypeReference<>() {
        }));

        return respond(caseDetails);
    }

    @PostMapping("/populate-drafts-info/mid-event")
    public CallbackResponse handlePopulateDraftInfo(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        caseDetails.getData().putAll(mapper.convertValue(service.getDraftsInfo(caseData), new TypeReference<>() {
        }));

        return respond(caseDetails);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        UploadDraftOrdersData eventData = caseData.getUploadDraftOrdersEventData();

        if (isNotEmpty(eventData.getCurrentHearingOrderDrafts())
            && eventData.getCurrentHearingOrderDrafts().size() > MAX_ORDERS) {
            return respond(caseDetails, List.of(String.format("Maximum number of draft orders is %s", MAX_ORDERS)));
        }

        List<Element<HearingOrder>> unsealedCMOs = caseData.getDraftUploadedCMOs();
        List<Element<HearingBooking>> hearings = defaultIfNull(caseData.getHearingDetails(), new ArrayList<>());
        List<Element<HearingFurtherEvidenceBundle>> evidenceDocuments = caseData.getHearingFurtherEvidenceDocuments();
        List<Element<HearingOrdersBundle>> bundles = service.migrateCmoDraftToOrdersBundles(caseData);

        UUID hearingId = service.updateCase(eventData, hearings, unsealedCMOs, evidenceDocuments, bundles);

        // update case data
        caseDetails.getData().put("draftUploadedCMOs", unsealedCMOs);
        caseDetails.getData().put("hearingDetails", hearings);
        caseDetails.getData().put("hearingFurtherEvidenceDocuments", evidenceDocuments);
        caseDetails.getData().put("hearingOrdersBundlesDrafts", bundles);
        caseDetails.getData().put("lastHearingOrderDraftsHearingId", hearingId);

        // remove transient fields
        removeTemporaryFields(caseDetails, UploadDraftOrdersData.transientFields());

        return respond(caseDetails);
    }

    @PostMapping("/submitted")
    public void handleSubmitted(@RequestBody CallbackRequest request) {
        CaseData caseDataBefore = getCaseDataBefore(request);
        CaseData caseData = getCaseData(request);

        publishEvent(service.buildEventToPublish(caseData, caseDataBefore));
    }
}