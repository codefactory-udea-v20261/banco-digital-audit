package com.udea.bancodigital.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuditApplicationTest {

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        // Test that Spring context loads correctly
    }

}
