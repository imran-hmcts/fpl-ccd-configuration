package uk.gov.hmcts.reform.rd.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.fpl.config.FeignConfiguration;
import uk.gov.hmcts.reform.rd.model.AddCaseAssignedUserRolesRequest;
import uk.gov.hmcts.reform.rd.model.AddCaseAssignedUserRolesResponse;
import uk.gov.hmcts.reform.rd.model.Organisation;
import uk.gov.hmcts.reform.rd.model.OrganisationUser;
import uk.gov.hmcts.reform.rd.model.OrganisationUsers;
import uk.gov.hmcts.reform.rd.model.Status;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

@FeignClient(name = "rd-professional-api", url = "${rd_professional.api.url}", configuration = FeignConfiguration.class)
public interface OrganisationApi {
    @GetMapping("/refdata/external/v1/organisations/users")
    OrganisationUsers findUsersByOrganisation(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestParam(value = "status") Status status,
        @RequestParam(value = "returnRoles") Boolean returnRoles
    );

    @GetMapping("/refdata/external/v1/organisations/users/accountId")
    OrganisationUser findUserByEmail(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestParam(value = "email") final String email
    );

    @GetMapping("/refdata/external/v1/organisations")
    Organisation findOrganisationById(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization
    );

    @PostMapping(
         value = "/refdata/external/v2/case-users",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    AddCaseAssignedUserRolesResponse addCaseUserRoles(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestBody AddCaseAssignedUserRolesRequest caseRoleRequest
    );
}
