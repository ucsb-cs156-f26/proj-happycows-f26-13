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

public class MigrateFarmerCowsToHerdsMigrationTests {

    private static final String CHANGE_SET_RESOURCE =
            "db/migration/changes/019_migrate_farmer_cows_to_herds.json";

    private String extractSql() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try (InputStream input =
                getClass().getClassLoader().getResourceAsStream(CHANGE_SET_RESOURCE)) {
            root = mapper.readTree(input);
        }
        return root.get("databaseChangeLog").get(0).get("changeSet")
                .get("changes").get(0).get("sql").get("sql").asText();
    }

    @Test
    void migration_creates_one_herd_per_farmer_with_cows_preserving_count_and_health() throws Exception {
        String sql = extractSql();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:migration019test_" + System.identityHashCode(this))) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE farmer (user_id BIGINT, game_id BIGINT, "
                        + "num_of_cows INT, cow_health DOUBLE PRECISION, PRIMARY KEY (game_id, user_id))");
                stmt.execute("CREATE TABLE herds (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "user_id BIGINT NOT NULL, game_id BIGINT NOT NULL, "
                        + "num_cows INT NOT NULL, health DOUBLE PRECISION NOT NULL)");

                // (user_id, game_id, num_of_cows, cow_health)
                stmt.execute("INSERT INTO farmer VALUES (1, 1, 10, 100.0)");
                stmt.execute("INSERT INTO farmer VALUES (2, 1, 3, 42.5)");
                stmt.execute("INSERT INTO farmer VALUES (1, 2, 25, 0.0)");   // zero health, still has cows
                stmt.execute("INSERT INTO farmer VALUES (3, 1, 0, 100.0)");  // no cows -> no herd
                stmt.execute("INSERT INTO farmer VALUES (4, 2, 0, 12.0)");   // no cows -> no herd

                stmt.execute(sql);

                Map<String, String> expected = new LinkedHashMap<>();
                expected.put("1/1", "10@100.0");
                expected.put("1/2", "25@0.0");
                expected.put("2/1", "3@42.5");

                assertEquals(expected, herdsByFarmer(stmt));
                assertEquals(totalCows(stmt, "SELECT SUM(num_of_cows) FROM farmer"),
                        totalCows(stmt, "SELECT SUM(num_cows) FROM herds"));

                // Re-running must not duplicate cows.
                stmt.execute(sql);

                assertEquals(expected, herdsByFarmer(stmt));
                assertEquals(3, totalCows(stmt, "SELECT COUNT(*) FROM herds"));
                assertEquals(38, totalCows(stmt, "SELECT SUM(num_cows) FROM herds"));
            }
        }
    }

    private Map<String, String> herdsByFarmer(Statement stmt) throws Exception {
        Map<String, String> actual = new LinkedHashMap<>();
        try (ResultSet rs = stmt.executeQuery(
                "SELECT user_id, game_id, num_cows, health FROM herds ORDER BY user_id, game_id")) {
            while (rs.next()) {
                actual.put(rs.getLong("user_id") + "/" + rs.getLong("game_id"),
                        rs.getInt("num_cows") + "@" + rs.getDouble("health"));
            }
        }
        return actual;
    }

    private long totalCows(Statement stmt, String query) throws Exception {
        try (ResultSet rs = stmt.executeQuery(query)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
