package com.server.util;

import com.server.enums.ErrorCode;
import com.server.exception.ApiException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    private static final int SALT_LENGTH = 16;

    private static final String ALGORITHM = "MD5";

    //生成salt
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * MD5加盐加密
     */
    public static String encrypt(String password, String salt) {
        try {
            if (password==null || password.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST);
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            // 将盐值加入密码
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 验证密码
     * @param inputPassword 用户输入的密码
     * @param salt 存储的盐值
     * @param storedPassword 存储的加密密码
     * @return 是否匹配
     */
    public static boolean verify(String inputPassword, String salt, String storedPassword) {
        String encryptedInput = encrypt(inputPassword, salt);
        return encryptedInput.equals(storedPassword);
    }
}
