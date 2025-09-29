package com.timetable;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * 数据库迁移工具
 */
public class MigrateDatabase {
    
    public static void main(String[] args) {
        try {
            // 数据库连接配置
            String url = "jdbc:mysql://121.36.91.199:3306/timetable_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true";
            String username = "timetable";
            String password = "Leilei*0217";
            
            // 配置Flyway
            FluentConfiguration config = Flyway.configure()
                    .dataSource(url, username, password)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .validateOnMigrate(true);
            
            // 创建Flyway实例
            Flyway flyway = config.load();
            
            // 执行迁移
            System.out.println("开始执行数据库迁移...");
            flyway.migrate();
            System.out.println("数据库迁移完成！");
            
        } catch (Exception e) {
            System.err.println("数据库迁移失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
