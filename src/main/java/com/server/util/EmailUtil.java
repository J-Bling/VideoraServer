package com.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.CountDownLatch;


@Component
public class EmailUtil {
    @Value("${spring.mail.username}")
    private String FROM;

    private static final Logger logger = LoggerFactory.getLogger(EmailUtil.class);

    private static final String ALLOWED_CHARS = "0123456789";
    private static final Random RANDOM = new SecureRandom(); // 使用安全的随机数生成器
    private static final int LENGTH=6;

    @Autowired
    private JavaMailSender mailSender;

    public String generateCode(){
        StringBuilder stringBuilder = new StringBuilder(LENGTH);
        for(int i=0;i<LENGTH;i++){
            int randomIndex=RANDOM.nextInt(ALLOWED_CHARS.length());
            stringBuilder.append(ALLOWED_CHARS.charAt(randomIndex));
        }
        return stringBuilder.toString();
    }

    @Async
    public void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        try {
            this.mailSender.send(message);
            logger.info("邮件发送成功 , {}", to);
        } catch (Exception e) {
            logger.error("发送邮件时发生异常", e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Async
    public void send(String to, CountDownLatch latch){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM);
        message.setTo(to);
        message.setSubject("测试邮件");
        message.setText("邮件码 "+this.generateCode());
        try{
            this.mailSender.send(message);
        } catch (MailException e) {
            throw new RuntimeException(e);
        }finally {
            latch.countDown();
        }
    }

    @Async
    public void sendHtmlMail(String to, String subject, String content) {
        MimeMessage message = this.mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(FROM);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            this.mailSender.send(message);
            logger.info("HTML邮件已发送至 {}", to);
        } catch (MessagingException e) {
            logger.error("发送HTML邮件时发生异常", e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 发送带附件的邮件
     */
    @Async
    public void sendAttachmentMail(String to, String subject, String content, String filePath) {
        MimeMessage message = this.mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(FROM);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);

            File file = new File(filePath);
            helper.addAttachment(file.getName(), file);

            this.mailSender.send(message);
            logger.info("带附件的邮件已发送至 {}", to);
        } catch (MessagingException e) {
            logger.error("发送带附件邮件时发生异常", e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
