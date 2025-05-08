package com.server.controller.api.publics.auth;

import com.server.dao.user.VerificationCodeDao;
import com.server.dto.request.UserRequestBase;
import com.server.dto.request.auth.AuthRequest;
import com.server.dto.response.Result;
import com.server.entity.user.User;
import com.server.enums.CodeScene;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.exception.AuthException;
import com.server.service.userservice.UserService;
import com.server.service.userservice.VerificationCodeService;
import com.server.util.JwtUtil;
import com.server.util.PasswordUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/auth")
@Tag(name = "用户身份验证类")
public class Auth {

    @Autowired
    private UserService userService;
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private VerificationCodeDao verificationCodeDao;

    private static final Logger logger= LoggerFactory.getLogger(Auth.class);

    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    // 中国手机号正则（支持13/14/15/16/17/18/19开头）
    private static final String CHINA_PHONE_REGEX = "^1[3-9]\\d{9}$";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern CHINA_PHONE_PATTERN = Pattern.compile(CHINA_PHONE_REGEX);


    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidChinaPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return CHINA_PHONE_PATTERN.matcher(phone).matches();
    }


    @PostMapping("/login-password")
    @Operation(summary = "密码登陆",description = "使用 phone/email + password 进行登陆 ; 成功返回用户数据")
    public ResponseEntity<Result> loginByPassword(@RequestBody AuthRequest authRequest){
        try {
            User user =userService.loginByPassGetUser(authRequest);

            if(user==null) return Result.ErrorResult(ErrorCode.UNAUTHORIZED,"该账户没有注册");
            boolean isVailPass= PasswordUtil.verify(authRequest.getPassword(),user.getSalt(),user.getPassword());
            if(!isVailPass){
                return Result.ErrorResult(ErrorCode.BAD_REQUEST,"密码错误");
            }

            String token = JwtUtil.generateToken(user.getId(),1);
            user.setPassword("");
            user.setSalt("");
            Map<String,Object> response=new HashMap<>();
            response.put("token",token);
            response.put("user",user);

            return Result.Ok(response);
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),"参数出错,请填入完整信息登陆");
        }catch(AuthException authException){
            return Result.ErrorResult(ErrorCode.BAD_REQUEST, authException.getMessage());
        }catch (Exception e){
            logger.error("loginByPass fail : reason is {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,"服务器出错");
        }
    }

    @PostMapping("/login-verification-code")
    @Operation(summary = "使用验证码登陆帐号",description = "使用email/phone + code登陆 ;成功返回用户数据")
    public ResponseEntity<Result> loginByVerificationCode(@RequestBody AuthRequest authRequest){
        try{
            User user= this.userService.loginByCodeGetUser(authRequest);
            if(user==null) return Result.ErrorResult(ErrorCode.UNAUTHORIZED,"该账户没有注册");

            boolean isVailPass = PasswordUtil.verify(authRequest.getPassword(),user.getSalt(),user.getPassword());
            if(!isVailPass){
                return Result.ErrorResult(ErrorCode.BAD_REQUEST,"密码错误");
            }

            String token=JwtUtil.generateToken(user.getId(),1);
            Map<String,Object> response=new HashMap<>();
            user.setPassword("");
            user.setSalt("");
            response.put("user",user);
            response.put("token",token);

            return Result.Ok(response);
        }catch(AuthException authException){
            return Result.ErrorResult(ErrorCode.BAD_REQUEST,authException.getMessage());
        }catch (Exception e){
            logger.error("login at verification-code fail  reason is {} ",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,"服务器错误");
        }
    }


    @PostMapping("/send-code")
    @Operation(summary = "发送验证码" ,description = "需要格式 account accountType scene")
    public ResponseEntity<Result> sendCode(@RequestBody UserRequestBase userRequestBase){
        try{
            boolean isVail = userRequestBase.isAccountType() ? isValidEmail(userRequestBase.getAccount())
                    : isValidChinaPhone(userRequestBase.getAccount());

            if(!isVail) return Result.ErrorResult(ErrorCode.BAD_REQUEST,"账号不符合规范");

            this.verificationCodeService.sendCode(userRequestBase);

            return Result.Ok("succeed");
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),"请填入帐号");
        }catch(AuthException authException){
            return Result.ErrorResult(ErrorCode.BAD_REQUEST,authException.getMessage());
        }catch (Exception e){
            logger.error("send-code fail : reason is {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,"服务器出错");
        }
    }


    @PostMapping("/register")
    @Operation(summary = "用户注册" ,description = "需要 email/phone , code , nickname , password")
    public ResponseEntity<Result> register(@RequestBody AuthRequest authRequest){
        try{
            if((authRequest.getEmail() == null && authRequest.getPhone()==null) || authRequest.getCode()==null){
                return Result.ErrorResult(ErrorCode.BAD_REQUEST,"参数不完整");
            }

            String account = authRequest.getEmail()!=null ? authRequest.getEmail() : authRequest.getPhone();
            boolean type = authRequest.getEmail()!=null;
            boolean isVail= verificationCodeDao.verifyCodeExists(account,type,authRequest.getCode(),CodeScene.REGISTER.getCode(),System.currentTimeMillis());
            if (!isVail) return Result.ErrorResult(ErrorCode.BAD_REQUEST,"验证码无效");

            Integer id= userService.createUser(authRequest);
            String token=JwtUtil.generateToken(id,1);

            Map<String,String> result = new HashMap<>();
            result.put("token",token);
            result.put("id",id.toString());
            return Result.Ok(result);
        }catch (ApiException apiException){
            if(apiException.getErrorCode().getStatusCode()==400)
                return Result.ErrorResult(apiException.getErrorCode(),0);
            return Result.ErrorResult(apiException.getErrorCode(),"该帐号已经注册过,请登陆");
        }catch(AuthException authException){
            return Result.ErrorResult(ErrorCode.BAD_REQUEST,authException.getMessage());
        }catch (Exception e){
            logger.error("register fail reason is {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,"register fail");
        }
    }
}
