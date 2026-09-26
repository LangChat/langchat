package cn.langchat.claw.datasource.model;

/**
 * 测试连接请求。
 *
 * @param dbType 数据库类型
 * @param host 主机地址
 * @param port 端口
 * @param databaseName 数据库名
 * @param username 用户名
 * @param password 密码
 * @author LangChat Team
 * @since 2026/8/27
 */
public record TestConnectionRequest(
        String dbType,
        String host,
        Integer port,
        String databaseName,
        String username,
        String password
) {
}
