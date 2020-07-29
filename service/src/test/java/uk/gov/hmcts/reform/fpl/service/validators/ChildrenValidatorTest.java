package uk.gov.hmcts.reform.fpl.service.validators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.utils.ElementUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ChildrenValidator.class, LocalValidatorFactoryBean.class})
class ChildrenValidatorTest {

    @Autowired
    private ChildrenValidator childrenValidator;

    @Test
    void shouldReturnErrorWhenNoChildrenSpecified() {

        final CaseData caseData = CaseData.builder().build();

        final List<String> errors = childrenValidator.validate(caseData);

        assertThat(errors).contains("You need to add details to children");
    }

    @Test
    void shouldReturnErrorsWhenNoChildrenDetailsSpecified() {

        final Child child = Child.builder()
            .party(ChildParty.builder().build())
            .build();

        final CaseData caseData = CaseData.builder()
            .children1(ElementUtils.wrapElements(child))
            .build();

        final List<String> errors = childrenValidator.validate(caseData);

        assertThat(errors).containsExactlyInAnyOrder(
            "Tell us the names of all children in the case",
            "Tell us the gender of all children in the case",
            "Tell us the date of birth of all children in the case"
        );
    }

    @Test
    void shouldReturnEmptyErrorsWhenRequiredChildrenDetailsArePresentAndValid() {

        final Child child = Child.builder()
            .party(ChildParty.builder()
                .firstName("Alex")
                .lastName("Brown")
                .gender("Boy")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build())
            .build();

        final CaseData caseData = CaseData.builder()
            .children1(ElementUtils.wrapElements(child))
            .build();

        final List<String> errors = childrenValidator.validate(caseData);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnErrorWhenDetailsOfNonStandardGenderAreMissing() {

        final Child child = Child.builder()
            .party(ChildParty.builder()
                .firstName("Alex")
                .lastName("Brown")
                .gender("Other")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build())
            .build();

        final CaseData caseData = CaseData.builder()
            .children1(ElementUtils.wrapElements(child))
            .build();

        final List<String> errors = childrenValidator.validate(caseData);

        assertThat(errors).containsExactly("Tell us the gender of all children in the case");
    }

    @Test
    void shouldReturnErrorWhenDateOfBirthIsInFuture() {

        final Child child = Child.builder()
            .party(ChildParty.builder()
                .firstName("Alex")
                .lastName("Brown")
                .gender("Boy")
                .dateOfBirth(LocalDate.now().plusDays(1))
                .build())
            .build();

        final CaseData caseData = CaseData.builder()
            .children1(ElementUtils.wrapElements(child))
            .build();

        final List<String> errors = childrenValidator.validate(caseData);

        assertThat(errors)
            .containsExactly("Date of birth is in the future. You cannot send this application until that date");
    }
}
