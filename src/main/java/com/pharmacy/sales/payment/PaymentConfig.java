package com.pharmacy.sales.payment;

import com.pharmacy.infra.config.AppConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);
    private static final Properties props = new Properties();

    private static String bankId = "";
    private static String accountNo = "";
    private static String accountName = "";
    private static String sepayApiToken = "";
    private static String sepayBaseUrl = "https://my.sepay.vn/userapi";
    private static long pollIntervalMs = 2000;

    static {
        loadProperties();
    }

    public static synchronized void loadProperties() {
        InputStream in = null;
        try {
            File localFile = new File("src/main/resources/payment.properties");
            if (localFile.exists()) {
                in = new FileInputStream(localFile);
            } else {
                in = PaymentConfig.class.getResourceAsStream("/payment.properties");
            }

            if (in != null) {
                props.clear();
                props.load(in);
                bankId = props.getProperty("payment.bank_id", "").trim();
                accountNo = props.getProperty("payment.account_no", "").trim();
                accountName = props.getProperty("payment.account_name", "").trim();
                sepayApiToken = props.getProperty("sepay.api_token", "").trim();
                sepayBaseUrl = props.getProperty("sepay.base_url", "https://my.sepay.vn/userapi").trim();
                String pollStr = props.getProperty("sepay.poll_interval_ms", "2000").trim();
                try {
                    pollIntervalMs = Long.parseLong(pollStr);
                } catch (NumberFormatException ignored) {}

                bankId = envOr("PAYMENT_BANK_ID", bankId);
                accountNo = envOr("PAYMENT_ACCOUNT_NO", accountNo);
                accountName = envOr("PAYMENT_ACCOUNT_NAME", accountName);
                sepayApiToken = envOr("SEPAY_API_TOKEN", sepayApiToken);

                // Không ghi số tài khoản/tên chủ tài khoản vào log (thông tin cá nhân)
                log.info(
                    "Loaded payment configuration: Bank={}, AccountConfigured={}, SePAY Token present={}",
                    bankId,
                    !accountNo.isBlank(),
                    isSepayConfigured()
                );
                if (bankId.isBlank() || accountNo.isBlank()) {
                    log.warn(
                        "Chưa cấu hình tài khoản nhận tiền: đặt PAYMENT_BANK_ID, PAYMENT_ACCOUNT_NO, PAYMENT_ACCOUNT_NAME trong .env."
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to load payment.properties: {}", e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private static String envOr(String key, String fallback) {
        String val = AppConfig.env(key);
        return val != null ? val : fallback;
    }

    public static String getBankId() {
        return bankId;
    }

    public static String getAccountNo() {
        return accountNo;
    }

    public static String getAccountName() {
        return accountName;
    }

    public static String getSepayApiToken() {
        return sepayApiToken;
    }

    public static String getSepayBaseUrl() {
        return sepayBaseUrl;
    }

    public static long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public static boolean isSepayConfigured() {
        return (
            sepayApiToken != null &&
            !sepayApiToken.isBlank() &&
            !sepayApiToken.equalsIgnoreCase("YOUR_SEPAY_API_TOKEN_HERE")
        );
    }
}
