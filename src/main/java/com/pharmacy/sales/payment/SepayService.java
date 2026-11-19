package com.pharmacy.sales.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SepayService {

    private static final Logger log = LoggerFactory.getLogger(SepayService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    public record PaymentResult(boolean matched, BigDecimal amount, String content) {
        public static PaymentResult notMatched() {
            return new PaymentResult(false, BigDecimal.ZERO, null);
        }

        public static PaymentResult matched(BigDecimal amount, String content) {
            return new PaymentResult(true, amount, content);
        }
    }

    public static PaymentResult checkOrderPayment(String orderCode, BigDecimal expectedAmount) {
        if (!PaymentConfig.isSepayConfigured()) {
            return PaymentResult.notMatched();
        }

        if (orderCode == null || orderCode.isBlank() || expectedAmount == null) {
            return PaymentResult.notMatched();
        }

        try {
            String token = PaymentConfig.getSepayApiToken();
            String accountNo = PaymentConfig.getAccountNo();
            String baseUrl = PaymentConfig.getSepayBaseUrl();

            String url = baseUrl + "/transactions/list?limit=20";
            if (accountNo != null && !accountNo.isBlank()) {
                url += "&account_number=" + accountNo.trim();
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(4))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode transactions = root.path("transactions");

                if (transactions.isArray()) {
                    String normCode = orderCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

                    for (JsonNode tx : transactions) {
                        String amountInStr = tx.path("amount_in").asText("0");
                        BigDecimal amountIn;
                        try {
                            amountIn = new BigDecimal(amountInStr);
                        } catch (Exception e) {
                            continue;
                        }

                        if (amountIn.compareTo(expectedAmount) < 0) {
                            continue;
                        }

                        String content = tx.path("transaction_content").asText("");
                        String normContent = content.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

                        if (normContent.contains(normCode)) {
                            String refNum = tx.path("reference_number").asText("");
                            log.info(
                                "Detected matching SePAY transaction! Order={}, Amount={}, Ref={}",
                                orderCode,
                                amountIn,
                                refNum
                            );
                            return PaymentResult.matched(amountIn, content);
                        }
                    }
                }
            } else {
                logFailureThrottled(
                    "SePAY API call failed with status: " + response.statusCode() + ", body: " + response.body()
                );
            }
        } catch (Exception e) {
            logFailureThrottled("Error while querying SePAY API: " + e.getMessage());
        }

        return PaymentResult.notMatched();
    }

    // Vòng chờ chuyển khoản gọi API mỗi vài giây: nếu cổng thanh toán lỗi kéo dài thì chỉ ghi log 1 lần/phút để không làm đầy file log
    private static final long FAILURE_LOG_INTERVAL_MS = 60_000;
    private static long lastFailureLogAt;
    private static int suppressedFailures;

    private static synchronized void logFailureThrottled(String message) {
        long now = System.currentTimeMillis();
        if (now - lastFailureLogAt < FAILURE_LOG_INTERVAL_MS) {
            suppressedFailures++;
            return;
        }
        if (suppressedFailures > 0) {
            log.warn("{} (đã bỏ qua {} lần lỗi giống nhau trong 1 phút qua)", message, suppressedFailures);
        } else {
            log.warn(message);
        }
        suppressedFailures = 0;
        lastFailureLogAt = now;
    }
}
