package com.kavindu.techmart.ejb.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    @DisplayName("hash is deterministic for the same salt and verifies correctly")
    void hashAndVerify() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash("secret123", salt);
        assertNotNull(hash);
        assertEquals(hash, PasswordUtil.hash("secret123", salt));
        assertTrue(PasswordUtil.verify("secret123", salt, hash));
        assertFalse(PasswordUtil.verify("wrongpass", salt, hash));
    }

    @Test
    @DisplayName("different salts produce different hashes for the same password")
    void differentSaltsDifferentHashes() {
        String s1 = PasswordUtil.generateSalt();
        String s2 = PasswordUtil.generateSalt();
        assertNotEquals(s1, s2);
        assertNotEquals(PasswordUtil.hash("samePassword", s1), PasswordUtil.hash("samePassword", s2));
    }

    @Test
    @DisplayName("generateToken returns a non-empty unique token")
    void generateToken() {
        String t1 = PasswordUtil.generateToken();
        String t2 = PasswordUtil.generateToken();
        assertNotNull(t1);
        assertTrue(t1.length() > 20);
        assertNotEquals(t1, t2);
    }
}
