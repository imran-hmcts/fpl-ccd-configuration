package uk.gov.hmcts.reform.fpl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.Placement;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.asDynamicList;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.unwrapElements;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testChild;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testPlacement;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ObjectMapper.class, PlacementService.class})
public class PlacementServiceTest {

    @Autowired
    private PlacementService placementService;

    @Nested
    class HasSingleChild {

        @Test
        public void shouldReturnFalseWhenCaseHasNoChild() {
            CaseData caseData = caseWithChildren(emptyList());
            assertThat(placementService.hasSingleChild(caseData)).isFalse();
        }

        @Test
        public void shouldReturnFalseWhenCaseHasMoreThanOneChild() {
            CaseData caseData = caseWithChildren(testChild(), testChild());
            assertThat(placementService.hasSingleChild(caseData)).isFalse();
        }

        @Test
        public void shouldReturnTrueWhenCaseHasOneChild() {
            CaseData caseData = caseWithChildren(testChild());
            assertThat(placementService.hasSingleChild(caseData)).isTrue();
        }
    }

    @Nested
    class GetChild {

        @Test
        public void shouldGetChild() {
            Element<Child> child1 = testChild();
            Element<Child> child2 = testChild();

            CaseData caseData = caseWithChildren(child1, child2);

            assertThat(placementService.getChild(caseData, child1.getId())).isEqualTo(child1);
            assertThat(placementService.getChild(caseData, child2.getId())).isEqualTo(child2);
        }

        @Test
        public void shouldReturnNullIfChildIsNotFound() {
            Element<Child> child1 = testChild();

            CaseData caseData = caseWithChildren(child1);

            assertThat(placementService.getChild(caseData, randomUUID())).isNull();
        }
    }

    @Nested
    class GetChildrenList {

        @Test
        public void shouldGetChildrenList() {
            Element<Child> child1 = testChild();
            Element<Child> child2 = testChild();

            List<Element<Child>> children = List.of(child1, child2);

            CaseData caseData = caseWithChildren(children);
            DynamicList expectedList = asDynamicList(children, child -> child.getParty().getFullName());

            assertThat(placementService.getChildrenList(caseData, null)).isEqualTo(expectedList);
        }

        @Test
        public void shouldGetPreselectedChildrenList() {
            Element<Child> child1 = testChild();
            Element<Child> child2 = testChild();

            List<Element<Child>> children = List.of(child1, child2);

            CaseData caseData = caseWithChildren(children);
            DynamicList expectedList = asDynamicList(List.of(child1, child2), child2.getId(),
                child -> child.getParty().getFullName());

            assertThat(placementService.getChildrenList(caseData, child2)).isEqualTo(expectedList);
        }

        @Test
        public void shouldGetEmptyChildrenList() {
            List<Element<Child>> children = emptyList();

            CaseData caseData = caseWithChildren(children);
            DynamicList expectedList = asDynamicList(children, child -> child.getParty().getFullName());

            assertThat(placementService.getChildrenList(caseData, null)).isEqualTo(expectedList);
        }
    }

    @Nested
    class GetPlacement {

        @Test
        public void shouldGetDefaultPlacement() {
            Element<Child> child = testChild();

            CaseData caseData = caseWithChildren(child);

            Placement defaultPlacement = Placement.builder()
                .childId(child.getId())
                .childName(child.getValue().getParty().getFullName())
                .build();

            assertThat(placementService.getPlacement(caseData, child)).isEqualTo(defaultPlacement);
        }

        @Test
        public void shouldGetExistingPlacement() {
            Element<Child> child1 = testChild();
            Element<Child> child2 = testChild();

            Placement placement1 = testPlacement(child1);
            Placement placement2 = testPlacement(child2);

            List<Element<Child>> children = List.of(child1, child2);
            List<Element<Placement>> placements = wrapElements(placement1, placement2);

            CaseData caseData = caseWithPlacement(children, placements);

            assertThat(placementService.getPlacement(caseData, child2)).isEqualTo(placement2);
        }
    }

    @Nested
    class SetPlacement {

        @Test
        public void shouldAddNewPlacement() {
            Element<Child> child1 = testChild();
            Element<Child> child2 = testChild();

            Placement child1Placement = testPlacement(child1);
            Placement child2Placement = testPlacement(child2);

            CaseData caseData = caseWithPlacement(List.of(child1, child2), wrapElements(child1Placement));

            List<Element<Placement>> updatedPlacements = placementService.setPlacement(caseData, child2Placement);

            assertThat(unwrapElements(updatedPlacements)).containsExactlyInAnyOrder(child1Placement, child2Placement);
        }

        @Test
        public void shouldUpdateExistingPlacement() {
            Element<Child> child1 = testChild();
            Element<Child> child2 = testChild();

            Placement child1Placement = testPlacement(child1);
            Placement child2Placement = testPlacement(child2);

            List<Element<Child>> children = List.of(child1, child2);
            List<Element<Placement>> placements = wrapElements(child1Placement, child2Placement);

            CaseData caseData = caseWithPlacement(children, placements);

            Placement updatedChild2 = testPlacement(child2);

            List<Element<Placement>> updatedPlacements = placementService.setPlacement(caseData, updatedChild2);

            assertThat(unwrapElements(updatedPlacements)).containsExactlyInAnyOrder(child1Placement, updatedChild2);
        }
    }

    @SafeVarargs
    private static CaseData caseWithChildren(Element<Child>... children) {
        return caseWithChildren(Arrays.asList(children));
    }

    private static CaseData caseWithChildren(List<Element<Child>> children) {
        return caseWithPlacement(children, null);
    }

    private static CaseData caseWithPlacement(List<Element<Child>> children, List<Element<Placement>> placements) {
        return CaseData.builder().children1(children).placements(placements).build();
    }
}