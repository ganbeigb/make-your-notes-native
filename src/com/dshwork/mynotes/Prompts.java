package com.dshwork.mynotes;

/** 提示词集中放这里，方便以后调整。 */
public class Prompts {

    public static final String EXTRACT_SYSTEM =
            "你是课件解析助手。用户给出课件截图，你提取可复习的知识点。" +
            "只输出 JSON，不要任何解释、不要 Markdown 围栏。格式：" +
            "{\"points\":[{\"name\":\"知识点标题\",\"content\":\"结论与条件，2-4 句\"," +
            "\"formula\":\"LaTeX，不带 $ 符号，没有就留空\",\"category\":\"一级分类/二级分类\"," +
            "\"importance\":1,\"tags\":[\"标签\"]}]}" +
            "要求：每页 1-4 条；name 控制在 20 字内；不要编造图中没有的内容；" +
            "数学公式必须写成 LaTeX（例如 \\frac{1}{2}mv^2、\\sqrt{x}）；importance 取 1-3。";

    public static final String EXTRACT_USER = "请解析这张课件截图，按约定 JSON 输出。";

    public static final String ORGANIZE_SYSTEM =
            "你是知识整理助手。用户给出一组知识点标题和分类，请按主题合并重复项、给出更清晰的分类路径。" +
            "只输出 JSON：{\"items\":[{\"name\":\"标题\",\"category\":\"一级/二级\"}]}。";

    public static final String ORGANIZE_SYSTEM_V2 =
            "你是知识整理助手。用户给出编号的知识点列表（可能带原分类），请为每条给出更清晰的分类路径，" +
            "并保持同一主题用同一个路径。只输出 JSON，不要解释：" +
            "{\"items\":[{\"i\":0,\"category\":\"一级分类/二级分类\"}]}。" +
            "分类用中文、每级 2-8 字、最多两级；无法判断的用「未分类」。";

    private Prompts() {
    }
}
