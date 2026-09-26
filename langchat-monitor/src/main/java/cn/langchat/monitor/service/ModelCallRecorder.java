package cn.langchat.monitor.service;

import cn.langchat.monitor.config.ModelCallLogConfiguration;
import cn.langchat.monitor.entity.AigcModelCallLog;
import cn.langchat.monitor.model.ModelCallMetadata;
import cn.langchat.monitor.model.ModelCallRecord;
import cn.langchat.monitor.model.ModelCallType;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 模型调用记录器。
 *
 * <p>监控埋点的统一入口：所有模型调用的埋点组件（ChatModelListener、EmbeddingModelListener、
 * 图像/OCR 调用点）都汇入这里落库。</p>
 *
 * <p>写库在独立线程池异步执行且吞掉异常——监控是旁路能力，任何记录失败都不能
 * 影响主业务流程，也不能因监控拖慢模型响应。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Component
@Slf4j
public class ModelCallRecorder {

    private static final int MAX_ERROR_LENGTH = 500;

    private final AigcModelCallLogService aigcModelCallLogService;
    private final Executor executor;

    public ModelCallRecorder(
            AigcModelCallLogService aigcModelCallLogService,
            @Qualifier(ModelCallLogConfiguration.EXECUTOR_NAME) Executor executor
    ) {
        this.aigcModelCallLogService = aigcModelCallLogService;
        this.executor = executor;
    }

    /**
     * 异步记录一次模型调用。
     */
    public void record(ModelCallRecord record) {
        if (record == null || record.getMetadata() == null) {
            return;
        }
        try {
            executor.execute(() -> persist(record));
        } catch (Exception ex) {
            // 线程池已满或已关闭时退化为同步写入，保证调用不丢
            log.warn("提交模型调用日志任务失败，改为同步写入，modelId={}", record.getMetadata().modelId());
            persist(record);
        }
    }

    private void persist(ModelCallRecord record) {
        try {
            aigcModelCallLogService.save(toEntity(record));
        } catch (Exception ex) {
            log.warn("模型调用日志写入失败，modelId={}, callType={}",
                    record.getMetadata().modelId(), record.getCallType(), ex);
        }
    }

    private AigcModelCallLog toEntity(ModelCallRecord record) {
        ModelCallMetadata metadata = record.getMetadata();
        ModelCallType callType = record.getCallType() == null ? ModelCallType.CHAT : record.getCallType();
        AigcModelCallLog entity = new AigcModelCallLog();
        entity.setModelId(metadata.modelId());
        entity.setModelName(metadata.modelName());
        entity.setProvider(metadata.provider());
        entity.setScene(metadata.scene());
        entity.setCallType(callType.name());
        entity.setStatus(record.isSuccess() ? "SUCCESS" : "ERROR");
        entity.setInputToken(record.getInputToken());
        entity.setOutputToken(record.getOutputToken());
        entity.setTotalToken(resolveTotalToken(record));
        entity.setDuration(record.getDuration());
        entity.setItemCount(record.getItemCount());
        entity.setErrorMessage(truncate(record.getErrorMessage()));
        return entity;
    }

    /**
     * 总 Token：优先取模型返回的总量，缺省时用输入 + 输出补齐。
     */
    private Integer resolveTotalToken(ModelCallRecord record) {
        if (record.getTotalToken() != null) {
            return record.getTotalToken();
        }
        Integer input = record.getInputToken();
        Integer output = record.getOutputToken();
        if (input == null && output == null) {
            return null;
        }
        return (input == null ? 0 : input) + (output == null ? 0 : output);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
