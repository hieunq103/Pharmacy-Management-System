package com.pharmacy.sales.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SepayService {
    private static final Logger log = LoggerFactory.getLogger(SepayService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public record PaymentResult(
            boolean matched,
            BigDecimal amount,
            String content,
            String transactionDate,
            String referenceNumber) {
        public static PaymentResult notMatched() {
            return new PaymentResult(false, BigDecimal.ZERO, null, null, null);
        }

        public static PaymentResult matched(BigDecimal amount, String content, String date, String ref) {
            return new PaymentResult(true, amount, content, date, ref);
        }
    }

    /**
     * Kiểm tra SePAY xem có giao dịch chuyển khoản nào khớp với mã đơn hàng và số
     * tiền hay không.
     *
     * @param orderCode      Mã đơn hàng
     * @param expectedAmount Số tiền cần thanh toán
     * @return PaymentResult nếu tìm thấy giao dịch hợp lệ
     */
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

                        // Kiểm tra số tiền nhận vào >= số tiền đơn hàng
                        if (amountIn.compareTo(expectedAmount) < 0) {
                            continue;
                        }

                        String content = tx.path("transaction_content").asText("");
                        String normContent = content.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

                        // Kiểm tra nội dung giao dịch có chứa mã đơn hàng không
                        if (normContent.contains(normCode)) {
                            String txDate = tx.path("transaction_date").asText("");
                            String refNum = tx.path("reference_number").asText("");
                            log.info("Detected matching SePAY transaction! Order={}, Amount={}, Ref={}",
                                    orderCode, amountIn, refNum);
                            return PaymentResult.matched(amountIn, content, txDate, refNum);
                        }
                    }
                }
            } else {
                log.warn("SePAY API call failed with status: {}, body: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Error while querying SePAY API: {}", e.getMessage());
        }

        return PaymentResult.notMatched();
    }
}
