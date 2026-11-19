package edu.ucsb.cs156.happiercows.jobs;

import edu.ucsb.cs156.happiercows.JobTestCase;
import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.entities.GamePlus;
import edu.ucsb.cs156.happiercows.entities.User;
import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.Herd;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.happiercows.repositories.GameRepository;
import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.repositories.HerdRepository;
import edu.ucsb.cs156.happiercows.repositories.UserRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.happiercows.services.GamePlusBuilderService;
import edu.ucsb.cs156.happiercows.strategies.CowHealthUpdateStrategies;
import edu.ucsb.cs156.happiercows.strategies.CowHealthUpdateStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class UpdateCowHealthJobTests extends JobTestCase {
        @Mock
        GameRepository gameRepository;

        @Mock
        FarmerRepository farmerRepository;

        @Mock
        UserRepository userRepository;

        @Mock
        GamePlusBuilderService gamePlusBuilderService;

        @Mock
        HerdRepository herdRepository;

        private final User user = User
                        .builder()
                        .id(1L)
                        .fullName("Chris Gaucho")
                        .email("cgaucho@example.org")
                        .build();

        private final User user2 = User
                        .builder()
                        .id(2L)
                        .fullName("Dana Gaucho")
                        .email("dgaucho@example.org")
                        .build();

        private final Game game = Game
                        .builder()
                        .name("test game")
                        .cowPrice(10)
                        .milkPrice(2)
                        .startingBalance(300)
                        .startingDate(LocalDateTime.now().minusDays(5))
                        .lastDate(LocalDateTime.now().plusDays(5))
                        .capacityPerUser(0)
                        .carryingCapacity(100)
                        .degradationRate(1)
                        .belowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Noop)
                        .aboveCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Noop)
                        .build();

        private final Farmer farmer = Farmer
                        .builder()
                        .user(user)
                        .game(game)
                        .totalWealth(300)
                        .numOfCows(1)
                        .cowHealth(10.0)
                        .build();

        private final Herd herd = herd(1, 1, 10.0);

        private final Job job = Job.builder().build();
        private final JobContext ctx = new JobContext(null, job);

        // Fails the test if a cow death roll happens when none should.
        private static final DoubleSupplier NO_RANDOM = () -> {
                throw new AssertionError("random should not be used");
        };

        private DoubleSupplier random = NO_RANDOM;

        private static Herd herd(int herdNumber, int numCows, double health) {
                return Herd.builder().id(herdNumber).herdNumber(herdNumber).numCows(numCows).health(health).build();
        }

        // Returns the given values in order, then fails if asked for more.
        private static DoubleSupplier sequence(double... values) {
                int[] next = { 0 };
                return () -> {
                        if (next[0] >= values.length) {
                                throw new AssertionError("random used more times than expected");
                        }
                        return values[next[0]++];
                };
        }

        private void runUpdateCowHealthJob() throws Exception {
                var updateCowHealthJob = new UpdateCowHealthJob(gameRepository, farmerRepository,
                                userRepository, gamePlusBuilderService, herdRepository, random);
                updateCowHealthJob.accept(ctx);
        }

        private void setupUpdateCowHealthTestOnGame(int totalCows, int numUsers) {
                // the 1-cow herd is below 30% health, so it rolls once; it survives
                random = sequence(0.5);
                setupGame(totalCows, numUsers, List.of(farmer));
                when(herdRepository.findByGameIdAndUserId(game.getId(), 1L)).thenReturn(List.of(herd));
        }

        private void setupGame(int totalCows, int numUsers, List<Farmer> farmers) {
                List<Game> listOfGame = List.of(game);
                GamePlus gamePlus = GamePlus.builder().game(game).totalCows(totalCows).totalUsers(numUsers).build();

                when(gameRepository.findAll()).thenReturn(listOfGame);
                when(farmerRepository.findByGameId(game.getId())).thenReturn(farmers);
                when(gameRepository.getNumCows(game.getId())).thenReturn(Optional.of(totalCows));
                when(gameRepository.getNumUsers(game.getId())).thenReturn(Optional.of(numUsers));
                when(gamePlusBuilderService.convertToGamePlus(eq(listOfGame))).thenReturn(List.of(gamePlus));
                when(gamePlusBuilderService.toGamePlus(eq(game))).thenReturn(gamePlus);
        }

        @Test
        void test_log_output_with_no_game() throws Exception {
                runUpdateCowHealthJob();

                String expected = """
                                Updating cow health...
                                Cow health has been updated!""";
                assertEquals(expected, job.getLog());
        }

        @Test
        void test_skips_game_when_game_not_in_progress() throws Exception {
                Game futureGame = Game
                                .builder()
                                .name("future game")
                                .startingDate(LocalDateTime.now().plusDays(5))
                                .lastDate(LocalDateTime.now().plusDays(10))
                                .build();

                List<Game> listOfGame = List.of(futureGame);
                GamePlus futureGamePlus = GamePlus.builder().game(futureGame).totalCows(1)
                                .totalUsers(1).build();

                when(gameRepository.findAll()).thenReturn(listOfGame);
                when(gamePlusBuilderService.convertToGamePlus(eq(listOfGame)))
                                .thenReturn(List.of(futureGamePlus));

                runUpdateCowHealthJob();

                String expected = """
                                Updating cow health...
                                Skipping Game id=0 (future game) because the game is not in progress
                                Cow health has been updated!""";
                assertEquals(expected, job.getLog());
        }

        @Test
        void test_uses_above_capacity_update_strategy() throws Exception {
                game.setAboveCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Constant);

                setupUpdateCowHealthTestOnGame(101, 1);
                runUpdateCowHealthJob();

                assertEquals(9.0, herd.getHealth());
                assertEquals(9.0, farmer.getCowHealth());
                verify(herdRepository).save(herd);
                verify(farmerRepository).save(farmer);

                String expected = """
                                Updating cow health...
                                Game test game, degradationRate: 1.0, effectiveCapacity: 100
                                User: Chris Gaucho, numCows: 1, cowHealth: 10.0
                                 Herd #1: old health: 10.0, new health: 9.0, cows died: 0, cows left: 1
                                 new numCows: 1, new cowHealth: 9.0
                                Cow health has been updated!""";
                assertEquals(expected, job.getLog());
        }

        @Test
        void test_uses_below_capacity_update_strategy() throws Exception {
                game.setBelowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Constant);
                setupUpdateCowHealthTestOnGame(99, 1);
                runUpdateCowHealthJob();

                assertEquals(11.0, herd.getHealth());
                assertEquals(11.0, farmer.getCowHealth());
        }

        @Test
        void test_uses_below_capacity_update_strategy_if_equal_to_carrying_capacity() throws Exception {
                game.setBelowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Constant);

                setupUpdateCowHealthTestOnGame(game.getCarryingCapacity(), 1);
                runUpdateCowHealthJob();

                assertEquals(11.0, herd.getHealth());
        }

        @Test
        void test_cow_health_minimum_is_0() throws Exception {
                var mockStrategy = mock(CowHealthUpdateStrategy.class);
                when(mockStrategy.calculateNewCowHealth(any(), anyDouble(), anyInt())).thenReturn(-1.0);
                var newHealth = UpdateCowHealthJob.calculateNewCowHealthUsingStrategy(
                                mockStrategy, gamePlusBuilderService.toGamePlus(game), 10.0, 1);
                assertEquals(0.0, newHealth);
        }

        @Test
        void test_cow_health_maximum_is_100() throws Exception {
                var mockStrategy = mock(CowHealthUpdateStrategy.class);
                when(mockStrategy.calculateNewCowHealth(any(), anyDouble(), anyInt())).thenReturn(101.0);
                var newHealth = UpdateCowHealthJob.calculateNewCowHealthUsingStrategy(
                                mockStrategy, gamePlusBuilderService.toGamePlus(game), 10.0, 1);
                assertEquals(100.0, newHealth);
        }

        @Test
        void test_strategy_is_given_the_herds_health() throws Exception {
                var mockStrategy = mock(CowHealthUpdateStrategy.class);
                when(mockStrategy.calculateNewCowHealth(any(), eq(42.0), eq(7))).thenReturn(43.0);
                var newHealth = UpdateCowHealthJob.calculateNewCowHealthUsingStrategy(
                                mockStrategy, gamePlusBuilderService.toGamePlus(game), 42.0, 7);
                assertEquals(43.0, newHealth);
        }

        @Test
        void two_herds_of_one_farmer_keep_different_health_and_farmer_gets_weighted_average() throws Exception {
                game.setBelowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Linear);
                Herd herd1 = herd(1, 1, 50.0);
                Herd herd2 = herd(2, 3, 80.0);
                farmer.setNumOfCows(4);

                setupGame(99, 1, List.of(farmer));
                when(herdRepository.findByGameIdAndUserId(game.getId(), 1L)).thenReturn(List.of(herd1, herd2));

                runUpdateCowHealthJob();

                // Linear, 1 cow under capacity: each herd's health goes up by 1
                assertEquals(51.0, herd1.getHealth());
                assertEquals(81.0, herd2.getHealth());
                assertEquals(4, farmer.getNumOfCows());
                // (1 * 51 + 3 * 81) / 4
                assertEquals(73.5, farmer.getCowHealth());

                String expected = """
                                Updating cow health...
                                Game test game, degradationRate: 1.0, effectiveCapacity: 100
                                User: Chris Gaucho, numCows: 4, cowHealth: 10.0
                                 Herd #1: old health: 50.0, new health: 51.0, cows died: 0, cows left: 1
                                 Herd #2: old health: 80.0, new health: 81.0, cows died: 0, cows left: 3
                                 new numCows: 4, new cowHealth: 73.5
                                Cow health has been updated!""";
                assertEquals(expected, job.getLog());
        }

        @Test
        void test_updating_values_for_multiple_users() throws Exception {
                game.setBelowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Linear);
                Farmer farmer2 = Farmer.builder().user(user2).game(game).totalWealth(300)
                                .numOfCows(6).cowHealth(20).build();
                Herd herd2 = herd(1, 6, 20.0);
                // both herds are below 30% health: 1 roll for the first farmer's
                // cow, then 6 for herd2's cows; none die
                random = sequence(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5);

                setupGame(99, 2, List.of(farmer, farmer2));
                when(herdRepository.findByGameIdAndUserId(game.getId(), 1L)).thenReturn(List.of(herd));
                when(herdRepository.findByGameIdAndUserId(game.getId(), 2L)).thenReturn(List.of(herd2));

                runUpdateCowHealthJob();

                assertEquals(11.0, farmer.getCowHealth());
                assertEquals(21.0, farmer2.getCowHealth());
                assertEquals(6, farmer2.getNumOfCows());
        }

        @Test
        void calculateCowDeaths_kills_every_cow_at_zero_health() {
                Herd dying = herd(1, 5, 0.0);

                assertEquals(5, UpdateCowHealthJob.calculateCowDeaths(dying, NO_RANDOM));
                assertEquals(0, dying.getNumCows());
        }

        @Test
        void calculateCowDeaths_gives_each_cow_a_one_percent_chance_below_30_health() {
                Herd sick = herd(1, 5, 29.9);

                // only rolls below 0.01 kill a cow
                int deaths = UpdateCowHealthJob.calculateCowDeaths(sick, sequence(0.0, 0.5, 0.0099, 0.01, 0.99));

                assertEquals(2, deaths);
                assertEquals(3, sick.getNumCows());
                assertEquals(29.9, sick.getHealth());
        }

        @Test
        void calculateCowDeaths_does_nothing_at_30_health_or_more() {
                Herd atThreshold = herd(1, 5, 30.0);
                Herd healthy = herd(2, 5, 90.0);

                assertEquals(0, UpdateCowHealthJob.calculateCowDeaths(atThreshold, NO_RANDOM));
                assertEquals(0, UpdateCowHealthJob.calculateCowDeaths(healthy, NO_RANDOM));
                assertEquals(5, atThreshold.getNumCows());
                assertEquals(5, healthy.getNumCows());
        }

        @Test
        void death_rule_matches_gamePlay_md() {
                assertEquals(30.0, UpdateCowHealthJob.DEATH_HEALTH_THRESHOLD);
                assertEquals(0.01, UpdateCowHealthJob.DEATH_CHANCE_PER_COW);
        }

        @Test
        void cows_die_one_herd_at_a_time_and_empty_herds_are_removed() throws Exception {
                game.setBelowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Noop);
                Herd dead = herd(1, 4, 0.0);
                Herd sick = herd(2, 3, 20.0);
                Herd healthy = herd(3, 2, 90.0);
                farmer.setNumOfCows(9);
                farmer.setCowDeaths(1);
                // sick herd's 3 cows: the second one dies
                random = sequence(0.5, 0.001, 0.5);

                setupGame(9, 1, List.of(farmer));
                when(herdRepository.findByGameIdAndUserId(game.getId(), 1L))
                                .thenReturn(List.of(dead, sick, healthy));

                runUpdateCowHealthJob();

                verify(herdRepository).delete(dead);
                verify(herdRepository, never()).save(dead);
                verify(herdRepository).save(sick);
                verify(herdRepository).save(healthy);
                assertEquals(2, sick.getNumCows());
                assertEquals(2, healthy.getNumCows());

                assertEquals(4, farmer.getNumOfCows());
                assertEquals(1 + 4 + 1, farmer.getCowDeaths());
                // (2 * 20 + 2 * 90) / 4; the dead herd no longer counts
                assertEquals(55.0, farmer.getCowHealth());

                String expected = """
                                Updating cow health...
                                Game test game, degradationRate: 1.0, effectiveCapacity: 100
                                User: Chris Gaucho, numCows: 9, cowHealth: 10.0
                                 Herd #1: old health: 0.0, new health: 0.0, cows died: 4, cows left: 0
                                 Herd #2: old health: 20.0, new health: 20.0, cows died: 1, cows left: 2
                                 Herd #3: old health: 90.0, new health: 90.0, cows died: 0, cows left: 2
                                 new numCows: 4, new cowHealth: 55.0
                                Cow health has been updated!""";
                assertEquals(expected, job.getLog());
        }

        @Test
        void farmer_whose_herds_all_die_has_no_cows_and_full_health() throws Exception {
                game.setBelowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Noop);
                Herd dying = herd(1, 5, 0.0);
                farmer.setNumOfCows(5);

                setupGame(5, 1, List.of(farmer));
                when(herdRepository.findByGameIdAndUserId(game.getId(), 1L)).thenReturn(List.of(dying));

                runUpdateCowHealthJob();

                verify(herdRepository).delete(dying);
                assertEquals(0, farmer.getNumOfCows());
                assertEquals(5, farmer.getCowDeaths());
                assertEquals(100.0, farmer.getCowHealth());
        }

        @Test
        void farmer_with_no_herds_has_no_cows_and_full_health() throws Exception {
                setupGame(0, 1, List.of(farmer));
                when(herdRepository.findByGameIdAndUserId(game.getId(), 1L)).thenReturn(List.of());

                runUpdateCowHealthJob();

                assertEquals(0, farmer.getNumOfCows());
                assertEquals(100.0, farmer.getCowHealth());
                verify(farmerRepository).save(farmer);
        }

        @Test
        void test_skipping_job_when_game_has_zero_users() throws Exception {
                GamePlus gamePlus = GamePlus.builder().game(game).totalCows(5).totalUsers(1).build();

                List<GamePlus> gamePlusList = List.of(gamePlus);
                List<Game> gameList = List.of(game);

                when(gamePlusBuilderService.convertToGamePlus(eq(gameList))).thenReturn(gamePlusList);
                when(gamePlusBuilderService.toGamePlus(eq(game))).thenReturn(gamePlus);
                game.setBelowCapacityHealthUpdateStrategy(CowHealthUpdateStrategies.Linear);

                when(gameRepository.findAll()).thenReturn(List.of(game));
                when(gameRepository.getNumUsers(game.getId())).thenReturn(Optional.of(0));

                runUpdateCowHealthJob();

                String expected = """
                                Updating cow health...
                                Game test game, degradationRate: 1.0, effectiveCapacity: 100
                                No users in this game, skipping
                                Cow health has been updated!""";

                assertEquals(expected, job.getLog());
        }

        @Test
        void test_throws_exception_when_get_num_cows_fails() {
                setupUpdateCowHealthTestOnGame(100, 1);
                game.setId(117);
                when(gameRepository.getNumCows(game.getId())).thenReturn(Optional.empty());
                when(gameRepository.getNumUsers(game.getId())).thenReturn(Optional.of(1));

                var thrown = Assertions.assertThrows(RuntimeException.class, this::runUpdateCowHealthJob);

                Assertions.assertEquals("Error calling getNumCows(117)", thrown.getMessage());
        }

        @Test
        void test_throws_exception_when_get_num_users_fails() {
                setupUpdateCowHealthTestOnGame(100, 1);
                game.setId(117);
                when(gameRepository.getNumUsers(game.getId())).thenReturn(Optional.empty());

                var thrown = Assertions.assertThrows(RuntimeException.class, this::runUpdateCowHealthJob);

                Assertions.assertEquals("Error calling getNumUsers(117)", thrown.getMessage());
        }

        @Test
        void getters_return_values_from_constructor() {
                var job = new UpdateCowHealthJob(gameRepository, farmerRepository,
                                userRepository, gamePlusBuilderService, herdRepository, random);

                assertSame(gamePlusBuilderService, job.getGamePlusBuilderService());
                assertSame(herdRepository, job.getHerdRepository());
                assertSame(random, job.getRandom());
        }
}
