package io.agenda4j.internal.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import io.agenda4j.Agenda;
import io.agenda4j.JobHandler;
import io.agenda4j.config.AgendaProperties;
import io.agenda4j.core.CancelMode;
import io.agenda4j.core.CancelQuery;
import io.agenda4j.core.JobHandlerRegistry;
import io.agenda4j.core.JobType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class MongoAgendaIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private MongoTemplate mongoTemplate;
    private MongoJobStore jobStore;

    @BeforeEach
    void setUp() {
        mongoTemplate = new MongoTemplate(MongoClients.create(MONGO.getReplicaSetUrl()), "agenda4j_test");
        mongoTemplate.dropCollection(ScheduledJobDocument.class);
        jobStore = new MongoJobStore(mongoTemplate, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(ScheduledJobDocument.class);
    }

    @Test
    void claimDueJobsShouldLockAndPreventDoubleClaim() {
        ScheduledJobDocument due = newDoc("email-sync", Instant.now().minusSeconds(5));
        mongoTemplate.insert(due);

        List<ScheduledJobDocument> claimed = jobStore.claimDueJobs(
                Instant.now().plusSeconds(2),
                1,
                Duration.ofSeconds(30),
                "worker-A"
        );

        assertEquals(1, claimed.size());
        ScheduledJobDocument locked = claimed.getFirst();
        assertEquals("worker-A", locked.getLockedBy());
        assertNotNull(locked.getLockUntil());

        List<ScheduledJobDocument> secondClaim = jobStore.claimDueJobs(
                Instant.now().plusSeconds(2),
                1,
                Duration.ofSeconds(30),
                "worker-B"
        );

        assertTrue(secondClaim.isEmpty());
    }

    @Test
    void cancelShouldSupportDisableAndDelete() {
        Agenda agenda = new MongoAgenda(defaultProps(), jobStore, new JobHandlerRegistry(List.of()), new ObjectMapper());

        ScheduledJobDocument disableCandidate = newDoc("cleanup", Instant.now().plusSeconds(30));
        mongoTemplate.insert(disableCandidate);

        var disableResult = agenda.cancel(
                CancelQuery.builder().name("cleanup").build(),
                new Agenda.CancelOptions(CancelMode.DISABLE, 10)
        );

        assertEquals(1, disableResult.modified());
        ScheduledJobDocument disabled = mongoTemplate.findById(disableCandidate.getId(), ScheduledJobDocument.class);
        assertNotNull(disabled);
        assertNull(disabled.getNextRunAt());

        ScheduledJobDocument deleteCandidate = newDoc("cleanup-delete", Instant.now().plusSeconds(30));
        mongoTemplate.insert(deleteCandidate);

        var deleteResult = agenda.cancel(
                CancelQuery.builder().name("cleanup-delete").build(),
                new Agenda.CancelOptions(CancelMode.DELETE, 10)
        );

        assertEquals(1, deleteResult.deleted());
        ScheduledJobDocument deleted = mongoTemplate.findById(deleteCandidate.getId(), ScheduledJobDocument.class);
        assertNull(deleted);
    }

    @Test
    void failedHandlerShouldIncreaseFailCountAndReschedule() throws Exception {
        JobHandler<Map<String, Object>> failingHandler = new JobHandler<>() {
            @Override
            public String name() {
                return "failing-job";
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<Map<String, Object>> dataClass() {
                return (Class<Map<String, Object>>) (Class<?>) Map.class;
            }

            @Override
            public void execute(Map<String, Object> data) {
                throw new IllegalStateException("simulated failure");
            }
        };

        MongoAgenda agenda = new MongoAgenda(
                defaultProps(),
                jobStore,
                new JobHandlerRegistry(List.of(failingHandler)),
                new ObjectMapper()
        );

        agenda.now("failing-job", Map.of("id", "A1"));
        agenda.start();

        boolean reached = waitUntil(8, TimeUnit.SECONDS, () -> {
            ScheduledJobDocument doc = mongoTemplate.findOne(
                    new Query(Criteria.where("name").is("failing-job")),
                    ScheduledJobDocument.class
            );
            return doc != null && doc.getFailCount() >= 1 && doc.getNextRunAt() != null;
        });

        agenda.stop();

        assertTrue(reached);
        ScheduledJobDocument updated = mongoTemplate.findOne(
                new Query(Criteria.where("name").is("failing-job")),
                ScheduledJobDocument.class
        );
        assertNotNull(updated);
        assertTrue(updated.getFailCount() >= 1);
        assertNotNull(updated.getNextRunAt());
        assertFalse(updated.getNextRunAt().isBefore(Instant.now().minusSeconds(1)));
    }

    private static ScheduledJobDocument newDoc(String name, Instant nextRunAt) {
        ScheduledJobDocument doc = new ScheduledJobDocument();
        doc.setName(name);
        doc.setType(JobType.NORMAL);
        doc.setPriority(0);
        doc.setNextRunAt(nextRunAt);
        doc.setData(Map.of("k", "v"));
        return doc;
    }

    private static AgendaProperties defaultProps() {
        AgendaProperties props = new AgendaProperties();
        props.setProcessEvery(Duration.ofMillis(200));
        props.setDefaultLockLifetime(Duration.ofSeconds(2));
        props.setMaxConcurrency(1);
        props.setDefaultConcurrency(1);
        props.setLockLimit(10);
        props.setBatchSize(1);
        props.setMaxRetryCount(3);
        props.setCleanupFinishedJobs(false);
        props.setWorkerId("test-worker");
        return props;
    }

    private static boolean waitUntil(long timeout, TimeUnit unit, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
