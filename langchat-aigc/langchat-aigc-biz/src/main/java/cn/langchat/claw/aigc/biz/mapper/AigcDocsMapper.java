package cn.langchat.claw.aigc.biz.mapper;

import cn.langchat.claw.aigc.biz.entity.AigcDocs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcDocsMapper extends BaseMapper<AigcDocs> {
}
