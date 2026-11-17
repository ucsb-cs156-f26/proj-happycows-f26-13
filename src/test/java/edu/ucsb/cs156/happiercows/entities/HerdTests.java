package edu.ucsb.cs156.happiercows.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HerdTests {

    @Test
    void herd_serialized_to_json_includes_user_and_game_id_but_not_farmer() {
        var objectMapper = new ObjectMapper();
        var farmer = Farmer.builder()
                .game(Game.builder().id(5).build())
                .user(User.builder().id(10).build())
                .build();
        var herd = Herd.builder()
                .id(7)
                .farmer(farmer)
                .numCows(12)
                .health(87.5)
                .build();

        Map<String, Object> asMap = objectMapper.convertValue(herd, Map.class);

        assertEquals(7L, asMap.get("id"));
        assertEquals(5L, asMap.get("gameId"));
        assertEquals(10L, asMap.get("userId"));
        assertEquals(12, asMap.get("numCows"));
        assertEquals(87.5, asMap.get("health"));
        assertFalse(asMap.containsKey("farmer"));
    }
}
