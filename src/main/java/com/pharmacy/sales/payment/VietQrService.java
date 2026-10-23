package com.pharmacy.sales.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class VietQrService {
    private static final Logger log = LoggerFactory.getLogger(VietQrService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Map<String, String> BANK_BIN_MAP = new HashMap<>();

    static {
        BANK_BIN_MAP.put("MB", "970422");
        BANK_BIN_MAP.put("MBBANK", "970422");
        BANK_BIN_MAP.put("VCB", "970436");
        BANK_BIN_MAP.put("VIETCOMBANK", "970436");
        BANK_BIN_MAP.put("CTG", "970415");
        BANK_BIN_MAP.put("ICB", "970415");
        BANK_BIN_MAP.put("VIETINBANK", "970415");
        BANK_BIN_MAP.put("BIDV", "970418");
        BANK_BIN_MAP.put("ACB", "970416");
        BANK_BIN_MAP.put("TCB", "970407");
        BANK_BIN_MAP.put("TECHCOMBANK", "970407");
        BANK_BIN_MAP.put("TPB", "970423");
        BANK_BIN_MAP.put("TPBANK", "970423");
        BANK_BIN_MAP.put("VPB", "970432");
        BANK_BIN_MAP.put("VPBANK", "970432");
        BANK_BIN_MAP.put("VIB", "970441");
        BANK_BIN_MAP.put("STB", "970403");
        BANK_BIN_MAP.put("SACOMBANK", "970403");
        BANK_BIN_MAP.put("SHB", "970443");
        BANK_BIN_MAP.put("HDB", "970437");
        BANK_BIN_MAP.put("HDBANK", "970437");
        BANK_BIN_MAP.put("OCB", "970448");
        BANK_BIN_MAP.put("MSB", "970426");
        BANK_BIN_MAP.put("LPB", "970449");
        BANK_BIN_MAP.put("LPBANK", "970449");
        BANK_BIN_MAP.put("SEAB", "970440");
        BANK_BIN_MAP.put("SEABANK", "970440");
        BANK_BIN_MAP.put("BVB", "970438");
        BANK_BIN_MAP.put("NCB", "970419");
        BANK_BIN_MAP.put("KLB", "970452");
        BANK_BIN_MAP.put("PVB", "970412");
        BANK_BIN_MAP.put("ABB", "970425");
        BANK_BIN_MAP.put("BAB", "970409");
    }

    public static String getBinForBank(String bankInput) {
        if (bankInput == null || bankInput.isBlank()) {
            return "970422"; // default MB Bank
        }
        String clean = bankInput.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (clean.matches("\\d{6}")) {
            return clean;
        }
        return BANK_BIN_MAP.getOrDefault(clean, "970422");
    }

    public static Image generateQrImage(String bankCode, String accountNo, String accountName, BigDecimal amount, String content) {
        try {
            String bin = getBinForBank(bankCode);
            long amountVal = amount != null ? amount.longValue() : 0L;

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("accountNo", accountNo);
            reqBody.put("accountName", accountName != null ? accountName : "");
            reqBody.put("acqId", bin);
            reqBody.put("amount", amountVal);
            reqBody.put("addInfo", content != null ? content : "");
            reqBody.put("format", "text");
            reqBody.put("template", "compact2");

            String jsonPayload = mapper.writeValueAsString(reqBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.vietqr.io/v2/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                if ("00".equals(root.path("code").asText())) {
                    String qrDataUrl = root.path("data").path("qrDataURL").asText();
                    if (qrDataUrl != null && qrDataUrl.contains(",")) {
                        String base64 = qrDataUrl.substring(qrDataUrl.indexOf(",") + 1);
                        byte[] imageBytes = Base64.getDecoder().decode(base64);
                        return new Image(new ByteArrayInputStream(imageBytes));
                    }
                }
            }
            log.warn("VietQR API returned non-success response: {}", response.body());
        } catch (Exception e) {
            log.error("Failed to generate VietQR image: {}", e.getMessage());
        }
        return null;
    }
}
