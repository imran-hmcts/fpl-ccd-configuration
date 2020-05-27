package uk.gov.hmcts.reform.fpl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.EmailAddress;
import uk.gov.hmcts.reform.fpl.model.common.Telephone;
import uk.gov.hmcts.reform.fpl.model.order.selector.ChildSelector;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testChildren;

@ExtendWith(SpringExtension.class)
class ChildrenServiceTest {

    private final ChildrenService service = new ChildrenService();

    @Test
    void shouldBuildExpectedLabelWhenEmptyList() {
        String label = service.getChildrenLabel(List.of());
        assertThat(label).isEqualTo("No children in the case");
    }

    @Test
    void shouldBuildExpectedLabelWhenPopulatedList() {
        String label = service.getChildrenLabel(List.of(childWithConfidentialFields(randomUUID())));
        assertThat(label).isEqualTo("Child 1: James\n");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnFalseWhenListEmptyOrNull(List<Element<Child>> list) {
        boolean result = service.allChildrenHaveFinalOrder(list);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAtLeastOneChildDoesNotHaveFinalOrder() {
        List<Element<Child>> children = List.of(childWithFinalOrderIssued("Yes"), childWithFinalOrderIssued("No"));

        boolean result = service.allChildrenHaveFinalOrder(children);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueWhenAllChildrenHaveFinalOrder() {
        List<Element<Child>> children = List.of(childWithFinalOrderIssued("Yes"), childWithFinalOrderIssued("Yes"));

        boolean result = service.allChildrenHaveFinalOrder(children);

        assertThat(result).isTrue();
    }

    @Test
    void shouldUpdateFinalOrderIssuedWhenAppliesToAllChildren() {
        List<Element<Child>> result = service.updateFinalOrderIssued(testChildren(), "Yes", null);

        assertThat(result).extracting(element -> element.getValue().getFinalOrderIssued())
            .containsExactly("Yes", "Yes", "Yes");
    }

    @Test
    void shouldUpdateFinalOrderIssuedWhenAppliesToSelectedChildren() {
        List<Element<Child>> children = testChildren();

        ChildSelector childSelector = ChildSelector.builder()
            .childCount("1")
            .selected(List.of(1))
            .build();

        List<Element<Child>> result = service.updateFinalOrderIssued(children, "No", childSelector);

        assertThat(result).extracting(element -> element.getValue().getFinalOrderIssued())
            .containsExactly("No", "Yes", "No");
    }

    @Test
    void shouldUpdateFinalOrderIssuedWhenAppliesToSelectedChildrenAndAlreadyIssuedForOtherChild() {
        List<Element<Child>> children = List.of(childWithFinalOrderIssued("No"), childWithFinalOrderIssued("Yes"),
            childWithFinalOrderIssued("No"), childWithFinalOrderIssued("No"));

        ChildSelector childSelector = ChildSelector.builder()
            .childCount("4")
            .selected(List.of(0, 2))
            .build();

        List<Element<Child>> result = service.updateFinalOrderIssued(children, "No", childSelector);

        assertThat(result).extracting(element -> element.getValue().getFinalOrderIssued())
            .containsExactly("Yes", "Yes", "Yes", "No");
    }

    private Element<Child> childWithConfidentialFields(UUID id) {
        return element(id, Child.builder()
            .party(ChildParty.builder()
                .firstName("James")
                .detailsHidden("Yes")
                .email(EmailAddress.builder().email("email@email.com").build())
                .address(Address.builder().addressLine1("Address Line 1").build())
                .telephoneNumber(Telephone.builder().telephoneNumber("01227 831393").build())
                .build())
            .build());
    }

    private Element<Child> childWithFinalOrderIssued(String finalOrderIssued) {
        return element(Child.builder()
            .finalOrderIssued(finalOrderIssued)
            .party(ChildParty.builder()
                .firstName(randomAlphanumeric(10))
                .lastName(randomAlphanumeric(10))
                .build())
            .build());
    }
}
