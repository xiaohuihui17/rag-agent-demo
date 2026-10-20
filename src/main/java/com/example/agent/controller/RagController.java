package com.example.agent.controller;

import com.example.agent.tool.KnowledgeIngestService;
import com.example.agent.tool.RagChatService;
import com.example.agent.tool.RagMemoryChatService;
import com.example.agent.tool.RetrievalAugmentationChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagController {

    @Autowired
    private KnowledgeIngestService knowledgeIngestService;
    @Autowired
    private RagChatService ragChatService;
    @Autowired
    private RagMemoryChatService ragMemoryChatService;
    @Autowired
    private RetrievalAugmentationChatService retrievalAugmentationChatService;

    /**
     * 把 classpath:knowledge/sample.txt 入库。
     * 实际项目可以改成上传文件、扫描本地目录。
     */
    @GetMapping("/ingest")
    public String ingest() {
        knowledgeIngestService.ingest(new ClassPathResource("knowledge/sample.txt"));
        return "知识库入库完成";
    }

    @GetMapping("/ingestAll")
    public String ingestAll() {
        knowledgeIngestService.ingestAll();
        return "知识库全部入库完成";
    }

    /**
     * 不带记忆的 RAG 问答
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String question) {
        return ragChatService.chat(question);
    }

    /**
     * 带记忆的 RAG 问答
     * 例如：/rag/memory/chat?conversationId=user1&question=刚才说的报销流程是什么
     */
    @GetMapping("/memory/chat")
    public String memoryChat(@RequestParam String conversationId,
                             @RequestParam String question) {
        return ragMemoryChatService.chat(conversationId, question);
    }

    /**
     * 单独测试vectorStore查询结果
     * @param question
     * @return
     */
    @GetMapping("/testVectorStoreResult")
    public String testVectorStoreResult(@RequestParam String question){

        return ragChatService.testVectorStoreResult(question);
    }

    /**
     * 不带记忆的 RAG 问答;
     * 采用RetrievalAugmentationAdvisor
     */
    @GetMapping("/retrievalAugmentationChat")
    public String retrievalAugmentationChat(@RequestParam String question){
        return retrievalAugmentationChatService.chat(question);
    }
}
