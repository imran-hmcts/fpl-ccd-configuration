package uk.gov.hmcts.reform.fpl.controllers;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseUserApi;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseUser;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityUserLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.SystemUpdateUserConfiguration;
import uk.gov.hmcts.reform.fpl.exceptions.GrantCaseAccessException;
import uk.gov.hmcts.reform.fpl.exceptions.UnknownLocalAuthorityCodeException;
import uk.gov.hmcts.reform.fpl.service.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.rd.client.OrganisationApi;
import uk.gov.hmcts.reform.rd.model.OrganisationUser;
import uk.gov.hmcts.reform.rd.model.OrganisationUsers;
import uk.gov.hmcts.reform.rd.model.Status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.fpl.CaseDefinitionConstants.CASE_TYPE;
import static uk.gov.hmcts.reform.fpl.CaseDefinitionConstants.JURISDICTION;
import static uk.gov.hmcts.reform.fpl.enums.CaseRole.CREATOR;
import static uk.gov.hmcts.reform.fpl.enums.CaseRole.LASOLICITOR;
import static uk.gov.hmcts.reform.fpl.utils.AssertionHelper.checkUntil;
import static uk.gov.hmcts.reform.fpl.utils.CoreCaseDataStoreLoader.callbackRequest;
import static uk.gov.hmcts.reform.fpl.utils.assertions.ExceptionAssertion.assertException;

@ActiveProfiles("integration-test")
@WebMvcTest(CaseInitiationController.class)
@OverrideAutoConfiguration(enabled = true)
class CaseInitiationControllerTest extends AbstractControllerTest {

    private static final String CALLER_ID = USER_ID;

    private static final String LA_1_CODE = "LA_1";
    private static final String LA_1_USER_1_ID = "LA_1-1";
    private static final String LA_1_USER_2_ID = "LA_1-2";
    private static final List<String> LA_1_USER_IDS = List.of(CALLER_ID, LA_1_USER_1_ID, LA_1_USER_2_ID);

    private static final String LA_2_CODE = "LA_2";
    private static final String LA_2_USER_1_ID = "LA_2-1";
    private static final String LA_2_USER_2_ID = "LA_2-2";
    private static final List<String> LA_2_USER_IDS = List.of(CALLER_ID, LA_2_USER_1_ID, LA_2_USER_2_ID);

    private static final String CASE_ID = "12345";
    private static final Set<String> CASE_ROLES = Set.of("[LASOLICITOR]", "[CREATOR]");

    @MockBean
    private LocalAuthorityUserLookupConfiguration localAuthorityUserLookupConfiguration;

    @MockBean
    private OrganisationApi organisationApi;

    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockBean
    private CaseUserApi caseUserApi;

    @MockBean
    private IdamClient client;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private SystemUpdateUserConfiguration userConfig;

    CaseInitiationControllerTest() {
        super("case-initiation");
    }

    @BeforeEach
    void setup() {
        given(client.getAccessToken(userConfig.getUserName(), userConfig.getPassword())).willReturn(USER_AUTH_TOKEN);

        given(authTokenGenerator.generate()).willReturn(SERVICE_AUTH_TOKEN);

        given(serviceAuthorisationApi.serviceToken(anyMap())).willReturn(SERVICE_AUTH_TOKEN);

        given(client.getUserInfo(USER_AUTH_TOKEN)).willReturn(
            UserInfo.builder().sub("user@example.gov.uk").build());

        given(localAuthorityUserLookupConfiguration.getUserIds(LA_1_CODE)).willReturn(LA_1_USER_IDS);

        given(localAuthorityUserLookupConfiguration.getUserIds(LA_2_CODE))
            .willThrow(new UnknownLocalAuthorityCodeException(LA_2_CODE));

        givenPRDWillReturn(LA_2_USER_IDS);
    }

    @Test
    void shouldAddCaseLocalAuthorityToCaseData() {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(Map.of("caseName", "title"))
            .build();

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(caseDetails);

        assertThat(callbackResponse.getData())
            .containsEntry("caseName", "title")
            .containsEntry("caseLocalAuthority", "example");
    }

