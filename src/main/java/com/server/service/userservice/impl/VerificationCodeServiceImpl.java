package com.server.service.userservice.impl;

import com.server.dao.user.VerificationCodeDao;
import com.server.dto.request.UserRequestBase;
import com.server.entity.user.VerificationCode;
import com.server.enums.CodeScene;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.service.userservice.VerificationCodeService;
import com.server.util.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    @Autowired
    private VerificationCodeDao verificationCodeDao;

    @Autowired
    private EmailUtil emailUtil;

    private static final String SUBJECT="用户你好,xxx网络优先公司欢迎您";
    private static final String TEXT="\uD83D\uDD10 欢迎注册[xxx网络公司] -" +
            "\n 请使用验证码激活您的账户" +
            "\n 您的验证码为 %s" +
            "\n 验证码5分钟内有效," +
            "\n如果发送验证码的人不是您,请忽略";

    @Override
    @Transactional(value = "mysqlTransactionManager")
    public void sendCode(UserRequestBase userRequestBase){
        boolean type=userRequestBase.isAccountType();
        String account=userRequestBase.getAccount();
        Integer scene=CodeScene.fromCode(userRequestBase.getScene());
        if( account==null ){
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        VerificationCode verificationCode=this.verificationCodeDao.findVerificationCodeByAccount(type,account);
        boolean exists=verificationCode!=null;

        if(verificationCode==null){
            verificationCode=new VerificationCode();
            verificationCode.setAccount(account);
            verificationCode.setType(type);
        }

        String code;
        if(type) {
            code = this.emailUtil.generateCode();
            this.emailUtil.sendSimpleMail(account,SUBJECT,String.format(TEXT,code));
        }else {
            code="";//后续
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        verificationCode.setScene(scene);
        verificationCode.setCode(code);
        verificationCode.setExpired(System.currentTimeMillis()+6*60*1000);//6分钟过期

        if(!exists) this.verificationCodeDao.increaseVerificationCode(verificationCode);
        else this.verificationCodeDao.updateCodeByAccount(verificationCode);

    }
}
