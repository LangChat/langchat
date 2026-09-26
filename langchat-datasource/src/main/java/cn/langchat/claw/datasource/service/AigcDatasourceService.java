package cn.langchat.claw.datasource.service;

import cn.langchat.claw.datasource.entity.AigcDatasource;
import cn.langchat.claw.datasource.model.DatabaseStructure;
import cn.langchat.claw.datasource.model.DataQueryResult;
import cn.langchat.claw.datasource.model.TestConnectionRequest;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.Set;

/**
 * 数据源 Service 接口。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
public interface AigcDatasourceService extends IService<AigcDatasource> {

    /**
     * 测试数据库连接。
     */
    boolean testConnection(TestConnectionRequest request);

    /**
     * 内省数据库表结构。
     */
    DatabaseStructure introspect(String id) throws Exception;

    /**
     * 获取自定义表结构 JSON。
     */
    String getStructure(String id);

    /**
     * 保存自定义表结构 JSON。
     */
    boolean saveStructure(String id, String structureJson);

    /**
     * 在指定表白名单内执行只读查询。
     */
    DataQueryResult executeReadOnlyQuery(String id, String sql, Set<String> allowedTables);
}
