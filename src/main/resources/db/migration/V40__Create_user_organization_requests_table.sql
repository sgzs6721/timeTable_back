-- V40__Create_user_organization_requests_table.sql
-- 创建用户机构申请表

CREATE TABLE IF NOT EXISTS user_organization_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL COMMENT '用户ID（审批通过后才创建）',
    organization_id BIGINT NOT NULL COMMENT '申请的机构ID',
    wechat_openid VARCHAR(100) NOT NULL COMMENT '微信OpenID',
    wechat_unionid VARCHAR(100) NULL COMMENT '微信UnionID',
    wechat_nickname VARCHAR(100) NULL COMMENT '微信昵称',
    wechat_avatar VARCHAR(500) NULL COMMENT '微信头像',
    wechat_sex TINYINT NULL COMMENT '微信性别：0未知，1男，2女',
    apply_reason TEXT NULL COMMENT '申请理由',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    approved_by BIGINT NULL COMMENT '审批人ID',
    approved_at TIMESTAMP NULL COMMENT '审批时间',
    reject_reason TEXT NULL COMMENT '拒绝理由',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_wechat_openid (wechat_openid),
    INDEX idx_status (status),
    INDEX idx_organization_id (organization_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户机构申请表';

