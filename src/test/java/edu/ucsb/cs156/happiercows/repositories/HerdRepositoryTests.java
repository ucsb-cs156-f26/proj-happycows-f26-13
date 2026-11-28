package edu.ucsb.cs156.happiercows.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.entities.Herd;
import edu.ucsb.cs156.happiercows.entities.User;

@DataJpaTest
public class HerdRepositoryTests {

    @Autowired
    private HerdRepository herdRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private Game game1;
    private Game game2;
    private Farmer user1Game1;
    private Farmer user2Game1;
    private Farmer user1Game2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder().email("one@ucsb.edu").build());
        user2 = userRepository.save(User.builder().email("two@ucsb.edu").build());
        game1 = gameRepository.save(Game.builder().name("game 1").lastDate(LocalDateTime.now()).build());
        game2 = gameRepository.save(Game.builder().name("game 2").lastDate(LocalDateTime.now()).build());
        user1Game1 = farmerRepository.save(Farmer.builder().user(user1).game(game1).username("one").build());
        user2Game1 = farmerRepository.save(Farmer.builder().user(user2).game(game1).username("two").build());
        user1Game2 = farmerRepository.save(Farmer.builder().user(user1).game(game2).username("one").build());
    }

    private Herd saveHerd(Farmer farmer, int numCows, double health) {
        return herdRepository.save(Herd.builder().farmer(farmer).numCows(numCows).health(health).build());
    }

    private List<Herd> find(Game game, User user) {
        List<Herd> result = new ArrayList<>();
        herdRepository.findByGameIdAndUserId(game.getId(), user.getId()).forEach(result::add);
        return result;
    }

    @Test
    void finds_only_the_given_farmers_herds_in_the_given_game_ordered_by_id() {
        Herd a = saveHerd(user1Game1, 5, 100.0);
        Herd b = saveHerd(user1Game1, 3, 42.5);
        saveHerd(user2Game1, 7, 90.0);  // same game, other farmer
        saveHerd(user1Game2, 9, 80.0);  // same user, other game

        List<Herd> results = find(game1, user1);

        assertEquals(2, results.size());
        assertEquals(a.getId(), results.get(0).getId());
        assertEquals(5, results.get(0).getNumCows());
        assertEquals(100.0, results.get(0).getHealth());
        assertEquals(b.getId(), results.get(1).getId());
        assertEquals(3, results.get(1).getNumCows());
        assertEquals(42.5, results.get(1).getHealth());
    }

    @Test
    void returns_empty_when_farmer_has_no_herds() {
        saveHerd(user2Game1, 7, 90.0);

        assertEquals(0, find(game1, user1).size());
    }

    @Test
    void deleting_a_farmer_cascades_to_their_herds() {
        saveHerd(user1Game1, 5, 100.0);
        saveHerd(user2Game1, 7, 90.0);
        // Clear the persistence context so the delete hits the database the
        // same way GameController's delete endpoints do (no herds loaded), and
        // the removal of the herd rows is done by the FK's ON DELETE CASCADE.
        entityManager.flush();
        entityManager.clear();

        farmerRepository.delete(farmerRepository.findByGameIdAndUserId(game1.getId(), user1.getId()).get());
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, find(game1, user1).size());
        assertEquals(1, find(game1, user2).size());
    }
}
