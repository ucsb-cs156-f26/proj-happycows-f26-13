package edu.ucsb.cs156.happiercows.jobs;

import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.entities.GamePlus;
import edu.ucsb.cs156.happiercows.entities.User;
import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.Herd;
import edu.ucsb.cs156.happiercows.repositories.GameRepository;
import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.repositories.HerdRepository;
import edu.ucsb.cs156.happiercows.repositories.UserRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import edu.ucsb.cs156.happiercows.services.GamePlusBuilderService;
import edu.ucsb.cs156.happiercows.strategies.CowHealthUpdateStrategy;
import java.util.function.DoubleSupplier;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class UpdateCowHealthJob implements JobContextConsumer {

    @Getter
    private GameRepository gameRepository;
    @Getter
    private FarmerRepository farmerRepository;
    @Getter
    private UserRepository userRepository;
    @Getter
    private GamePlusBuilderService gamePlusBuilderService;
    @Getter
    private HerdRepository herdRepository;
    // Source of random numbers in [0, 1) for cow deaths; replaced in tests.
    @Getter
    private DoubleSupplier random;

    @Override
    public void accept(JobContext ctx) throws Exception {
        ctx.log("Updating cow health...");


        Iterable<Game> allGame = gameRepository.findAll();
        Iterable<GamePlus> allGamePlus = gamePlusBuilderService.convertToGamePlus(allGame);

        for (GamePlus gamePlus : allGamePlus) {


            Game game = gamePlus.getGame();

            if (!GameGate.shouldProcess(game, gameRepository, ctx)) {
                continue;
            }

            runUpdateJobInGame(game, gamePlus, gamePlusBuilderService, gameRepository, farmerRepository, herdRepository, random, ctx);

        }

        ctx.log("Cow health has been updated!");
    }

    // Cows in a herd below this health may die (see docs/gamePlay.md).
    public static final double DEATH_HEALTH_THRESHOLD = 30.0;
    // Chance that each cow in a herd below the threshold dies during one update.
    public static final double DEATH_CHANCE_PER_COW = 0.01;

    // exposed for testing
    public static double calculateNewCowHealthUsingStrategy(
            CowHealthUpdateStrategy strategy,
            GamePlus gamePlus,
            double cowHealth,
            int totalCows
    ) {
        var health = strategy.calculateNewCowHealth(gamePlus, cowHealth, totalCows);
        return Math.max(0, Math.min(health, 100));
    }

    /**
     * Removes the cows that die in this herd during one update, and returns how
     * many died. A herd at 0 health loses all of its cows. A herd below
     * DEATH_HEALTH_THRESHOLD loses each cow with chance DEATH_CHANCE_PER_COW,
     * using random (a source of numbers in [0, 1)) so tests can control it.
     */
    public static int calculateCowDeaths(Herd herd, DoubleSupplier random) {
        int deaths = 0;
        if (herd.getHealth() == 0.0) {
            deaths = herd.getNumCows();
        } else if (herd.getHealth() < DEATH_HEALTH_THRESHOLD) {
            for (int i = 0; i < herd.getNumCows(); i++) {
                if (random.getAsDouble() < DEATH_CHANCE_PER_COW) {
                    deaths++;
                }
            }
        }
        herd.setNumCows(herd.getNumCows() - deaths);
        return deaths;
    }

    public static void runUpdateJobInGame(Game game, GamePlus gamePlus, GamePlusBuilderService gamePlusBuilderService,
            GameRepository gameRepository, FarmerRepository farmerRepository, HerdRepository herdRepository,
            DoubleSupplier random, JobContext ctx) {
        ctx.log("Game " + game.getName() + ", degradationRate: " + game.getDegradationRate() + ", effectiveCapacity: " + gamePlus.getEffectiveCapacity());

        int numUsers = gameRepository.getNumUsers(game.getId()).orElseThrow(() -> new RuntimeException("Error calling getNumUsers(" + game.getId() + ")"));

        if (numUsers == 0) {
            ctx.log("No users in this game, skipping");
            return;
        }

        int carryingCapacity = gamePlus.getEffectiveCapacity();
        Iterable<Farmer> allFarmer = farmerRepository.findByGameId(game.getId());

        Integer totalCows = gameRepository.getNumCows(game.getId()).orElseThrow(() -> new RuntimeException("Error calling getNumCows(" + game.getId() + ")"));

        var isAboveCapacity = totalCows > carryingCapacity;
        var cowHealthUpdateStrategy = isAboveCapacity ? game.getAboveCapacityHealthUpdateStrategy() : game.getBelowCapacityHealthUpdateStrategy();
        GamePlus currentGamePlus = gamePlusBuilderService.toGamePlus(game);

        for (Farmer farmer : allFarmer) {
            User user = farmer.getUser();
            ctx.log("User: " + user.getFullName() + ", numCows: " + farmer.getNumOfCows() + ", cowHealth: " + farmer.getCowHealth());

            int survivingCows = 0;
            double totalHealth = 0;
            for (Herd herd : herdRepository.findByGameIdAndUserId(game.getId(), farmer.getUserId())) {
                double oldHealth = herd.getHealth();
                herd.setHealth(calculateNewCowHealthUsingStrategy(cowHealthUpdateStrategy, currentGamePlus, oldHealth, totalCows));
                int deaths = calculateCowDeaths(herd, random);
                farmer.setCowDeaths(farmer.getCowDeaths() + deaths);
                ctx.log(" Herd #" + herd.getHerdNumber() + ": old health: " + oldHealth + ", new health: " + herd.getHealth()
                        + ", cows died: " + deaths + ", cows left: " + herd.getNumCows());

                if (herd.getNumCows() == 0) {
                    herdRepository.delete(herd);
                } else {
                    herdRepository.save(herd);
                    survivingCows += herd.getNumCows();
                    totalHealth += herd.getHealth() * herd.getNumCows();
                }
            }

            // Keep the farmer's totals in step with their herds: total cows and
            // average health weighted by cow count (100 when they have no cows,
            // the health a newly bought herd starts at).
            farmer.setNumOfCows(survivingCows);
            farmer.setCowHealth(survivingCows > 0 ? totalHealth / survivingCows : 100.0);
            ctx.log(" new numCows: " + farmer.getNumOfCows() + ", new cowHealth: " + farmer.getCowHealth());
            farmerRepository.save(farmer);
        }
    }
}
