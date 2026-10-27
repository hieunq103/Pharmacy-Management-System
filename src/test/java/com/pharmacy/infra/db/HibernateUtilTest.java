package com.pharmacy.infra.db;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HibernateUtilTest {

    @Test
    void sessionTimeZoneFollowsServerTimezoneInUrl() {
        assertEquals("+07:00", HibernateUtil.sessionTimeZoneOffset(
                "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"));
        assertEquals("+07:00", HibernateUtil.sessionTimeZoneOffset(
                "jdbc:mysql://localhost:3306/db?connectionTimeZone=Asia%2FHo_Chi_Minh"));
        assertEquals("+00:00", HibernateUtil.sessionTimeZoneOffset("jdbc:mysql://localhost:3306/db?serverTimezone=UTC"));
    }

    @Test
    void sessionTimeZoneFallsBackToJvmZone() {
        String jvm = ZoneId.systemDefault().getRules().getOffset(Instant.now()).getId();
        String expected = "Z".equals(jvm) ? "+00:00" : jvm;
        assertEquals(expected, HibernateUtil.sessionTimeZoneOffset("jdbc:mysql://localhost:3306/db"));
        assertEquals(expected, HibernateUtil.sessionTimeZoneOffset("jdbc:mysql://localhost:3306/db?serverTimezone=LOCAL"));
        assertEquals(expected, HibernateUtil.sessionTimeZoneOffset("jdbc:mysql://localhost:3306/db?serverTimezone=Not/AZone"));
    }
}
