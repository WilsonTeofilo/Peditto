package com.peditto.common.tenant;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/** Sets the tenant GUC on the same connection and transaction used by JPA. */
@Configuration
public class TenantTransactionConfiguration {
    @Bean
    JpaTransactionManager transactionManager(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return new TenantJpaTransactionManager(dataSource, jdbcTemplate);
    }

    private static final class TenantJpaTransactionManager extends JpaTransactionManager {
        private final JdbcTemplate jdbcTemplate;

        private TenantJpaTransactionManager(DataSource dataSource, JdbcTemplate jdbcTemplate) {
            super();
            setDataSource(dataSource);
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            super.doBegin(transaction, definition);
            String organizationId = TenantContext.currentOrganizationId()
                    .map(Object::toString)
                    .orElse("");
            jdbcTemplate.queryForObject(
                    "SELECT set_config('peditto.organization_id', ?, true)",
                    String.class,
                    organizationId);
        }

    }
}
