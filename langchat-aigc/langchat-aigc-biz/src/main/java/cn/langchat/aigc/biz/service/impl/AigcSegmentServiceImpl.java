package cn.langchat.aigc.biz.service.impl;

import cn.langchat.aigc.biz.entity.AigcSegment;
import cn.langchat.aigc.biz.mapper.AigcSegmentMapper;
import cn.langchat.aigc.biz.service.AigcSegmentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 文档切片 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
public class AigcSegmentServiceImpl extends ServiceImpl<AigcSegmentMapper, AigcSegment> implements AigcSegmentService {

    @Override
    public List<Map<String, Object>> summarizeByDocsIds(Collection<String> docsIds) {
        if (docsIds == null || docsIds.isEmpty()) {
            return List.of();
        }
        return getBaseMapper().selectMaps(new QueryWrapper<AigcSegment>()
                .select("docs_id AS docsId", "COUNT(1) AS segmentCount", "SUM(CHAR_LENGTH(content)) AS charCount")
                .in("docs_id", docsIds)
                .groupBy("docs_id"));
    }
}
