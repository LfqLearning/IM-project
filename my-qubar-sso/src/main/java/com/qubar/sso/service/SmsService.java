// This file is auto-generated, don't edit it. Thanks.
package com.qubar.sso.service;

import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.qubar.sso.constant.AliyunAccess;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {

    public static final String REDIS_KEY_PREFIX = "CHECK_CODE_";

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 使用AK&SK初始化账号Client
     *
     * @param accessKeyId
     * @param accessKeySecret
     * @return Client
     * @throws Exception
     */
    // 生成一个Client对象
    public static com.aliyun.dysmsapi20170525.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，您的 AccessKey ID
                .setAccessKeyId(accessKeyId)
                // 必填，您的 AccessKey Secret
                .setAccessKeySecret(accessKeySecret);
        // 访问的域名
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new com.aliyun.dysmsapi20170525.Client(config);
    }

    /**
     * 向aliyun短信服务器发送短信请求
     *
     * @param mobile
     * @return 发送成功返回验证码，发送失败返回null
     * @throws Exception
     */
    // 向aliyun短信服务器发送————发送短信请求
    public static String sendSms(String mobile) throws Exception {

        // java.util.List<String> args = java.util.Arrays.asList(args_);
        java.util.List<String> args = new ArrayList<>();
        // 工程代码泄露可能会导致AccessKey泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议使用更安全的 STS 方式，更多鉴权访问方式请参见：https://help.aliyun.com/document_detail/378657.html
        com.aliyun.dysmsapi20170525.Client client = SmsService.createClient(AliyunAccess.accessKeyId, AliyunAccess.accessKeySecret);
        com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                .setSignName("趣吧交友")
                .setTemplateCode("SMS_275345230")
                .setPhoneNumbers(mobile);
        // 设置短信验证码
        //sendSmsRequest.setTemplateParam("{\"code\":\"1234\"}");
        String randomNumber = String.valueOf(RandomUtils.nextInt(100000, 999999));
        String captcha = "{\"code\":\"" + randomNumber + "\"}";
        sendSmsRequest.setTemplateParam(captcha);
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            // Client发送消息给aliyun服务器
            SendSmsResponse sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, runtime);
           /* System.out.println("错误码：" + sendSmsResponse.getBody().getCode());
            System.out.println("错误信息：" + sendSmsResponse.getBody().getMessage());
            System.out.println("请求头：" + sendSmsResponse.getHeaders());
            System.out.println("HTTP请求状态码：" + sendSmsResponse.getStatusCode());*/
            // 返回生成的密码 String类型
            if (StringUtils.equals(sendSmsResponse.getBody().getCode(), "OK")) {
                return randomNumber;
            }
        } catch (TeaException error) {
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        }
        return null;
    }

    /**
     * 发送验证码
     *
     * @param mobile
     * @return
     */
    // 发送验证码并从验证redis中是否已存在验证码
    public Map<String, Object> sendCheckCode(String mobile) {
        Map<String, Object> result = new HashMap<>(2);
        try {
            String redisKey = REDIS_KEY_PREFIX + mobile;
            // 获取验证码
            String value = this.redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isNotEmpty(value)) {
                result.put("code", 1);
                result.put("msg", "上一次发送的验证码还未失效");
                return result;
            }
/*
模拟发送短信——方便调试
*/
            // TODO 做了测试修改，验证码实际不会发出，后面修正
            //String code = this.sendSms(mobile);
            String code = "123456";// 测试用，模拟验证码为123456
            if (null == code) {
                result.put("code", 2);
                result.put("msg", "发送短信验证码失败");
                return result;
            }
            // 发送验证码成功
            result.put("code", 3);
            result.put("msg", "ok");
            // 将验证码存储到Redis,1分钟后失效
            this.redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(1));
            return result;
        } catch (Exception e) {
            LOGGER.error("发送验证码出错！" + mobile, e);
            result.put("code", 4);
            result.put("msg", "发送验证码出现异常");
            return result;
        }
    }
}