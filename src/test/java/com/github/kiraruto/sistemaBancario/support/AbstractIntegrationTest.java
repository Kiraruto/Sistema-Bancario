package com.github.kiraruto.sistemaBancario.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * O container é iniciado e encerrado por classe. Como o contexto do Spring é
 * cacheado entre classes, ele precisa ser descartado ao fim de cada uma para
 * não reutilizar a conexão de um container já encerrado.
 */
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.3")
                    .withDatabaseName("banking_test")
                    .withUsername("test")
                    .withPassword("test");
}
