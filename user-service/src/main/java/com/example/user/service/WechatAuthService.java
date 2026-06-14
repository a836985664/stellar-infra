package com.example.user.service;

import com.example.common.security.JwtService;
import com.example.user.dto.*;
import com.example.user.entity.User;
import com.example.user.model.LoginState;
import com.example.user.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信登录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RefreshScope
public class WechatAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    // 微信配置（后续替换为真实配置）
    @Value("${wechat.appid:mock_appid}")
    private String appId;

    @Value("${wechat.secret:mock_secret}")
    private String appSecret;

    @Value("${wechat.redirect-uri:http://192.168.1.10:8082/api/auth/wechat/callback}")
    private String redirectUri;

    @Value("${wechat.mobile-confirm-url:http://192.168.1.10:8082/mobile-confirm.html}")
    private String mobileConfirmUrl;

    // 存储登录状态（开发阶段用内存Map，生产环境用Redis）
    private final Map<String, LoginState> stateMap = new ConcurrentHashMap<>();

    // 二维码有效期（秒）
    private static final long QR_CODE_EXPIRE_TIME = 120;

    /**
     * 生成二维码（PC端扫码登录）
     */
    public QrcodeResponse generateQrcode() {
        String state = UUID.randomUUID().toString();
        long createTime = System.currentTimeMillis();

        // 生成手机端确认登录页面的URL（从Nacos配置读取，支持热更新）
        String qrcodeUrl = mobileConfirmUrl + "?state=" + state;

        // 生成二维码图片（base64）
        String qrcodeImage = generateQrcodeImage(qrcodeUrl);

        // 存储登录状态
        LoginState loginState = new LoginState(state, "pending", null, null, createTime);
        stateMap.put(state, loginState);

        log.info("生成二维码，state: {}", state);

        return new QrcodeResponse(qrcodeUrl, qrcodeImage, state);
    }

    /**
     * 生成二维码图片（base64格式）
     */
    private String generateQrcodeImage(String content) {
        try {
            // 创建二维码生成器

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    300, // 宽度
                    300, // 高度
                    java.util.Map.of(
                            EncodeHintType.CHARACTER_SET, "UTF-8",
                            EncodeHintType.MARGIN, 1
                    )
            );

            // 转换为BufferedImage
            java.awt.image.BufferedImage image = new BufferedImage(
                    bitMatrix.getWidth(),
                    bitMatrix.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_RGB
            );

            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                for (int y = 0; y < bitMatrix.getHeight(); y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // 转换为base64
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            log.error("生成二维码图片失败", e);
            return null;
        }
    }

    /**
     * 查询登录状态（PC端轮询）
     */
    public LoginStatusResponse checkStatus(String state) {
        LoginState loginState = stateMap.get(state);

        if (loginState == null) {
            log.warn("登录状态不存在，state: {}", state);
            return new LoginStatusResponse("expired", null);
        }

        // 检查是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime - loginState.getCreateTime() > QR_CODE_EXPIRE_TIME * 1000) {
            log.info("二维码已过期，state: {}", state);
            stateMap.remove(state);
            return new LoginStatusResponse("expired", null);
        }

        log.info("查询登录状态，state: {}, status: {}", state, loginState.getStatus());

        return new LoginStatusResponse(loginState.getStatus(), loginState.getToken());
    }

    /**
     * 获取微信授权URL（手机端点击登录）
     */
    public AuthUrlResponse getAuthUrl() {
        String state = UUID.randomUUID().toString();
        long createTime = System.currentTimeMillis();

        // 模拟微信授权URL
        String authUrl = "https://mock-wechat.com/connect/oauth2/authorize?" +
                "appid=" + appId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=snsapi_userinfo" +
                "&state=" + state +
                "#wechat_redirect";

        // 存储登录状态
        LoginState loginState = new LoginState(state, "pending", null, null, createTime);
        stateMap.put(state, loginState);

        log.info("生成微信授权URL，state: {}", state);

        return new AuthUrlResponse(authUrl);
    }

    /**
     * 处理微信回调（两种登录方式都用）
     */
    public LoginResponse handleCallback(String code, String state) {
        log.info("处理微信回调，code: {}, state: {}", code, state);

        // 模拟：用code换取openid（真实场景调用微信API）
        String mockOpenid = "mock_openid_" + code;

        // 查询或创建用户
        User user = findOrCreateByOpenid(mockOpenid);

        // 生成JWT
        String token = jwtService.genreateToken(new com.example.common.security.User(user.getId(), user.getUsername(), user.getRole()));

        // 更新登录状态
        LoginState loginState = stateMap.get(state);
        if (loginState != null) {
            loginState.setStatus("success");
            loginState.setToken(token);
            loginState.setOpenid(mockOpenid);
        }

        log.info("登录成功，userId: {}, openid: {}", user.getId(), mockOpenid);

        // 返回登录响应
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        );

        return new LoginResponse(token, userInfo);
    }

    /**
     * 模拟扫码（测试用）
     */
    public String mockScan(String state) {
        log.info("模拟扫码，state: {}", state);

        LoginState loginState = stateMap.get(state);
        if (loginState == null) {
            throw new RuntimeException("State不存在或已过期");
        }

        // 检查是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime - loginState.getCreateTime() > QR_CODE_EXPIRE_TIME * 1000) {
            stateMap.remove(state);
            throw new RuntimeException("二维码已过期");
        }

        // 模拟生成openid
        String mockOpenid = "mock_openid_" + state;

        // 查询或创建用户
        User user = findOrCreateByOpenid(mockOpenid);

        // 生成JWT
        String token = jwtService.genreateToken(new com.example.common.security.User(user.getId(), user.getUsername(), user.getRole()));

        // 更新登录状态
        loginState.setStatus("success");
        loginState.setToken(token);
        loginState.setOpenid(mockOpenid);

        log.info("模拟扫码成功，userId: {}, openid: {}", user.getId(), mockOpenid);

        return token;
    }

    /**
     * 根据openid查询或创建用户
     */
    private User findOrCreateByOpenid(String openid) {
        // 查询用户
        User user = userRepository.findByOpenid(openid);

        if (user != null) {
            log.info("用户已存在，userId: {}, openid: {}", user.getId(), openid);
            return user;
        }

        // 创建新用户
        user = new User();
        user.setOpenid(openid);
        user.setUsername("微信用户_" + openid.substring(0, 8));
        user.setNickname("微信用户");
        user.setAvatar("https://mock-avatar.com/default.png");
        user.setRole("USER");

        user = userRepository.save(user);

        log.info("创建新用户，userId: {}, openid: {}", user.getId(), openid);

        return user;
    }

    /**
     * 清理过期状态（定时任务调用）
     */
    public void cleanExpiredStates() {
        long currentTime = System.currentTimeMillis();
        long expireTime = QR_CODE_EXPIRE_TIME * 1000;

        stateMap.entrySet().removeIf(entry -> {
            boolean expired = currentTime - entry.getValue().getCreateTime() > expireTime;
            if (expired) {
                log.info("清理过期状态，state: {}", entry.getKey());
            }
            return expired;
        });
    }
}
