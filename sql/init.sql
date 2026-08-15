-- ============================================================
-- AI Demo Test - 初始建表
-- 基于 6 个 Entity 生成 (User/Provider/Model/Assistant/Conversation/Messages)
-- 结构与本地库 aiclientdemo 对齐
-- 建表顺序: user -> provider -> model -> assistant -> conversation -> messages
-- 注意: 如本地已有同结构表, 需先手动 DROP 旧表再执行
-- ============================================================

CREATE TABLE user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    account     VARCHAR(32)  NOT NULL UNIQUE COMMENT '登录账号',
    password    VARCHAR(128) NOT NULL             COMMENT '密码, BCrypt',
    nickname    VARCHAR(64)                       COMMENT '显示昵称',
    status      INT          DEFAULT 1            COMMENT '1=正常 0=禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE provider (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)                       COMMENT '提供商显示名(如:DeepSeek)',
    protocol    VARCHAR(32)  NOT NULL             COMMENT '请求体样式(openai/anthropicai/other)',
    base_url    VARCHAR(255) NOT NULL             COMMENT 'API端点',
    api_key     VARCHAR(255)                      COMMENT 'API密钥(AES加密存储)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider (protocol, base_url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型提供商表';

CREATE TABLE model (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL             COMMENT '模型名称: DeepSeek-V4',
    provider_id BIGINT       NOT NULL             COMMENT '提供商ID',
    model_id    VARCHAR(64)  NOT NULL             COMMENT 'API用模型ID: deepseek-v4-flash',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX provider_id (provider_id),
    UNIQUE KEY uk_model_id (model_id),
    CONSTRAINT fk_model_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型表';

CREATE TABLE assistant (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL             COMMENT '创建者',
    model_id    BIGINT       NOT NULL             COMMENT '使用的模型',
    name        VARCHAR(64)  NOT NULL             COMMENT '助手名称: Java导师',
    prompt      TEXT                              COMMENT '系统提示词模板',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_asst_user  FOREIGN KEY (user_id)  REFERENCES user(id),
    CONSTRAINT fk_asst_model FOREIGN KEY (model_id) REFERENCES model(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助手模板表';

CREATE TABLE conversation (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL           COMMENT '所属用户',
    assistant_id  BIGINT                           COMMENT '来源助手模板, 可为空',
    title         VARCHAR(255)                     COMMENT '会话标题',
    system_prompt TEXT                             COMMENT '系统提示词, 创建时固化',
    metadata      JSON                             COMMENT '扩展配置',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    CONSTRAINT fk_conv_user      FOREIGN KEY (user_id)      REFERENCES user(id),
    CONSTRAINT fk_conv_assistant FOREIGN KEY (assistant_id) REFERENCES assistant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT       NOT NULL          COMMENT '所属会话',
    role            VARCHAR(16)  NOT NULL          COMMENT 'user/assistant/system/tool',
    content         TEXT         NOT NULL          COMMENT '消息正文',
    seq             INT          DEFAULT 0         COMMENT '消息序号, 排序用',
    metadata        JSON                           COMMENT '扩展: tokens, tool_calls',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conv_id (conversation_id),
    INDEX idx_conv_seq (conversation_id, seq),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
