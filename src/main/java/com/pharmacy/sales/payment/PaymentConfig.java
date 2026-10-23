package com.pharmacy.sales.payment;

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

                // Cho phép ghi đè qua Biến môi trường (Environment Variables) để bảo mật khi
                // deploy
                String envBankId = System.getenv("PAYMENT_BANK_ID");
                if (envBankId != null && !envBankId.isBlank())
                    bankId = envBankId.trim();

                String envAccountNo = System.getenv("PAYMENT_ACCOUNT_NO");
                if (envAccountNo != null && !envAccountNo.isBlank())
                    accountNo = envAccountNo.trim();

                String envAccountName = System.getenv("PAYMENT_ACCOUNT_NAME");
                if (envAccountName != null && !envAccountName.isBlank())
                    accountName = envAccountName.trim();

                String envToken = System.getenv("SEPAY_API_TOKEN");
                if (envToken != null && !envToken.isBlank())
                    sepayApiToken = envToken.trim();

                String envStoreCode = System.getenv("PAYMENT_STORE_CODE");
                if (envStoreCode != null && !envStoreCode.isBlank())
                    storeCode = envStoreCode.trim();

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

    public static String getBankId() {
        return bankId;
    }

    public static void setBankId(String bankId) {
        PaymentConfig.bankId = bankId;
    }

    public static String getAccountNo() {
        return accountNo;
    }

    public static void setAccountNo(String accountNo) {
        PaymentConfig.accountNo = accountNo;
    }

    public static String getAccountName() {
        return accountName;
    }

    public static void setAccountName(String accountName) {
        PaymentConfig.accountName = accountName;
    }

    public static String getSepayApiToken() {
        return sepayApiToken;
    }

    public static void setSepayApiToken(String token) {
        PaymentConfig.sepayApiToken = token;
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

    public static void setStoreCode(String storeCode) {
        PaymentConfig.storeCode = storeCode;
    }

    public static boolean isSepayConfigured() {
        return sepayApiToken != null && !sepayApiToken.isBlank()
                && !sepayApiToken.equalsIgnoreCase("YOUR_SEPAY_API_TOKEN_HERE");
    }
}
