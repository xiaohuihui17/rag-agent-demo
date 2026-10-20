package com.example.agent.tool;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class KnowledgeIngestService {

    private final VectorStore vectorStore;

    public KnowledgeIngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 读取文本文件，切块后写入向量库。
     * VectorStore.add() 会自动调用 EmbeddingModel 生成向量，不需要手动 embedding。
     */
    public void ingest(Resource resource) {

        // 读取文本文件，生成 Document 列表
        TextReader reader = new TextReader(resource);
        List<Document> documents = reader.get();

        // 按 Token 切块，避免一次塞入过长文本
        // Spring AI 1.1.x 使用构造函数配置，不使用 builder().withChunkOverlap()
        // 参数顺序：chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator
        TokenTextSplitter splitter = new TokenTextSplitter(
                10,    // 每个块目标 token 数，中文场景可以适当调小
                5,    // 最小字符数，小于这个值会尝试合并，避免碎块
                1,      // 太短的块直接丢弃
                10000,  // 单文档最多切多少块
                true,    // 保留换行符等分隔符
                List.of('。', '？', '！', '；', '\n')       // 中文标点 + 换行符
        );

        // 对文档列表执行切块，得到更小的 Document 列表
        List<Document> chunks = splitter.apply(documents);

        // 写入向量库：自动 embedding + 存储
        vectorStore.add(chunks);

        if(vectorStore instanceof SimpleVectorStore s){
            //SimpleVectorStore有save方法  // 入库后立即持久化
            s.save(new File("src/main/resources/knowledge/local_vector_store.json"));
        }

    }

    public void ingestAll() {
        // 多个文件，逐个读入
        String[] files = {
                "src/main/resources/knowledge/产品使用指南.txt",
                "src/main/resources/knowledge/人工智能发展简史.txt",
                "src/main/resources/knowledge/武汉旅游指南.txt"
        };

        // 按 Token 切块，避免一次塞入过长文本
        // Spring AI 1.1.x 使用构造函数配置，不使用 builder().withChunkOverlap()
        // 参数顺序：chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator
        TokenTextSplitter splitter = new TokenTextSplitter(
                300,    // 每个块目标 token 数，中文场景可以适当调小
                100,    // 最小字符数，小于这个值会尝试合并，避免碎块
                5,      // 太短的块直接丢弃
                10000,  // 单文档最多切多少块
                true,    // 保留换行符等分隔符
                List.of('。', '？', '！', '；', '\n')       // 中文标点 + 换行符
        );

        for (String path : files) {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("跳过不存在文件：" + path);
                continue;
            }
            // 读取文本文件，生成 Document 列表
            TextReader reader = new TextReader(new FileSystemResource(file));
            reader.setCharset(StandardCharsets.UTF_8);
            List<Document> documents = reader.get();
            // 对文档列表执行切块，得到更小的 Document 列表
            List<Document> chunks = splitter.apply(documents);

            // 写入向量库：自动 embedding + 存储
            vectorStore.add(chunks);
            System.out.println("已追加：" + file.getName() + "，共 " + chunks.size() + " 块");
        }

        if(vectorStore instanceof SimpleVectorStore s){
            //SimpleVectorStore有save方法  // 入库后立即持久化
            s.save(new File("src/main/resources/knowledge/local_vector_store.json"));
        }
    }


}
