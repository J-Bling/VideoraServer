package com.server.dao.user;

import com.server.entity.user.VerificationCode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VerificationCodeDao {

    VerificationCode findVerificationCodeByAccount(@Param("type") boolean type, @Param("account") String account);

    @Select("select count(*) from verificationcode where account=#{account} and type=#{type} and code=#{code} and scene=#{scene} and expired>#{expired} limit 1")
    Boolean verifyCodeExists(@Param("account") String account,@Param("type") boolean type,@Param("code") String code,@Param("scene") Integer scene,@Param("expired") long expired);

    void increaseVerificationCode(VerificationCode verificationCode);
    void updateCodeByAccount(VerificationCode verificationCode);
}
