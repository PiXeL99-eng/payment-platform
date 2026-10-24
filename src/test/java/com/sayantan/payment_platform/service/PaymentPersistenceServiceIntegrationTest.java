package com.sayantan.payment_platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.sayantan.payment_platform.exception.IdempotencyConflictException;
import com.sayantan.payment_platform.model.PaymentEvent;
import com.sayantan.payment_platform.model.PaymentPreparation;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.model.PaymentStatus;
import com.sayantan.payment_platform.repository.PaymentEventRepository;
import com.sayantan.payment_platform.repository.PaymentRepository;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentPersistenceServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("payment_test_db")
            .withUsername("payment_test_user")
            .withPassword("payment_test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PaymentPersistenceService paymentPersistenceService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentEventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void preparePaymentPersistsProcessingPaymentAndLifecycleEvents() {
        PaymentRequest request = request("ORDER-1001", "150.00");

        PaymentPreparation preparation =
            paymentPersistenceService.preparePayment(request, "key-1001");

        assertThat(preparation.newlyCreated()).isTrue();
        assertThat(preparation.payment().status()).isEqualTo(PaymentStatus.PROCESSING);

        PaymentResponse storedPayment = paymentPersistenceService
            .findExistingPayment(request, "key-1001")
            .orElseThrow()
            .payment();

        assertThat(storedPayment.status()).isEqualTo(PaymentStatus.PROCESSING);

        List<PaymentEvent> events = paymentEventRepository
            .findByPaymentIdOrderByCreatedAtAscIdAsc(storedPayment.id());

        assertThat(events)
            .extracting(event -> Objects.requireNonNull(event).getStatus())
            .containsExactly(PaymentStatus.CREATED, PaymentStatus.PROCESSING);
    }

    @Test
    void completePaymentPersistsFinalStatusAndEvent() {
        PaymentPreparation preparation = paymentPersistenceService.preparePayment(
            request("ORDER-1002", "250.00"),
            "key-1002"
        );

        PaymentResponse completed = paymentPersistenceService.completePayment(
            preparation.payment().id(),
            PaymentStatus.SUCCESS
        );

        assertThat(completed.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentRepository.findById(completed.id()).orElseThrow().getStatus())
            .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentEventRepository
            .findByPaymentIdOrderByCreatedAtAscIdAsc(completed.id()))
            .extracting(event -> Objects.requireNonNull(event).getStatus())
            .containsExactly(
                PaymentStatus.CREATED,
                PaymentStatus.PROCESSING,
                PaymentStatus.SUCCESS
            );
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingPaymentWithoutNewRows() {
        PaymentRequest request = request("ORDER-1003", "350.00");

        PaymentPreparation first =
            paymentPersistenceService.preparePayment(request, "key-1003");
        PaymentPreparation repeated =
            paymentPersistenceService.preparePayment(request, "key-1003");

        assertThat(repeated.newlyCreated()).isFalse();
        assertThat(repeated.payment().id()).isEqualTo(first.payment().id());
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(paymentEventRepository.count()).isEqualTo(2);
    }

    @Test
    void repeatedIdempotencyKeyWithDifferentPayloadThrowsConflict() {
        paymentPersistenceService.preparePayment(
            request("ORDER-1004", "450.00"),
            "key-1004"
        );

        assertThatThrownBy(() -> paymentPersistenceService.preparePayment(
            request("ORDER-DIFFERENT", "450.00"),
            "key-1004"
        ))
            .isInstanceOf(IdempotencyConflictException.class)
            .hasMessageContaining("different payment request");

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(paymentEventRepository.count()).isEqualTo(2);
    }

    private PaymentRequest request(String orderId, String amount) {
        return new PaymentRequest(orderId, new BigDecimal(amount), "INR");
    }
}
