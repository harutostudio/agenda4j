package io.agenda4j.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agenda4j.Agenda;
import io.agenda4j.JobHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgendaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgendaConfig.class))
            .withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(JobHandler.class, DemoJobHandler::new)
            .withPropertyValues(
                    "agenda.enabled=true",
                    "agenda.worker-id=test-worker",
                    "agenda.process-every=500ms",
                    "agenda.default-lock-lifetime=5s"
            );

    @Test
    void shouldAutoConfigureAgendaBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Agenda.class);
            assertThat(context).hasSingleBean(AgendaLifecycle.class);
            assertThat(context).hasSingleBean(AgendaProperties.class);
        });
    }

    static class DemoJobHandler implements JobHandler<String> {
        @Override
        public String name() {
            return "demo";
        }

        @Override
        public Class<String> dataClass() {
            return String.class;
        }

        @Override
        public void execute(String data) {
            // no-op for context bootstrap test
        }
    }
}
