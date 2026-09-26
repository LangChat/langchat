package cn.langchat.aigc.biz.mapper;

import cn.langchat.aigc.biz.entity.AigcVectorStore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 向量库 Mapper。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Mapper
public interface AigcVectorStoreMapper extends BaseMapper<AigcVectorStore> {
}
