/*
 * Copyright (c) 2024 LangChat. TyCoding All Rights Reserved.
 *
 * Licensed under the GNU Affero General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/agpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.tycoding.langchat.ai.core.provider;

import cn.hutool.core.util.ObjectUtil;
import cn.tycoding.langchat.ai.biz.component.ModelTypeEnum;
import cn.tycoding.langchat.ai.biz.entity.AigcModel;
import cn.tycoding.langchat.ai.biz.service.AigcModelService;
import cn.tycoding.langchat.ai.core.consts.ModelConst;
import cn.tycoding.langchat.ai.core.provider.build.ModelBuildHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author tycoding
 * @since 2024/6/16
 */
@Configuration
@Slf4j
public class ModelStoreFactory {

    @Autowired
    private AigcModelService aigcModelService;
    @Autowired
    private List<ModelBuildHandler> modelBuildHandlers;

    private final List<AigcModel> modelStore = new CopyOnWriteArrayList<>();
    private final Map<String, StreamingChatLanguageModel> streamingChatMap = new ConcurrentHashMap<>();
    private final Map<String, ChatLanguageModel> chatLanguageMap = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModelMap = new ConcurrentHashMap<>();
    private final Map<String, ImageModel> imageModelMap = new ConcurrentHashMap<>();

    @Async
    @PostConstruct
    public void init() {
        modelStore.clear();
        streamingChatMap.clear();
        chatLanguageMap.clear();
        embeddingModelMap.clear();
        imageModelMap.clear();

        List<AigcModel> list = aigcModelService.list();
        list.forEach(model -> {
            if (Objects.equals(model.getBaseUrl(), "")) {
                model.setBaseUrl(null);
            }

            chatHandler(model);
            embeddingHandler(model);
            imageHandler(model);
        });

        modelStore.forEach(i -> log.info("已成功注册模型：{} -- {}， 模型配置：{}", i.getProvider(), i.getType(), i));
    }

    private void chatHandler(AigcModel model) {
        if (!ModelTypeEnum.CHAT.name().equals(model.getType())) {
            return;
        }
        modelBuildHandlers.forEach(x -> {
            try {
                StreamingChatLanguageModel streamingModel = x.buildStreamingChat(model);
                if (ObjectUtil.isNotEmpty(streamingModel)) {
                    streamingChatMap.put(model.getId(), streamingModel);
                    modelStore.add(model);
                }

                ChatLanguageModel languageModel = x.buildChatLanguageModel(model);
                if (ObjectUtil.isNotEmpty(languageModel)) {
                    chatLanguageMap.put(model.getId() + ModelConst.TEXT_SUFFIX, languageModel);
                }
            } catch (Exception e) {
                log.error("model [id: {} name: {}] chat 配置报错", model.getId(), model.getName(), e);
            }
        });
    }

    private void embeddingHandler(AigcModel model) {
        if (!ModelTypeEnum.EMBEDDING.name().equals(model.getType())) {
            return;
        }
        modelBuildHandlers.forEach(x -> {
            try {
                EmbeddingModel embeddingModel = x.buildEmbedding(model);
                if (ObjectUtil.isNotEmpty(embeddingModel)) {
                    embeddingModelMap.put(model.getId(), embeddingModel);
                    modelStore.add(model);
                }
            } catch (Exception e) {
                log.error("model [id: {} name: {}] embedding 配置报错", model.getId(), model.getName(), e);
            }
        });
    }

    private void imageHandler(AigcModel model) {
        if (!ModelTypeEnum.TEXT_IMAGE.name().equals(model.getType())) {
            return;
        }
        modelBuildHandlers.forEach(x -> {
            try {
                ImageModel imageModel = x.buildImage(model);
                if (ObjectUtil.isNotEmpty(imageModel)) {
                    imageModelMap.put(model.getId(), imageModel);
                    modelStore.add(model);
                }
            } catch (Exception e) {
                log.error("model [id: {} name: {}] image 配置报错", model.getId(), model.getName(), e);
            }
        });
    }

    public StreamingChatLanguageModel getStreamingChatModel(String modelId) {
        return streamingChatMap.get(modelId);
    }

    public boolean containsStreamingChatModel(String modelId) {
        return streamingChatMap.containsKey(modelId);
    }

    public ChatLanguageModel getChatLanguageModel(String modelId) {
        return chatLanguageMap.get(modelId + ModelConst.TEXT_SUFFIX);
    }

    public boolean containsChatLanguageModel(String modelId) {
        return chatLanguageMap.containsKey(modelId + ModelConst.TEXT_SUFFIX);
    }

    public EmbeddingModel getEmbeddingModel(String modelId) {
        return embeddingModelMap.get(modelId);
    }

    public boolean containsEmbeddingModel(String modelId) {
        return embeddingModelMap.containsKey(modelId);
    }

    public ImageModel getImageModel(String modelId) {
        return imageModelMap.get(modelId);
    }

    public boolean containsImageModel(String modelId) {
        return imageModelMap.containsKey(modelId);
    }
}
