SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `aigc_conversation`;
CREATE TABLE `aigc_conversation` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `agent_id` varchar(50) DEFAULT NULL COMMENT 'AgentID',
  `share_id` varchar(50) DEFAULT NULL COMMENT '分享窗口ID',
  `title` varchar(100) DEFAULT NULL COMMENT '标题',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话窗口表';

DROP TABLE IF EXISTS `aigc_docs`;
CREATE TABLE `aigc_docs` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `oss_id` varchar(50) DEFAULT NULL COMMENT '资源文件ID',
  `knowledge_id` varchar(50) NOT NULL COMMENT '知识库ID',
  `parent_id` varchar(50) DEFAULT NULL COMMENT '父节点ID',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `indexing_status` tinyint(1) DEFAULT NULL COMMENT '训练状态',
  `embed_status` varchar(20) DEFAULT NULL COMMENT '向量化状态',
  `embed_error` text COMMENT '向量化错误信息',
  `embed_start_time` bigint DEFAULT NULL COMMENT '向量化开始时间',
  `embed_end_time` bigint DEFAULT NULL COMMENT '向量化结束时间',
  `ingestion_config` text COMMENT '向量化配置信息',
  `type` varchar(50) DEFAULT NULL COMMENT '文档类型',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  `ext` varchar(50) DEFAULT NULL COMMENT '文件后缀名',
  `url` varchar(500) DEFAULT NULL COMMENT '文件URL',
  `pdf_url` varchar(500) DEFAULT NULL COMMENT 'PDF文件URL',
  `content` text COMMENT '内容或链接',
  `size` int DEFAULT NULL COMMENT '文件大小',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

DROP TABLE IF EXISTS `aigc_knowledge`;
CREATE TABLE `aigc_knowledge` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `name` varchar(50) DEFAULT NULL COMMENT '知识库名称',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面URL',
  `max_results` int DEFAULT NULL COMMENT '召回数量',
  `min_score` double DEFAULT NULL COMMENT '召回分数',
  `vector_store_id` varchar(50) DEFAULT NULL COMMENT '向量数据库ID',
  `vector_model_id` varchar(50) DEFAULT NULL COMMENT '向量模型ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

