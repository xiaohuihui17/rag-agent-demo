package com.example.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * 采用RetrievalAugmentationAdvisor打印实际检索出的内容，排查同样的请求为啥2次结果不一样
 */
@Service
@Slf4j
public class RetrievalAugmentationChatService {

    private final ChatClient chatClient;

    public RetrievalAugmentationChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ToolService toolService) {
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                // 指定用 VectorStore 做相似度检索
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(5)
                                .similarityThreshold(0.5)
                                .build()
                )
                // 关键：打印每次实际检索到的文档
                .documentPostProcessors((query, documents) -> {
                    log.info("RAG 实际检索 query: {}", query.text());
                    log.info("RAG 命中条数: {}", documents.size());
                    for (int i = 0; i < documents.size(); i++) {
                        Document doc = documents.get(i);
                        log.info("RAG chunk[{}] score={} text={}",
                                i,
                                doc.getScore(),
                                doc.getText() != null ? doc.getText().substring(0, Math.min(300, doc.getText().length())) : "(null)");
                    }
                    return documents;
                })
                .order(10)
                .build();


        // 给 ChatClient 挂上 RAG Advisor
        // 每次用户提问时，它会先去向量库检索，再把检索结果拼进 prompt
        this.chatClient = chatClientBuilder
                .defaultSystem("优先根据本轮检索到的参考资料回答。\n" +
                        "如果资料不足，可以补充少量常识，但必须明确说明“以下为常识补充，未来自知识库”。")
                .defaultTools(toolService)
                .defaultAdvisors(SimpleLoggerAdvisor.builder().order(1).build(), ragAdvisor)
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


}
