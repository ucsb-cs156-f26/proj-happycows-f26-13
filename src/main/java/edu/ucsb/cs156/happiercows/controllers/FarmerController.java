package edu.ucsb.cs156.happiercows.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.repositories.GameRepository;
import edu.ucsb.cs156.happiercows.repositories.HerdRepository;
import edu.ucsb.cs156.happiercows.entities.User;
import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.FarmerActivity;
import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.entities.Herd;
import edu.ucsb.cs156.happiercows.errors.EntityNotFoundException;
import edu.ucsb.cs156.happiercows.errors.InvalidNumberOfCowsException;
import edu.ucsb.cs156.happiercows.errors.NoCowsException;
import edu.ucsb.cs156.happiercows.errors.NotEnoughMoneyException;
import edu.ucsb.cs156.happiercows.errors.GameHiddenException;
import edu.ucsb.cs156.happiercows.errors.NotEnrolledInCourseAssociatedWithGameException;
import edu.ucsb.cs156.happiercows.models.FarmerWithStudentStatus;
import edu.ucsb.cs156.happiercows.services.CourseAccessService;
import edu.ucsb.cs156.happiercows.services.FarmerActivityService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.transaction.annotation.Transactional;

@Tag(name = "User Game")
@RequestMapping("/api/farmer")
@RestController
public class FarmerController extends ApiController {

  @Autowired
  private FarmerRepository farmerRepository;

  @Autowired
  private GameRepository gameRepository;

  @Autowired
  private HerdRepository herdRepository;

  @Autowired
  ObjectMapper mapper;

  @Autowired
  private CourseAccessService courseAccessService;

  @Autowired
  private FarmerActivityService farmerActivityService;

  @Operation(summary = "Get a specific user game (admin only)")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("")
  public Farmer getFarmerById(
      @Parameter(name="userId") @RequestParam Long userId,
      @Parameter(name="gameId") @RequestParam Long gameId) throws JsonProcessingException {

    Farmer farmer = farmerRepository.findByGameIdAndUserId(gameId, userId)
        .orElseThrow(
            () -> new EntityNotFoundException(Farmer.class, "gameId", gameId, "userId", userId));
    return farmer;
  }

