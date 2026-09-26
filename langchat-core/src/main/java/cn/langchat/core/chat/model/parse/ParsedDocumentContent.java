package cn.langchat.core.chat.model.parse;

import java.util.List;
import lombok.Data;

/**
 * 解析后的文档内容。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class ParsedDocumentContent {

    /** 文档 ID。 */
    private String docsId;

    /** 文档标题。 */
    private String title;

    /** 归一化全文内容。 */
    private String plainText;

    /** 结构化分段内容。 */
    private List<String> sections;

    /** 解析器名称。 */
    private String parserName;
}
