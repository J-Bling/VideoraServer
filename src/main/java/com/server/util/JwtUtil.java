package com.server.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonObjectBuilder;
import java.io.StringReader;
import java.io.StringWriter;

public class JwtUtil {


    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String SECRET="0bzd9X7Rx55-40X5Uxj7zo0FuEwBmi1T";
    private static final long EXPIRE=3*24*60*60*1000;



    private static String encodeJson(Map<String, Object> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                builder.add(entry.getKey(),value.toString());
            } else if (value instanceof Integer) {
                builder.add(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                builder.add(entry.getKey(), (Long) value);
            } else if (value instanceof Double) {
                builder.add(entry.getKey(), (Double) value);
            } else if (value instanceof Boolean) {
                builder.add(entry.getKey(), (Boolean) value);
            } else {
                builder.add(entry.getKey(), value.toString());
            }
        }

        JsonObject jsonObject = builder.build();
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }

        return BASE64_URL_ENCODER.encodeToString(
                stringWriter.toString().getBytes(StandardCharsets.UTF_8));
    }


    private static Map<String, Object> decodeJson(String encoded) {
        byte[] decodedBytes = BASE64_URL_DECODER.decode(encoded);
        String jsonStr = new String(decodedBytes, StandardCharsets.UTF_8);

        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonStr))) {
            JsonObject jsonObject = jsonReader.readObject();
            Map<String, Object> map = new HashMap<>();

            for (String key : jsonObject.keySet()) {
                map.put(key, jsonObject.get(key));
            }
            return map;
        }
    }

    private static String calculateSignature(String encodedHeader, String encodedPayload) {
        try {
            Mac sha256_HMAC = Mac.getInstance(ALGORITHM);
            SecretKeySpec secret_key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            sha256_HMAC.init(secret_key);

            String data = encodedHeader + "." + encodedPayload;
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL_ENCODER.encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    public static String generateToken(Integer subject, long expiration) {
        if(subject==null) return null;
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", subject);
        payload.put("iat", new Date().getTime() / 1000);
        payload.put("exp", (new Date().getTime() + EXPIRE) / 1000);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);

        String signature = calculateSignature(encodedHeader, encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }


    public static String validateAndGetToken(String token) {
        try {
            if(token==null) return null;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String signature = parts[2];

            String expectedSignature = calculateSignature(encodedHeader, encodedPayload);
            if (!signature.equals(expectedSignature)) {
                return null;
            }

            Map<String, Object> payload = decodeJson(encodedPayload);
            long exp = Integer.parseInt(payload.get("exp").toString());
            if(exp > System.currentTimeMillis() / 1000){
                return payload.get("sub").toString();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
