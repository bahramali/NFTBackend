package se.hydroleaf.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ConnectionPoolMetricsTest {

    @Test
    void whenDisabledNoLogIsEmitted(CapturedOutput output) {
        HikariDataSource dataSource = Mockito.mock(HikariDataSource.class);
        ConnectionPoolMetrics metrics = new ConnectionPoolMetrics(dataSource, false);
        metrics.logMetrics();
        assertThat(output.getOut()).doesNotContain("HikariCP -");
    }
}
