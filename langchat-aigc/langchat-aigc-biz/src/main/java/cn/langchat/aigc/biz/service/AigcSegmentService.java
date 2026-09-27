package cn.langchat.aigc.biz.service;

import cn.langchat.aigc.biz.entity.AigcSegment;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文档切片 Service 接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface AigcSegmentService extends IService<AigcSegment> {

    /**
     * 按文档ID汇总分段数量与字符数。
     *
     * @param docsIds 文档ID集合
     * @return 每篇文档一行，包含 docsId、segmentCount、charCount
     */
    List<Map<String, Object>> summarizeByDocsIds(Collection<String> docsIds);
}
