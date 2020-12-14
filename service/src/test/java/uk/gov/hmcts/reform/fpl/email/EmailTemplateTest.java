package uk.gov.hmcts.reform.fpl.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.service.notify.NotificationClient;

@SpringBootTest(classes = {ObjectMapper.class, NotificationService.class})
@Import(EmailTemplateTest.TestConfiguration.class)
public class EmailTemplateTest {

    protected static final String NEW_LINE = "\r\n";

    public static class TestConfiguration {
        @Bean
        public NotificationClient notificationClient() {
            return new NotificationClient(
                "integrationtests-12f756df-f01d-4a32-a405-e1ea8a494fbb-0d14df98-a35d-4d56-9d0c-006094b18ed4"
            );
        }
    }

    String line(String line) {
        return line + NEW_LINE;
    }

    String line() {
        return NEW_LINE;
    }
}
