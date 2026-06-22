package com.kavindu.techmart.ejb.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PayHereUtil {

    private PayHereUtil() {
    }

    public static String checkoutHash(String merchantId, String orderId, String amountFormatted,
                                      String currency, String merchantSecret) {
        String secretMd5 = upperMd5(merchantSecret);
        return upperMd5(merchantId + orderId + amountFormatted + currency + secretMd5);
    }

    public static String localNotifySig(String merchantId, String orderId, String payhereAmount,
                                        String payhereCurrency, String statusCode, String merchantSecret) {
        String secretMd5 = upperMd5(merchantSecret);
        return upperMd5(merchantId + orderId + payhereAmount + payhereCurrency + statusCode + secretMd5);
    }

    public static boolean verifyNotifySig(String merchantId, String orderId, String payhereAmount,
                                          String payhereCurrency, String statusCode,
                                          String merchantSecret, String receivedSig) {
        if (receivedSig == null) {
            return false;
        }
        String local = localNotifySig(merchantId, orderId, payhereAmount, payhereCurrency,
                statusCode, merchantSecret);
        return MessageDigest.isEqual(
                local.getBytes(StandardCharsets.UTF_8),
                receivedSig.toUpperCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String upperMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input == null ? new byte[0] : input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
