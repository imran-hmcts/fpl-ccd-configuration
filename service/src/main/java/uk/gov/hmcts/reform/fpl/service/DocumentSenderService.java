package uk.gov.hmcts.reform.fpl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.SentDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.service.time.Time;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocumentSenderService {

    private final Time time;
    private final DateFormatterService dateFormatterService;
    private final SendLetterApi sendLetterApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentDownloadService documentDownloadService;
    private final DocmosisCoverDocumentsService docmosisCoverDocumentsService;

    public List<SentDocument> send(DocumentReference mainDocument, List<Representative> representativesServedByPost,
                                   Long caseId, String familyManCaseNumber) {
        List<SentDocument> printedDocuments = new ArrayList<>();
        for (Representative representative : representativesServedByPost) {
            byte[] mainDocumentBinary = documentDownloadService.downloadDocument(mainDocument.getBinaryUrl());
            byte[] coverDocument = docmosisCoverDocumentsService.createCoverDocuments(familyManCaseNumber,
                caseId,
                representative).getBytes();

            sendLetterApi.sendLetter(authTokenGenerator.generate(),
                new LetterWithPdfsRequest(List.of(coverDocument, mainDocumentBinary),
                    "string",
                    Map.of()));

            printedDocuments.add(SentDocument.builder()
                .partyName(representative.getFullName())
                .document(mainDocument)
                .sentAt(dateFormatterService.formatLocalDateTimeBaseUsingFormat(time.now(), "h:mma, d MMMM yyyy"))
                .build());
        }

        return printedDocuments;
    }
}
