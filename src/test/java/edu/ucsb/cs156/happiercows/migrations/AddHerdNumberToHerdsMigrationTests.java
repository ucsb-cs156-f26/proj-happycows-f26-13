package edu.ucsb.cs156.happiercows.migrations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AddHerdNumberToHerdsMigrationTests {

    private static final String CHANGE_SET_RESOURCE =
            "db/migration/changes/020_add_herd_number_to_herds.json";

    private JsonNode changes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input =
                getClass().getClassLoader().getResourceAsStream(CHANGE_SET_RESOURCE)) {
            return mapper.readTree(input).get("databaseChangeLog").get(0)
                    .get("changeSet").get("changes");
        }
    }

    @Test
    void existing_herds_are_numbered_from_1_for_each_farmer_in_each_game() throws Exception {
        JsonNode changes = changes();
        JsonNode column = changes.get(0).get("addColumn").get("columns").get(0).get("column");
        assertEquals("herd_number", column.get("name").asText());
        assertEquals("INT", column.get("type").asText());
        String numberingSql = changes.get(1).get("sql").get("sql").asText();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:migration020test_" + System.identityHashCode(this))) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE herds (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "user_id BIGINT NOT NULL, game_id BIGINT NOT NULL, "
                        + "num_cows INT NOT NULL, health DOUBLE PRECISION NOT NULL)");

                // (user_id, game_id) in id order, interleaved across farmers and games
                stmt.execute("INSERT INTO herds (user_id, game_id, num_cows, health) VALUES "
                        + "(1, 1, 5, 100.0), (2, 1, 3, 90.0), (1, 2, 4, 80.0), "
                        + "(1, 1, 2, 70.0), (2, 1, 6, 60.0), (1, 1, 1, 50.0)");

                stmt.execute("ALTER TABLE herds ADD herd_number INT DEFAULT 0 NOT NULL");
                stmt.execute(numberingSql);

                Map<Long, Integer> expected = new LinkedHashMap<>();
                expected.put(1L, 1);  // user 1, game 1
                expected.put(2L, 1);  // user 2, game 1
                expected.put(3L, 1);  // user 1, game 2
                expected.put(4L, 2);  // user 1, game 1
                expected.put(5L, 2);  // user 2, game 1
                expected.put(6L, 3);  // user 1, game 1

                assertEquals(expected, herdNumbersById(stmt));
            }
        }
    }

    private Map<Long, Integer> herdNumbersById(Statement stmt) throws Exception {
        Map<Long, Integer> actual = new LinkedHashMap<>();
        try (ResultSet rs = stmt.executeQuery("SELECT id, herd_number FROM herds ORDER BY id")) {
            while (rs.next()) {
                actual.put(rs.getLong("id"), rs.getInt("herd_number"));
            }
        }
        return actual;
    }
}
