package cn.langchat.common.persistence;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 公共字段自动填充处理器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Slf4j
@Component
public class LangChatMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        long now = System.currentTimeMillis();
        String userId = currentUserId();
        strictInsertFill(metaObject, "creator", String.class, userId);
        strictInsertFill(metaObject, "updater", String.class, userId);
        strictInsertFill(metaObject, "createTime", Long.class, now);
        strictInsertFill(metaObject, "updateTime", Long.class, now);
        log.debug("自动填充新增字段完成，creator={}", userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        long now = System.currentTimeMillis();
        String userId = currentUserId();
        strictUpdateFill(metaObject, "updater", String.class, userId);
        strictUpdateFill(metaObject, "updateTime", Long.class, now);
        log.debug("自动填充更新字段完成，updater={}", userId);
    }

    /**
     * 获取当前登录用户ID，未登录时回退为 system。
     *
     * <p>流式对话的持久化可能运行在非 Web 线程（如 SSE 回调线程），
     * sa-token 在非 Web 上下文会抛 {@code NotWebContextException}，此处兜底为 system。
     */
    private String currentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId == null ? "system" : String.valueOf(loginId);
        } catch (Exception ex) {
            return "system";
        }
    }
}
