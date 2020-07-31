package uk.gov.hmcts.reform.fpl.controllers;

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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Other;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.service.ConfidentialDetailsService;
import uk.gov.hmcts.reform.fpl.service.OthersServiceTest;

import java.util.List;

@Api
@RestController
@RequestMapping("/callback/enter-others")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OthersController {
    private final ObjectMapper mapper;
    private final ConfidentialDetailsService confidentialService;
    private final OthersServiceTest othersServiceTest;

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest callbackrequest) {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);

//        List<Element<Other>> others = confidentialService.combineOtherDetails(caseData.getAllOthers(),
//            caseData.getConfidentialOthers());

        caseDetails.getData().put("others", othersServiceTest.prepareOthers(caseData));


//        caseDetails.getData().put("others", Others.from(others));

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = mapper.convertValue(caseDetails.getData(), CaseData.class);
        List<Element<Other>> allOthers = caseData.getAllOthers();

//        confidentialService.addConfidentialDetailsToCase(caseDetails, allOthers, OTHER);
//
//        List<Element<Other>> others = confidentialService.removeConfidentialDetails(allOthers);
//
//        caseDetails.getData().put("others", Others.from(others));

//        List<Element<Other>> confidentialOthers =
//            confidentialService.addPartyMarkedConfidentialToList(caseData.getAllOthers());
//
//        List<Element<Other>> confidentialOthersModified = othersService.retainConfidentialDetails(confidentialOthers);
//
//        confidentialService.addConfidentialDetailsToCaseDetails(caseDetails, confidentialOthersModified, OTHER);
//
//        caseDetails.getData().put("others", othersServiceTest.modifyHiddenValues(caseData.getAllOthers()));

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

}
