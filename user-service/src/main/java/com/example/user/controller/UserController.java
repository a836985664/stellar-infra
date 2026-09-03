package com.example.user.controller;

import com.example.user.annotation.OperationLog;
import com.example.user.entity.ChatMessage;
import com.example.user.entity.User;
import com.example.user.repository.ChatMessageRepository;
import com.example.user.repository.UserRepository;
import com.example.user.service.AiAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AiAssistantService aiAssistantService;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    Map<Long, User> userMap = new HashMap<>();

    @OperationLog(value = "创建用户", module = "用户管理", saveParams = false)
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @GetMapping("/test")
    public String test() {
        return "用户服务正常运行中！";
    }

    @PostMapping("/chat")
    public String chatWithAi(@RequestBody ChatRequest request) {
        String aiReply = aiAssistantService.chatWithAssistant(request.message(), request.id());
        
        // 保存 AI 的回复到数据库
            if (!aiReply.contains("__TRANSFER_TO_HUMAN__")) {
            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setSessionId(request.sessionId());
            aiMsg.setFromUser("AI Assistant");
            // 注意：这里要和 WebSocket 里的 username 保持逻辑一致
            // 如果前端传的是 1，这里可能需要根据 ID 去查用户名，或者前端直接传 username
            aiMsg.setToUser("访客01"); 
            aiMsg.setContent(aiReply);
            aiMsg.setAi(true);
            aiMsg.setSenderRole("AI");
            chatMessageRepository.save(aiMsg);
        }
        
        return aiReply;
    }

    public record ChatRequest(String message, Long id, Long sessionId) {}
}