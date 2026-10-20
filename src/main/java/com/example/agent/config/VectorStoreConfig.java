package com.example.agent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 使用内存向量库，适合本地 16G 环境快速验证
        // SimpleVectorStore 会把向量和文档存在内存里
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        // 尝试从本地文件加载之前保存的向量数据，避免每次重启都重新入库
        File file = new File("src/main/resources/knowledge/local_vector_store.json");
        if (file.exists() && file.length() > 0) {
            store.load(file);
        }
        return store;
    }

    @Bean
    public ApplicationListener<ContextClosedEvent> vectorStoreSaver(VectorStore vectorStore) {
        return event -> {
            if (vectorStore instanceof SimpleVectorStore s) {
                s.save(new File("src/main/resources/knowledge/local_vector_store.json"));
            }
        };
    }


}