    @Test
    void shouldPopulateErrorsInResponseWhenDomainNameIsNotFound() {
        AboutToStartOrSubmitCallbackResponse expectedResponse = AboutToStartOrSubmitCallbackResponse.builder()
            .errors(List.of("The email address was not linked to a known Local Authority"))
            .build();

        given(client.getUserInfo(USER_AUTH_TOKEN))
            .willReturn(UserInfo.builder().sub("user@email.gov.uk").build());

        AboutToStartOrSubmitCallbackResponse actualResponse = postAboutToSubmitEvent(
            "core-case-data-store-api/empty-case-details.json");

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void updateCaseRolesShouldBeCalledOnceForEachUserFetchedFromPRD() {
        final CallbackRequest request = getCase(LA_2_CODE);

        postSubmittedEvent(request);

        verifyCaseRoleGrantedToEachUser(LA_2_USER_IDS);
        verifyTaskListUpdated(request.getCaseDetails());
    }

    @Test
    void updateCaseRolesShouldBeCalledOnceForEachUser() {
        givenPRDWillFail();

        final CallbackRequest request = getCase(LA_1_CODE);

        postSubmittedEvent(request);

        verifyCaseRoleGrantedToEachUser(LA_1_USER_IDS);
        verifyTaskListUpdated(request.getCaseDetails());
    }

    @Test
    void shouldGrantCaseAccessToOtherUsersAndThrowExceptionWhenCallerAccessNotGranted() {
        doThrow(RuntimeException.class)
            .when(caseUserApi).updateCaseRolesForUser(any(), any(), any(), eq(CALLER_ID), any());

        givenPRDWillReturn(LA_1_USER_IDS);

        final Exception exception = assertThrows(Exception.class, () -> postSubmittedEvent(getCase(LA_1_CODE)));

        assertException(exception)
            .isCausedBy(new GrantCaseAccessException(CASE_ID, Set.of(USER_ID), Set.of(CREATOR, LASOLICITOR)));

        verifyCaseRoleGrantedToEachUser(LA_1_USER_IDS);
    }

    @Test
    void shouldAttemptGrantAccessToAllLocalAuthorityUsersWhenGrantAccessFailsForSomeOfThem() {
        doThrow(RuntimeException.class)
            .when(caseUserApi).updateCaseRolesForUser(any(), any(), any(), eq(LA_1_USER_1_ID), any());

        givenPRDWillReturn(LA_1_USER_IDS);

        postSubmittedEvent(getCase(LA_1_CODE));

        verifyCaseRoleGrantedToEachUser(LA_1_USER_IDS);
    }

    private void verifyCaseRoleGrantedToEachUser(List<String> users) {
        verify(caseUserApi).updateCaseRolesForUser(
            USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, CASE_ID, CALLER_ID,
            new CaseUser(CALLER_ID, CASE_ROLES));

        checkUntil(() -> users.stream()
            .filter(userId -> !CALLER_ID.equals(userId))
            .forEach(userId ->
                verify(caseUserApi).updateCaseRolesForUser(
                    USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, CASE_ID, userId,
                    new CaseUser(userId, CASE_ROLES))));
    }

    private static OrganisationUsers organisation(List<String> userIds) {
        List<OrganisationUser> users = userIds.stream()
            .map(id -> OrganisationUser.builder().userIdentifier(id).build())
            .collect(toList());

        return OrganisationUsers.builder().users(users).build();
    }

    private static CallbackRequest getCase(String localAuthority) {
        return callbackRequest(Map.of("localAuthority", localAuthority));
    }

    private void verifyTaskListUpdated(CaseDetails caseDetails) {
        verify(coreCaseDataService).triggerEvent(
            eq(JURISDICTION),
            eq(CASE_TYPE),
            eq(caseDetails.getId()),
            eq("internal-update-task-list"),
            anyMap());
    }

    private void givenPRDWillFail() {
        Request request = Request.create(GET, "", Map.of(), new byte[] {}, UTF_8, null);
        given(organisationApi.findUsersByOrganisation(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, Status.ACTIVE, false))
            .willThrow(new FeignException.NotFound("", request, new byte[] {}));
    }

    private void givenPRDWillReturn(List<String> userIds) {
        given(organisationApi.findUsersByOrganisation(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, Status.ACTIVE, false))
            .willReturn(organisation(userIds));
    }
}