  @Operation(summary = "Get a user game for current user")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/forcurrentuser")
  public Farmer getFarmerById(
      @Parameter(name="gameId") @RequestParam Long gameId) throws JsonProcessingException {

    User u = getCurrentUser().getUser();
    Long userId = u.getId();
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new EntityNotFoundException("Game", gameId));
    ensureUserCanAccessCourseLinkedGame(u, game);
    Farmer farmer = farmerRepository.findByGameIdAndUserId(gameId, userId)
        .orElseThrow(
            () -> new EntityNotFoundException(Farmer.class, "gameId", gameId, "userId", userId));
    return farmer;
  }

  @Operation(summary = "Buy a herd of numCows cows (starting at 100 health), totalWealth updated")
  @PreAuthorize("hasRole('ROLE_USER')")
  @PutMapping("/buy")
  public ResponseEntity<String> putFarmerByIdBuy(
          @Parameter(name="gameId") @RequestParam Long gameId,
          @Parameter(name="numCows") @RequestParam int numCows) throws NotEnoughMoneyException, InvalidNumberOfCowsException, JsonProcessingException{

        if (numCows < 1) {
          throw new InvalidNumberOfCowsException("You must buy at least 1 cow!");
        }

        User u = getCurrentUser().getUser();
        Long userId = u.getId();

        Game game = gameRepository.findById(gameId).orElseThrow( 
          ()->new EntityNotFoundException("Game", gameId));

        ensureUserCanAccessCourseLinkedGame(u, game);

        if (game.isHidden()) {
          throw new GameHiddenException(gameId);
        }

        Farmer farmer = farmerRepository.findByGameIdAndUserId(gameId, userId)
        .orElseThrow(
            () -> new EntityNotFoundException(Farmer.class, "gameId", gameId, "userId", userId));

        double costPerCow = game.getCowPrice() + game.getPurchaseFee();
        if(farmer.getTotalWealth() >= (costPerCow * numCows)){
          farmer.setTotalWealth(farmer.getTotalWealth() - (costPerCow * numCows));
          farmer.setNumOfCows(farmer.getNumOfCows() + numCows);
          farmer.setCowsBought(farmer.getCowsBought() + numCows);
        }
        else{
          throw new NotEnoughMoneyException("You need more money!");
        }
        int herdNumber = herdRepository.findMaxHerdNumber(gameId, userId) + 1;
        Herd newHerd = Herd.builder().farmer(farmer).herdNumber(herdNumber).numCows(numCows).health(100).build();
        List<Herd> herds = new ArrayList<>();
        herdRepository.findByGameIdAndUserId(gameId, userId).forEach(herds::add);
        herds.add(newHerd);
        farmer.setCowHealth(averageHealth(herds));
        farmerRepository.save(farmer);
        herdRepository.save(newHerd);

        farmerActivityService.recordActivityIfStudentMatch(
            u, game, farmer, FarmerActivity.ACTIVITY_TYPE_BUY, numCows);

        String body = mapper.writeValueAsString(farmer);
        return ResponseEntity.ok().body(body);
    }

  /**
   * Sells cows from specific herds. The request body maps each herd id to the
   * number of cows to sell from that herd, e.g. {"12": 3, "15": 1}. Every herd
   * must belong to the current user in this game, and each count must be
   * between 0 and the number of cows in that herd. Each cow sells for the cow
   * price times its herd's health (as a percentage), minus the selling fee.
   * Herds that reach 0 cows are removed.
   */
  @Operation(summary = "Sell cows from specific herds, totalWealth updated")
  @PreAuthorize("hasRole('ROLE_USER')")
  @PutMapping("/sell")
  @Transactional
  public ResponseEntity<String> putFarmerByIdSell(
          @Parameter(name="gameId") @RequestParam Long gameId,
          @RequestBody Map<Long, Integer> herdCounts) throws NoCowsException, InvalidNumberOfCowsException, JsonProcessingException {
        User u = getCurrentUser().getUser();
        Long userId = u.getId();

        Game game = gameRepository.findById(gameId).orElseThrow( 
          ()->new EntityNotFoundException("Game", gameId));

        ensureUserCanAccessCourseLinkedGame(u, game);

        if (game.isHidden()) {
          throw new GameHiddenException(gameId);
        }

        Farmer farmer = farmerRepository.findByGameIdAndUserId(gameId, userId)
        .orElseThrow(
            () -> new EntityNotFoundException(Farmer.class, "gameId", gameId, "userId", userId));

        // Only the current user's herds in this game can be sold from.
        Map<Long, Herd> herdsById = new HashMap<>();
        for (Herd herd : herdRepository.findByGameIdAndUserId(gameId, userId)) {
          herdsById.put(herd.getId(), herd);
        }

        // Validate every requested count before changing anything.
        int totalCows = 0;
        double totalValue = 0;
        for (Map.Entry<Long, Integer> entry : herdCounts.entrySet()) {
          Herd herd = herdsById.get(entry.getKey());
          if (herd == null) {
            throw new EntityNotFoundException(Herd.class, entry.getKey());
          }
          Integer count = entry.getValue();
          if (count == null || count < 0) {
            throw new InvalidNumberOfCowsException("You cannot sell a negative number of cows!");
          }
          if (count > herd.getNumCows()) {
            throw new NoCowsException("You do not have enough cows in that herd to sell!");
          }
          double cowValue = game.getCowPrice() * herd.getHealth() / 100
              - game.getSellingFee();
          totalCows += count;
          totalValue += cowValue * count;
        }
        if (totalCows < 1) {
          throw new InvalidNumberOfCowsException("You must sell at least 1 cow!");
        }

        for (Map.Entry<Long, Integer> entry : herdCounts.entrySet()) {
          Herd herd = herdsById.get(entry.getKey());
          int remaining = herd.getNumCows() - entry.getValue();
          if (remaining == 0) {
            herdRepository.delete(herd);
            herdsById.remove(herd.getId());
          } else if (remaining < herd.getNumCows()) {
            herd.setNumCows(remaining);
            herdRepository.save(herd);
          }
        }

        farmer.setTotalWealth(farmer.getTotalWealth() + totalValue);
        farmer.setNumOfCows(farmer.getNumOfCows() - totalCows);
        farmer.setCowsSold(farmer.getCowsSold() + totalCows);
        farmer.setCowHealth(averageHealth(herdsById.values()));
        farmerRepository.save(farmer);

        farmerActivityService.recordActivityIfStudentMatch(
            u, game, farmer, FarmerActivity.ACTIVITY_TYPE_SELL, totalCows);

        String body = mapper.writeValueAsString(farmer);
        return ResponseEntity.ok().body(body);
    }

    

    /**
     * The farmer's cow health shown in Farm Stats: the average health of their
     * herds, weighted by the number of cows in each herd. A farmer with no
     * cows has health 100, matching the daily health update.
     */
    static double averageHealth(Collection<Herd> herds) {
        int cows = 0;
        double totalHealth = 0;
        for (Herd herd : herds) {
            cows += herd.getNumCows();
            totalHealth += herd.getHealth() * herd.getNumCows();
        }
        return cows > 0 ? totalHealth / cows : 100.0;
    }

    @Operation(summary = "Get all user game for a specific game")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @GetMapping("/game/all")
    public List<FarmerWithStudentStatus> getFarmersByGameId(
        @Parameter(name="gameId") @RequestParam Long gameId) {

      Game game = gameRepository.findById(gameId)
          .orElseThrow(() -> new EntityNotFoundException("Game", gameId));

      List<FarmerWithStudentStatus> result = new ArrayList<>();
      for (Farmer farmer : farmerRepository.findByGameId(gameId)) {
        boolean isStudent = courseAccessService
            .findMatchingStudentForCourseLinkedGame(farmer.getUser(), game)
            .isPresent();

        result.add(FarmerWithStudentStatus.builder()
            .userId(farmer.getUserId())
            .gameId(farmer.getGameId())
            .username(farmer.getUsername())
            .totalWealth(farmer.getTotalWealth())
            .numOfCows(farmer.getNumOfCows())
            .cowHealth(farmer.getCowHealth())
            .cowsBought(farmer.getCowsBought())
            .cowsSold(farmer.getCowsSold())
            .cowDeaths(farmer.getCowDeaths())
            .student(isStudent)
            .build());
      }
      return result;
  }

  private void ensureUserCanAccessCourseLinkedGame(User user, Game game) {
    if (game.getCourseId() != null && !courseAccessService.isEligibleForGame(user, game)) {
      throw new NotEnrolledInCourseAssociatedWithGameException();
    }
  }

}