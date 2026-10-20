package com.example.agent.config;

import com.example.agent.tool.ToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * 多轮记忆：内存滑动窗口，最多保留 20 条消息，超出丢最旧的。
     * 生产环境可换成 Redis/DB 持久化实现（服务重启记忆不丢）。
     * 底层存的是一条条 Message（用户消息 + 助手回复都存），
     * 每次请求框架会把该会话的历史消息自动拼进 prompt。
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    /**
     * 关键一步：构建 ChatClient 时把 ToolService 注册为"工具集"。
     * 之后模型看到用户请求，会自己决定调用这些 Java 方法（Function Calling）。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolService toolService, ChatMemory chatMemory) {
        return builder
                .defaultSystem("你是一个乐于助人的 Java 助手。需要具体数据时，请优先调用可用工具获取，不要编造。")
                .defaultTools(toolService)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }




}
