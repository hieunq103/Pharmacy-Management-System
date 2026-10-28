package com.pharmacy.sales.payment;

import com.pharmacy.infra.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PaymentConfig {
    private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);
    private static final Properties props = new Properties();

    private static String bankId = "";
    private static String accountNo = "";
    private static String accountName = "";
    private static String sepayApiToken = "";
    private static String sepayBaseUrl = "https://my.sepay.vn/userapi";
    private static String storeCode = "CH01";
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
                storeCode = props.getProperty("payment.store_code", "CH01").trim();
                String pollStr = props.getProperty("sepay.poll_interval_ms", "2000").trim();
                try {
                    pollIntervalMs = Long.parseLong(pollStr);
                } catch (NumberFormatException ignored) {
                }

                bankId = envOr("PAYMENT_BANK_ID", bankId);
                accountNo = envOr("PAYMENT_ACCOUNT_NO", accountNo);
                accountName = envOr("PAYMENT_ACCOUNT_NAME", accountName);
                sepayApiToken = envOr("SEPAY_API_TOKEN", sepayApiToken);
                storeCode = envOr("PAYMENT_STORE_CODE", storeCode);

                log.info(
                        "Loaded payment configuration: StoreCode={}, Bank={}, AccountNo={}, AccountName={}, SePAY Token present={}",
                        storeCode, bankId, accountNo, accountName, isSepayConfigured());
            }
        } catch (Exception e) {
            log.error("Failed to load payment.properties: {}", e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
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

    public static String getStoreCode() {
        return storeCode;
    }

    public static boolean isSepayConfigured() {
        return sepayApiToken != null && !sepayApiToken.isBlank()
                && !sepayApiToken.equalsIgnoreCase("YOUR_SEPAY_API_TOKEN_HERE");
    }
}
