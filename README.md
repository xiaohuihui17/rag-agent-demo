# rag-agent-demo：Java + Spring AI + Ollama 最小 rag+Agent（零数据库）

## 技术栈
- JDK 17 + Spring Boot 3.5.6 + spring-ai-bom（管理 Spring AI 相关依赖的版本）
- Spring AI 1.1.7（`spring-ai-starter-model-ollama`）
- Ollama 本地模型（qwen2.5:7b、nomic-embed-text:latest ）

一个**纯内存、零数据库**的 Java AI Agent 示例：
1、大模型（本地 Ollama）根据用户的话， 自动决定调用服务内 Java 方法（`@Tool`）
--这就是 Function Calling / Agent 的最小闭环。
| 工具 | 方法 | 示例提问 |
|---|---|---|
| 查当前时间 | `getCurrentTime()` | "现在几点了？" |
| 整数加法 | `add(a, b)` | "帮我算 12345 + 67890" |
| 查天气（写死数据） | `getWeather(city)` | "武汉今天天气怎么样？" |
| 待办清单（内存 Map） | `addTodo / listTodos` | "记一条待办：周五上午九点答题，然后看看待办清单" |

# 带会话的agent调用服务方法测试用例
POST http://localhost:18082/chat/chat
Content-Type: application/json
{"message":"武汉天气怎么样？顺便记一条待办：晚上 8 点复习 JVM，然后列出待办清单"}

2、rag查询，采用SimpleVectorStore作为向量仓库
#1 读取本地文件初始化vectorStore
get http://localhost:18082/rag/ingestAll
#2 不带会话的rag检索
get http://localhost:18082/rag/chat?question=武汉旅游
#3 带会话的rag检索
get http://localhost:18082/rag/memory/chat?question=武汉旅游&conversationId=user-001
#4 当同样的用户输入，但2次返回结果不一样时
--不用会话
--配置temperature从0.2改成0.0，避免自由发挥
--提示词修改，尽量返回结果表述精确
优先根据本轮检索到的参考资料回答。如果资料不足，可以补充少量常识，但必须明确说明“以下为常识补充，未来自知识库”。
--主动调用vectorStore检索，判断仓库查询前后是否一致
get http://localhost:18082/rag/testVectorStoreResult?question=武汉旅游
--从QuestionAnswerAdvisor切换成RetrievalAugmentationAdvisor，documentPostProcessors方法获取advisor链中查询结果
get http://localhost:18082/rag/retrievalAugmentationChat?question=武汉旅游

