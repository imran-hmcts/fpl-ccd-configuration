package uk.gov.hmcts.reform.fpl.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.reform.fpl.enums.JudicialMessageStatus;
import uk.gov.hmcts.reform.fpl.enums.YesNo;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.fpl.enums.YesNo.YES;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@ToString(callSuper = true)
public class JudicialMessage extends JudicialMessageMetaData {
    private final String dateSent;
    private final LocalDateTime updatedTime;
    private final String note;
    private final JudicialMessageStatus status;
    private final List<Element<DocumentReference>> relatedDocuments;
    private final String relatedDocumentFileNames;
    private final YesNo isRelatedToC2;
    private final String messageHistory;

    public String toLabel() {
        List<String> labels = new ArrayList<>();

        if (YES.equals(isRelatedToC2)) {
            labels.add("C2");
        }

        if (getUrgency() != null) {
            labels.add(getUrgency());
        }

        labels.add(dateSent);

        return String.join(", ", labels);
    }
}
