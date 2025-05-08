package com.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.util.Random;

public class PhoneUtil {
    private static final Logger logger = LoggerFactory.getLogger(PhoneUtil.class);

    private static final String ALLOWED_CHARS = "0123456789";
    private static final Random RANDOM = new SecureRandom(); // 使用安全的随机数生成器
    private static final int LENGTH=6;

    public String generateCode(){
        StringBuilder stringBuilder = new StringBuilder(LENGTH);
        for(int i=0;i<LENGTH;i++){
            int randomIndex=RANDOM.nextInt(ALLOWED_CHARS.length());
            stringBuilder.append(ALLOWED_CHARS.charAt(randomIndex));
        }
        return stringBuilder.toString();
    }


}
