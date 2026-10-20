package com.example.agent.tool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagMemoryChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public RagMemoryChatService(ChatClient.Builder chatClientBuilder,
                                VectorStore vectorStore) {
        // 使用内存会话记忆，按 conversationId 区分不同会话
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(6) // 窗口大小：保留最近 20 条消息，避免 16G 内存吃太满
                .build();

        // RAG 检索 Advisor：负责从向量库检索知识库内容
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                .topK(3)
                                .similarityThreshold(0.5)
                                .build()
                )
                .build();

        // 记忆 Advisor：负责自动携带历史对话上下文
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        this.chatClient = chatClientBuilder
                .defaultSystem("优先根据本轮检索到的参考资料回答。\n" +
                        "如果资料不足，可以补充少量常识，但必须明确说明“以下为常识补充，未来自知识库”。")
                .defaultAdvisors(qaAdvisor, memoryAdvisor)
                .build();
    }

    /**
     * 带会话记忆的问答。
     * conversationId 相同，就会保留上下文。
     */
    public String chat(String conversationId, String question) {
        List<Message> list = chatMemory.get(conversationId);
        String msg = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(question)
                .call()
                .content();
        return msg;
    }
}