DROP TABLE IF EXISTS `aigc_log`;
CREATE TABLE `aigc_log` (
  `id` varchar(50) NOT NULL COMMENT '编号',
  `type` varchar(50) DEFAULT NULL COMMENT '日志类型',
  `username` varchar(20) DEFAULT NULL COMMENT '操作用户',
  `operation` varchar(20) DEFAULT NULL COMMENT '操作描述',
  `url` varchar(1000) DEFAULT NULL COMMENT '请求URL',
  `time` bigint DEFAULT NULL COMMENT '耗时(毫秒)',
  `method` varchar(100) DEFAULT NULL COMMENT '操作方法',
  `params` varchar(255) DEFAULT NULL COMMENT '操作参数',
  `ip` varchar(255) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志表';

DROP TABLE IF EXISTS `aigc_mcp`;
CREATE TABLE `aigc_mcp` (
  `id` varchar(50) NOT NULL COMMENT '主键ID',
  `uuid` varchar(50) DEFAULT NULL COMMENT 'UUID',
  `name` varchar(255) DEFAULT NULL COMMENT '服务名称',
  `mcp_json` text COMMENT 'MCP链接JSON',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '应用封面',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
  `authorized` tinyint(1) DEFAULT NULL COMMENT '是否授权',
  `transport` varchar(50) DEFAULT NULL COMMENT 'MCP协议类型',
  `sse_url` varchar(500) DEFAULT NULL COMMENT 'HTTP协议请求地址',
  `headers` text COMMENT '请求头',
  `docker_image` varchar(100) DEFAULT NULL COMMENT 'Docker镜像名称',
  `docker_host` varchar(100) DEFAULT NULL COMMENT 'Docker Host地址',
  `site_url` varchar(250) DEFAULT NULL COMMENT '网站地址',
  `timeout` int DEFAULT NULL COMMENT '超时时间',
  `description` varchar(500) DEFAULT NULL COMMENT '描述信息',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息渠道表';

DROP TABLE IF EXISTS `aigc_menu`;
CREATE TABLE `aigc_menu` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `name` varchar(100) DEFAULT NULL COMMENT '菜单名称',
  `parent_id` varchar(50) DEFAULT NULL COMMENT '父级ID',
  `path` varchar(255) DEFAULT NULL COMMENT '菜单路径',
  `perms` varchar(255) DEFAULT NULL COMMENT '权限标识',
  `type` varchar(20) DEFAULT NULL COMMENT '菜单类型',
  `order_no` int DEFAULT NULL COMMENT '排序',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `is_disabled` tinyint(1) DEFAULT NULL COMMENT '是否禁用',
  `is_ext` tinyint(1) DEFAULT NULL COMMENT '是否外链',
  `is_keepalive` tinyint(1) DEFAULT NULL COMMENT '是否缓存',
  `is_show` tinyint(1) DEFAULT NULL COMMENT '是否显示',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

DROP TABLE IF EXISTS `aigc_message`;
CREATE TABLE `aigc_message` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `conversation_id` varchar(50) DEFAULT NULL COMMENT '会话ID',
  `chat_id` varchar(50) DEFAULT NULL COMMENT '消息的ID',
  `agent_id` varchar(50) DEFAULT NULL COMMENT 'AgentID',
  `role` varchar(10) DEFAULT NULL COMMENT '角色',
  `model` varchar(50) DEFAULT NULL COMMENT '模型名称',
  `message` longtext COMMENT '消息内容(JSON格式)',
  `type` varchar(50) DEFAULT NULL COMMENT '消息类型',
  `finish_reason` varchar(50) DEFAULT NULL COMMENT '完成原因',
  `trace_info` text COMMENT '上下文历史',
  `attachments` text COMMENT '附件',
  `input_token` int DEFAULT NULL COMMENT '输入Token',
  `output_token` int DEFAULT NULL COMMENT '输出Token',
  `likes` tinyint(1) DEFAULT NULL COMMENT '是否喜欢',
  `duration` int DEFAULT NULL COMMENT '执行耗时',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_aigc_message_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

DROP TABLE IF EXISTS `aigc_message_event`;
CREATE TABLE `aigc_message_event` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `conversation_id` varchar(50) DEFAULT NULL COMMENT '会话ID',
  `message_id` varchar(50) DEFAULT NULL COMMENT '关联消息ID',
  `chat_id` varchar(50) DEFAULT NULL COMMENT '聊天链路ID',
  `agent_id` varchar(50) DEFAULT NULL COMMENT 'Agent ID',
  `event_index` int DEFAULT NULL COMMENT '事件顺序',
  `event_name` varchar(100) DEFAULT NULL COMMENT '事件名称',
  `event_type` varchar(50) DEFAULT NULL COMMENT '事件类型',
  `event_status` varchar(50) DEFAULT NULL COMMENT '事件状态',
  `payload_json` longtext COMMENT '原始事件JSON',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_aigc_message_event_conversation_id` (`conversation_id`),
  KEY `idx_aigc_message_event_message_id` (`message_id`),
  KEY `idx_aigc_message_event_chat_id` (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息事件表';

DROP TABLE IF EXISTS `aigc_model`;
CREATE TABLE `aigc_model` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `type` varchar(100) DEFAULT NULL COMMENT '类型',
  `model` varchar(100) DEFAULT NULL COMMENT '模型名称',
  `provider` varchar(100) DEFAULT NULL COMMENT '供应商',
  `name` varchar(100) DEFAULT NULL COMMENT '别名',
  `max_token` int DEFAULT NULL COMMENT '最大回复长度',
  `temperature` double DEFAULT NULL COMMENT '温度',
  `top_p` double DEFAULT NULL,
  `api_key` varchar(255) DEFAULT NULL,
  `timeout` int DEFAULT NULL COMMENT '超时时间（分钟）',
  `base_url` varchar(100) DEFAULT NULL,
  `endpoint` varchar(100) DEFAULT NULL,
  `dimension` int DEFAULT NULL COMMENT '向量维数',
  `secret_key` varchar(255) DEFAULT NULL,
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  `billing_type` varchar(20) DEFAULT 'TOKEN' COMMENT '计费类型',
  `input_price` decimal(10,6) DEFAULT '0.000000' COMMENT '输入价格',
  `output_price` decimal(10,6) DEFAULT '0.000000' COMMENT '输出价格',
  `times_price` decimal(10,6) DEFAULT '0.000000' COMMENT '按次计费价格',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM模型配置表';

DROP TABLE IF EXISTS `aigc_agent`;
CREATE TABLE `aigc_agent` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `agent_name` varchar(255) DEFAULT NULL COMMENT 'Agent名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `status` varchar(20) DEFAULT NULL COMMENT '状态',
  `system_prompt` longtext COMMENT '系统提示词',
  `reasoning_model_id` varchar(50) DEFAULT NULL COMMENT '推理模型ID',
  `knowledge_ids` text COMMENT '知识库ID列表(JSON格式)',
  `skill_ids` text COMMENT '技能ID列表(JSON格式)',
  `mcp_ids` text COMMENT 'MCP服务ID列表(JSON格式)',
  `model_config_json` text COMMENT '模型配置JSON',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像',
  `meta_json` text COMMENT '元数据JSON',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent主配置表';

DROP TABLE IF EXISTS `aigc_agent_api_key`;
CREATE TABLE `aigc_agent_api_key` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `agent_id` varchar(50) NOT NULL COMMENT 'AgentID',
  `name` varchar(100) DEFAULT NULL COMMENT '密钥名称',
  `api_key` varchar(100) NOT NULL COMMENT 'API Key',
  `status` varchar(20) DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `call_count` int DEFAULT 0 COMMENT '调用次数',
  `input_tokens` bigint DEFAULT 0 COMMENT '累计输入Token',
  `output_tokens` bigint DEFAULT 0 COMMENT '累计输出Token',
  `last_call_time` bigint DEFAULT NULL COMMENT '最后调用时间',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent API Key表';

DROP TABLE IF EXISTS `aigc_skill`;
CREATE TABLE `aigc_skill` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '技能标识(SKILL.md frontmatter name)，运行时作为工具名',
  `title` varchar(255) DEFAULT NULL COMMENT '技能标题',
  `description` varchar(1000) DEFAULT NULL COMMENT '技能描述',
  `version` varchar(50) DEFAULT NULL COMMENT '技能版本',
  `entry_file` varchar(255) DEFAULT 'SKILL.md' COMMENT '入口文档相对路径',
  `license` varchar(100) DEFAULT NULL COMMENT '开源协议',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签,逗号分隔',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `package_size` bigint DEFAULT NULL COMMENT '原始技能包大小(字节)',
  `file_count` int DEFAULT NULL COMMENT '包内文件数量',
  `oss_object_key` varchar(500) DEFAULT NULL COMMENT '原始技能包OSS objectKey',
  `oss_filename` varchar(255) DEFAULT NULL COMMENT '原始技能包文件名',
  `local_path` varchar(500) DEFAULT NULL COMMENT '本地工作区相对路径(相对技能工作区根目录)',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill主表';

DROP TABLE IF EXISTS `aigc_role`;
CREATE TABLE `aigc_role` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `code` varchar(50) DEFAULT NULL COMMENT '角色别名',
  `description` varchar(100) DEFAULT NULL COMMENT '描述',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

DROP TABLE IF EXISTS `aigc_oss`;
CREATE TABLE `aigc_oss` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `filename` varchar(500) DEFAULT NULL COMMENT '文件存储名称',
  `original_filename` varchar(500) DEFAULT NULL COMMENT '原始文件名',
  `url` text COMMENT '文件地址',
  `path` varchar(500) DEFAULT NULL COMMENT '文件的绝对路径',
  `size` int DEFAULT NULL COMMENT '文件大小',
  `ext` varchar(50) DEFAULT NULL COMMENT '文件后缀',
  `content_type` varchar(100) DEFAULT NULL COMMENT '文件头',
  `platform` varchar(50) DEFAULT NULL COMMENT '平台',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源文件表';

DROP TABLE IF EXISTS `aigc_role_menu`;
CREATE TABLE `aigc_role_menu` (
  `role_id` varchar(50) NOT NULL COMMENT '角色ID',
  `menu_id` varchar(50) NOT NULL COMMENT '菜单/按钮ID',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色资源关联表';

DROP TABLE IF EXISTS `aigc_segment`;
CREATE TABLE `aigc_segment` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `index_hash` varchar(50) DEFAULT NULL COMMENT '索引Hash',
  `docs_id` varchar(50) NOT NULL COMMENT '文档ID',
  `knowledge_id` varchar(50) NOT NULL COMMENT '知识库ID',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `name` varchar(255) DEFAULT NULL COMMENT '文档名称',
  `position` int DEFAULT '0' COMMENT '索引位置',
  `content` text COMMENT '切片内容',
  `status` tinyint(1) DEFAULT NULL COMMENT '训练状态',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';

DROP TABLE IF EXISTS `aigc_user`;
CREATE TABLE `aigc_user` (
  `id` varchar(50) NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `real_name` varchar(255) DEFAULT NULL COMMENT '真实姓名',
  `sex` varchar(10) DEFAULT NULL COMMENT '性别',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` mediumtext COMMENT '头像',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态 0锁定 1有效',
  `dept_id` varchar(50) DEFAULT NULL COMMENT '所属部门ID',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aigc_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

DROP TABLE IF EXISTS `aigc_user_role`;
CREATE TABLE `aigc_user_role` (
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `role_id` varchar(50) NOT NULL COMMENT '角色ID',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

DROP TABLE IF EXISTS `aigc_vector_store`;
CREATE TABLE `aigc_vector_store` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `name` varchar(100) DEFAULT NULL COMMENT '别名',
  `provider` varchar(100) DEFAULT NULL COMMENT '供应商',
  `host` varchar(100) DEFAULT NULL COMMENT '地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `database_name` varchar(100) DEFAULT NULL COMMENT '数据库名称',
  `dimension` int DEFAULT NULL COMMENT '向量维数',
  `table_name` varchar(255) DEFAULT NULL COMMENT '表名或集合名',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Embedding向量数据库配置表';

-- ----------------------------
-- Mock data for local development
-- 执行完成后可直接使用 admin / 123456 登录
-- ----------------------------

INSERT INTO `aigc_role` (`id`, `name`, `code`, `description`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('role_admin', '超级管理员', 'ADMIN', 'LangChat 系统内置管理员角色，拥有全部权限（内置角色，不可编辑/删除）', 'system', 'system', 1774310400000, 1774310400000),
('role_user', '普通用户', 'USER', '业务使用者：可使用智能应用、知识库与智能问数', 'system', 'system', 1774310400000, 1774310400000),
('role_ops', '运维管理员', 'OPS', '负责模型、向量存储、技能、MCP 与数据源等基础能力运维', 'system', 'system', 1774310400000, 1774310400000),
('role_analyst', '数据分析师', 'ANALYST', '面向智能问数与数据洞察场景的分析角色', 'system', 'system', 1774310400000, 1774310400000);

-- 角色菜单授权（超级管理员为内置角色，默认拥有全部菜单，无需显式授权）
INSERT INTO `aigc_role_menu` (`role_id`, `menu_id`, `creator`, `updater`, `create_time`, `update_time`) VALUES
-- 普通用户：智能应用分组（Agent 管理、知识库、智能问数）
('role_user', 'menu_ai', 'system', 'system', 1774310400000, 1774310400000),
('role_user', 'menu_agents', 'system', 'system', 1774310400000, 1774310400000),
('role_user', 'menu_knowledges', 'system', 'system', 1774310400000, 1774310400000),
('role_user', 'menu_data_analysis', 'system', 'system', 1774310400000, 1774310400000),
-- 运维管理员：模型与能力分组（模型、向量存储、技能、MCP、数据源）
('role_ops', 'menu_model', 'system', 'system', 1774310400000, 1774310400000),
('role_ops', 'menu_models', 'system', 'system', 1774310400000, 1774310400000),
('role_ops', 'menu_vector_stores', 'system', 'system', 1774310400000, 1774310400000),
('role_ops', 'menu_skills', 'system', 'system', 1774310400000, 1774310400000),
('role_ops', 'menu_mcp', 'system', 'system', 1774310400000, 1774310400000),
('role_ops', 'menu_datasources', 'system', 'system', 1774310400000, 1774310400000),
('role_ops', 'menu_datasource_detail', 'system', 'system', 1774310400000, 1774310400000),
-- 数据分析师：智能问数 + 数据源
('role_analyst', 'menu_ai', 'system', 'system', 1774310400000, 1774310400000),
('role_analyst', 'menu_data_analysis', 'system', 'system', 1774310400000, 1774310400000),
('role_analyst', 'menu_datasources', 'system', 'system', 1774310400000, 1774310400000),
('role_analyst', 'menu_datasource_detail', 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_user` (`id`, `username`, `password`, `real_name`, `sex`, `phone`, `email`, `avatar`, `status`, `dept_id`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('user_admin', 'admin', '123456', 'LangChat 管理员', 'UNKNOWN', '13800138000', 'admin@langchat.local', 'https://api.dicebear.com/9.x/glass/svg?seed=LangChat', 1, NULL, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_user_role` (`user_id`, `role_id`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('user_admin', 'role_admin', 'system', 'system', 1774310400000, 1774310400000);

-- ============================================
-- 菜单数据 - 4 个一级分组，统一 4 字命名
-- ============================================

-- 分组1: 智能应用 (Catalog) - 挂载 Agent 管理、知识库与智能问数
INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_ai', '智能应用', NULL, '/ai', 'langchat:ai:view', 'CATALOG', 10, 'lucide:layout-grid', 'BasicLayout', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_agents', 'Agent管理', 'menu_ai', '/agents', 'aigc:agent:list', 'MENU', 11, 'lucide:bot-message-square', '/views/agents/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_knowledges', '知识库', 'menu_ai', '/knowledges', 'aigc:knowledge:list', 'MENU', 12, 'lucide:library-big', '/views/knowledges/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_data_analysis', '智能问数', 'menu_ai', '/data-analysis', 'aigc:data-analysis:use', 'MENU', 13, 'lucide:chart-no-axes-combined', '/views/data-analysis/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

-- 分组2: 图片创作 (Catalog) - 独立一级菜单，挂载图片生成与图片识别
INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_studio', '图片创作', NULL, '/studio', 'aigc:studio:view', 'CATALOG', 15, 'lucide:sparkles', 'BasicLayout', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_studio_image', '图片生成', 'menu_studio', '/studio/image-generation', 'aigc:studio:image', 'MENU', 14, 'lucide:image-plus', '/views/studio/image-generation/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_studio_ocr', '图片识别', 'menu_studio', '/studio/ocr', 'aigc:studio:ocr', 'MENU', 15, 'lucide:scan-text', '/views/studio/ocr/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

-- 分组3: 模型与能力 (Catalog) - 合并原模型服务与知识中心，技能、MCP 归入此副菜单
INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_model', '模型与能力', NULL, '/model', 'langchat:model:view', 'CATALOG', 20, 'lucide:cpu', 'BasicLayout', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_models', '模型管理', 'menu_model', '/models', 'aigc:model:list', 'MENU', 21, 'lucide:brain-circuit', '/views/models/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_vector_stores', '向量存储', 'menu_model', '/vector-stores', 'aigc:vector-store:list', 'MENU', 22, 'lucide:database-zap', '/views/vector-stores/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_skills', '技能管理', 'menu_model', '/skills', 'aigc:skill:list', 'MENU', 23, 'lucide:wrench', '/views/skills/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_mcp', 'MCP管理', 'menu_model', '/mcp', 'aigc:mcp:list', 'MENU', 24, 'lucide:plug-zap', '/views/mcp/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_datasources', '数据源', 'menu_model', '/datasources', 'aigc:datasource:list', 'MENU', 25, 'lucide:database', '/views/datasource/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_datasource_detail', '数据源详情', 'menu_datasources', '/datasources/:id/detail', 'aigc:datasource:list', 'MENU', 27, 'lucide:database', '/views/datasource/detail.vue', 0, 0, 0, 0, 'system', 'system', 1774310400000, 1774310400000);

-- 分组4: 系统管理 (Catalog)
INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_permissions', '系统管理', NULL, '/permissions', 'langchat:permission:view', 'CATALOG', 30, 'lucide:settings', 'BasicLayout', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_users', '用户管理', 'menu_permissions', '/permissions/users', 'langchat:user:list', 'MENU', 31, 'lucide:users', '/views/users/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_roles', '角色管理', 'menu_permissions', '/permissions/roles', 'langchat:role:list', 'MENU', 32, 'lucide:shield', '/views/roles/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

INSERT INTO `aigc_menu` (`id`, `name`, `parent_id`, `path`, `perms`, `type`, `order_no`, `icon`, `component`, `is_disabled`, `is_ext`, `is_keepalive`, `is_show`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('menu_permission_menus', '菜单管理', 'menu_permissions', '/permissions/menus', 'langchat:menu:list', 'MENU', 33, 'lucide:menu-square', '/views/menus/index.vue', 0, 0, 1, 1, 'system', 'system', 1774310400000, 1774310400000);

-- ============================================
-- 初始化业务数据
-- 模型、向量存储、知识库、文档、Agent 等业务数据保持空表，
-- 由使用者在控制台自行配置；仅预置技能与 MCP 默认数据。
-- ============================================

-- 默认技能：技能包以 zip 上传后归档 OSS，并解压至本地工作区（langchat.skill.workspace-dir），
-- 以下为预置的默认技能记录，local_path 对应工作区内的解压目录。
INSERT INTO `aigc_skill` (`id`, `name`, `title`, `description`, `version`, `entry_file`, `license`, `tags`, `enabled`, `package_size`, `file_count`, `oss_object_key`, `oss_filename`, `local_path`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('skill_web_search', 'web-search', '联网搜索', '调用搜索引擎检索实时信息，汇总结果并标注来源链接。', '1.0.0', 'SKILL.md', 'MIT', '搜索,联网,信息检索', 1, 20480, 3, 'skills/web-search-1.0.0.zip', 'web-search-1.0.0.zip', 'web-search', 'system', 'system', 1774310400000, 1774310400000),
('skill_web_reader', 'web-reader', '网页阅读', '抓取指定网页内容，输出结构化正文与关键摘要。', '1.0.0', 'SKILL.md', 'MIT', '网页,抓取,摘要', 1, 15360, 2, 'skills/web-reader-1.0.0.zip', 'web-reader-1.0.0.zip', 'web-reader', 'system', 'system', 1774310400000, 1774310400000),
('skill_data_chart', 'data-chart', '图表生成', '根据数据集生成柱状图、折线图、饼图等可视化图表。', '1.0.0', 'SKILL.md', 'MIT', '图表,可视化,数据分析', 1, 18432, 2, 'skills/data-chart-1.0.0.zip', 'data-chart-1.0.0.zip', 'data-chart', 'system', 'system', 1774310400000, 1774310400000);

-- 默认 MCP 服务
INSERT INTO `aigc_mcp` (`id`, `uuid`, `name`, `mcp_json`, `cover_url`, `tags`, `authorized`, `transport`, `sse_url`, `headers`, `docker_image`, `docker_host`, `site_url`, `timeout`, `description`, `creator`, `updater`, `create_time`, `update_time`) VALUES
('mcp_browser', 'mcp-browser-001', 'Browser MCP', '{"server":"browser","entry":"sse"}', 'https://images.unsplash.com/photo-1527443154391-507e9dc6c5cc?auto=format&fit=crop&w=600&q=80', '浏览器,搜索,网页', 1, 'SSE', 'http://127.0.0.1:8931/sse', '{"Authorization":"Bearer demo-browser"}', 'langchat/browser-mcp:latest', 'unix:///var/run/docker.sock', 'https://browser.langchat.local', 30, '提供网页浏览、页面摘要与链接抓取能力。', 'system', 'system', 1774310400000, 1774310400000),
('mcp_devops', 'mcp-devops-001', 'DevOps MCP', '{"server":"devops","entry":"http"}', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=600&q=80', '运维,监控,发布', 0, 'HTTP', 'http://127.0.0.1:8940/api/mcp', '{"X-Env":"test"}', 'langchat/devops-mcp:latest', 'tcp://127.0.0.1:2375', 'https://ops.langchat.local', 20, '用于测试环境中的部署、监控与告警工具接入。', 'system', 'system', 1774310400000, 1774310400000);

DROP TABLE IF EXISTS `aigc_datasource`;
CREATE TABLE `aigc_datasource` (
  `id` varchar(50) NOT NULL COMMENT '主键',
  `name` varchar(100) DEFAULT NULL COMMENT '数据源名称',
  `db_type` varchar(50) DEFAULT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE/SQLSERVER',
  `host` varchar(100) DEFAULT NULL COMMENT '主机地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `database_name` varchar(100) DEFAULT NULL COMMENT '数据库名',
  `url` varchar(500) DEFAULT NULL COMMENT '系统根据连接信息自动生成的连接URL',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `schema_name` varchar(100) DEFAULT NULL COMMENT 'Schema名称',
  `structure_json` longtext COMMENT '自定义表结构JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(100) DEFAULT NULL COMMENT '更新人',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

SET FOREIGN_KEY_CHECKS = 1;
