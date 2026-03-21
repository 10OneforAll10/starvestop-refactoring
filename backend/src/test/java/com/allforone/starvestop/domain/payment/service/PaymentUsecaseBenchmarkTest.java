package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.dto.response.PaymentConfirmResponse;
import com.allforone.starvestop.domain.payment.dto.response.TossConfirmResponse;
import com.allforone.starvestop.domain.payment.dto.response.TossPaymentResponse;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentUsecaseBenchmarkTest {

    @Mock private PaymentService paymentService;
    @Mock private PrepareConfirmTx prepareConfirmTx;
    @Mock private FinalizeSuccessTx finalizeSuccessTx;
    @Mock private FinalizeFailTx finalizeFailTx;
    @Mock private OrderService orderService;
    @Mock private PaymentEventRelay paymentEventRelay;
    @Mock private PaymentVerifier paymentVerifier;

    @InjectMocks
    private PaymentUsecase paymentUsecase;

    @Test
    void benchmarkConfirmSuccess() throws IOException {
        String orderKey = "ok_bench";
        String paymentKey = "pk_bench";
        long amount = 1000L;

        TossConfirmResponse confirmResponse = new TossConfirmResponse(paymentKey, orderKey, amount, "DONE");
        TossPaymentResponse paymentResponse = new TossPaymentResponse(paymentKey, orderKey, amount, "DONE");

        when(prepareConfirmTx.prepare(orderKey, paymentKey, amount))
                .thenReturn(PrepareConfirmResult.proceed(55L, Map.of(
                        "paymentKey", paymentKey,
                        "orderId", orderKey,
                        "amount", amount
                )));
        when(paymentService.tossApiConfirm(anyMap())).thenReturn(confirmResponse);
        when(paymentService.getPayment(paymentKey)).thenReturn(paymentResponse);
        doNothing().when(paymentVerifier).verify(orderKey, paymentKey, java.math.BigDecimal.valueOf(amount), confirmResponse);
        doNothing().when(paymentVerifier).verify(orderKey, paymentKey, java.math.BigDecimal.valueOf(amount), paymentResponse);
        when(finalizeSuccessTx.finalizeSuccess(orderKey, paymentKey)).thenReturn(55L);

        for (int i = 0; i < 200; i++) {
            paymentUsecase.confirmSuccess(paymentKey, orderKey, amount);
        }

        int iterations = 5000;
        long[] samples = new long[iterations];
        long startAll = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            PaymentConfirmResponse response = paymentUsecase.confirmSuccess(paymentKey, orderKey, amount);
            long end = System.nanoTime();
            assertEquals(55L, response.orderId());
            samples[i] = end - start;
        }
        long total = System.nanoTime() - startAll;

        Arrays.sort(samples);
        double avgUs = Arrays.stream(samples).average().orElse(0) / 1_000.0;
        double p90Us = samples[(int) (iterations * 0.90)] / 1_000.0;
        double p95Us = samples[(int) (iterations * 0.95)] / 1_000.0;
        double maxUs = samples[iterations - 1] / 1_000.0;
        double throughput = iterations / (total / 1_000_000_000.0);

        String json = "{\n" +
                "  \"iterations\": " + iterations + ",\n" +
                "  \"avgUs\": " + String.format(java.util.Locale.US, "%.3f", avgUs) + ",\n" +
                "  \"p90Us\": " + String.format(java.util.Locale.US, "%.3f", p90Us) + ",\n" +
                "  \"p95Us\": " + String.format(java.util.Locale.US, "%.3f", p95Us) + ",\n" +
                "  \"maxUs\": " + String.format(java.util.Locale.US, "%.3f", maxUs) + ",\n" +
                "  \"throughputPerSec\": " + String.format(java.util.Locale.US, "%.3f", throughput) + "\n" +
                "}";

        String benchmarkFileName = System.getProperty("benchmark.file");
        if (benchmarkFileName == null || benchmarkFileName.isBlank()) {
            benchmarkFileName = System.getenv().getOrDefault("BENCHMARK_FILE", "payment-usecase-confirm-before.json");
        }
        Path out = Paths.get("build", "reports", "benchmarks", benchmarkFileName);
        Files.createDirectories(out.getParent());
        Files.writeString(out, json);
    }
}
