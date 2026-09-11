package com.jhorgi.libraryapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LibraryappApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    /**
     * Guards the isolation instead of trusting it.
     *
     * <p>This test used to boot against the developer's real Postgres, and once
     * the bootstrap seeder existed it wrote a live SUPER_ADMIN row there on every
     * run. The fix is src/test/resources/application.properties; this asserts the
     * fix is still in place, so a config change that reconnects the suite to a
     * real database fails the build rather than quietly mutating data.
     */
    @Test
    void testsRunAgainstAnInMemoryDatabaseNeverTheRealOne() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();

            assertThat(url).startsWith("jdbc:h2:mem:");
            assertThat(url).doesNotContain("postgresql");
        }
    }

    @Test
    void theBootstrapSeederIsOffSoNoTestCanCreateARealAdmin() {
        assertThat(environment.getProperty("security.bootstrap.enabled")).isEqualTo("false");
    }
}
