package com.yoedu.yoedurealestateapi.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * Temporarily configure Flyway to run repair() before migrate().
     * This fixes the checksum mismatch caused by adding the btree_gist extension to V1.
     */
    @Bean
    public FlywayMigrationStrategy repairStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
