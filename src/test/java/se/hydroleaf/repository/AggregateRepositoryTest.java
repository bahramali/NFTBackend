package se.hydroleaf.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregateRepositoryTest {

    @Mock
    NamedParameterJdbcTemplate jdbcTemplate;

    AggregateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AggregateRepository(jdbcTemplate);
    }

    @Test
    void handlesLargeCounts() throws SQLException {
        long largeCount = (long) Integer.MAX_VALUE + 5L;

        when(jdbcTemplate.queryForObject(anyString(), anyMap(), isA(RowMapper.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<AverageCount> mapper = (RowMapper<AverageCount>) invocation.getArgument(2);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getDouble("average")).thenReturn(0.0);
                    when(rs.getLong("count")).thenReturn(largeCount);
                    return mapper.mapRow(rs, 1);
                });

        AverageCount result = repository.getLatestAverage("sys", "layer", "type", "actuator_status");

        assertEquals(largeCount, result.getCount());
    }
}

