package com.server.entity.user;
import lombok.Data;

import java.sql.Timestamp;


@Data
public class VerificationCode {
    private Integer id;
    private String account;
    private String code;
    /**
     * true email;false phone
     */
    private Boolean type;
    private Integer scene;//使用场景
    private Long expired;
    private Timestamp created;

    public VerificationCode(){}
}
