package com.example.agent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 暴露给大模型的"手"——每个 @Tool 方法都会被 LLM 自动识别并择机调用。
 * 全部纯内存实现，零数据库。
 */
@Service
public class ToolService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 写死的城市天气示例数据（只是为了演示，不用真实 API） */
    private static final Map<String, String> WEATHER = Map.of(
            "武汉", "晴 26°C 东南风2级",
            "宁德", "多云 24°C 东风3级",
            "北京", "阴 18°C 北风4级",
            "上海", "小雨 22°C 南风2级"
    );

    /** 内存待办清单（进程内有效，重启即清空） */
    private final List<String> todos = new ArrayList<>();

    /**
     * 获取当前系统时间。
     */
    @Tool(name = "getCurrentTime", description = "获取当前系统时间，返回格式 yyyy-MM-dd HH:mm:ss")
    public String getCurrentTime() {
        System.out.println("[Tool调用] getCurrentTime()");
        String result = LocalDateTime.now().format(FMT);
        System.out.println("[Tool返回] getCurrentTime -> " + result);
        return result;
    }

    /**
     * 计算两个整数相加。
     */
    @Tool(name = "add", description = "计算两个整数 a + b 的和，返回整数结果")
    public String add(@ToolParam(description = "第一个加数") Integer a,
                      @ToolParam(description = "第二个加数") Integer b) {
        System.out.println("[Tool调用] add(a=" + a + ", b=" + b + ")");
        if (a == null || b == null) {
            String err = "参数不完整（a=" + a + ", b=" + b + "），请告诉我两个数字，比如：123 + 456";
            System.out.println("[Tool返回] add -> " + err);
            return err;
        }
        String result = String.valueOf(a + b);
        System.out.println("[Tool返回] add -> " + result);
        return result;
    }

    /**
     * 查询城市天气（演示用静态数据）。
     */
    @Tool(name = "getWeather", description = "查询指定城市的当前天气。支持的城市：武汉、宁德、北京、上海")
    public String getWeather(@ToolParam(description = "城市名，如：武汉") String city) {
        System.out.println("[Tool调用] getWeather(city=" + city + ")");
        String result = WEATHER.getOrDefault(city, "暂无 " + city + " 的天气数据，支持的城市：武汉、宁德、北京、上海");
        System.out.println("[Tool返回] getWeather -> " + result);
        return result;
    }

    /**
     * 添加一条待办事项。
     */
    @Tool(name = "addTodo", description = "向待办清单中添加一条待办事项")
    public String addTodo(@ToolParam(description = "待办事项内容") String item) {
        System.out.println("[Tool调用] addTodo(item=" + item + ")");
        if (item == null || item.isBlank()) {
            String err = "待办内容为空，请告诉我要添加的具体事项";
            System.out.println("[Tool返回] addTodo -> " + err);
            return err;
        }
        todos.add(item);
        String result = "已添加待办：" + item + "（当前共 " + todos.size() + " 条）";
        System.out.println("[Tool返回] addTodo -> " + result);
        return result;
    }

    /**
     * 查看全部待办事项。
     */
    @Tool(name = "listTodos", description = "查看当前待办清单中的所有事项")
    public String listTodos() {
        System.out.println("[Tool调用] listTodos()");
        String result;
        if (todos.isEmpty()) {
            result = "待办清单为空";
        } else {
            StringBuilder sb = new StringBuilder("待办清单：\n");
            for (int i = 0; i < todos.size(); i++) {
                sb.append(i + 1).append(". ").append(todos.get(i)).append("\n");
            }
            result = sb.toString();
        }
        System.out.println("[Tool返回] listTodos -> \n" + result);
        return result;
    }
}
