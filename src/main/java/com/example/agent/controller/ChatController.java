package com.example.agent.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    /** 没传 conversationId 时的默认会话（单用户 demo 够用） */
    private static final String DEFAULT_SESSION = "default-session";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatController(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * 对话入口：用户一句话，模型自动决定要不要调用 ToolService 里的 Java 方法。
     * 带多轮记忆：同一 conversationId 的对话，模型"记得"之前聊过什么。
     * 示例请求：
     *   {"message":"现在几点了？"}
     *   {"message":"帮我算一下 12345 + 67890"}
     *   {"message":"记一条待办：周五上午九点答题"}
     *   {"message":"现在有几条待办？","conversationId":"user-001"}   ← 带上会话 ID 才有记忆
     */
    @PostMapping("/chat")
    public Map<String, String> memoryChat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String conversationId = body.getOrDefault("conversationId", DEFAULT_SESSION);
        String answer = chatClient.prompt()
                .user(message)
                // 把"会话标识"告诉记忆顾问：按 conversationId 存取该会话的历史消息
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return Map.of("message", message, "answer", answer);
    }
}
