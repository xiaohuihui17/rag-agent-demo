package com.example.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RagChatService {

    private final ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

    public RagChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        // 给 ChatClient 挂上 RAG Advisor
        // 每次用户提问时，它会先去向量库检索，再把检索结果拼进 prompt
        this.chatClient = chatClientBuilder
                .defaultSystem("优先根据本轮检索到的参考资料回答。\n" +
                        "如果资料不足，可以补充少量常识，但必须明确说明“以下为常识补充，未来自知识库”。")
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(5)                    // 每次检索返回最相关的 5 条
                                                .similarityThreshold(0.5) // 相似度低于 0.5 的片段会被过滤
                                                .build()
                                )
                                .order(10)
                                .build(),
                        SimpleLoggerAdvisor.builder().order(0).build()
                )
                .build();
    }

    /**
     * 普通同步问答
     */
    public String chat(String question) {
        String msg =  chatClient.prompt()
                .user(question)
                .call()
                .content();
        return msg;
    }

    public String testVectorStoreResult(String question) {
        List<Document> docs1 = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .similarityThreshold(0.5)
                        .build()
        );
        StringBuilder sb = new StringBuilder();
        log.info("question="+question);
        for (int i = 0; i < docs1.size(); i++) {
            String str = "检索["+i+"] score="+docs1.get(i).getScore()+" text="+docs1.get(i).getText()+"\n";
            sb.append(str);
            log.info(str);
        }
        return sb.toString();
    }
}
