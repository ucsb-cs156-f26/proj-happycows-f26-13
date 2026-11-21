package edu.ucsb.cs156.happiercows.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.entities.Herd;
import edu.ucsb.cs156.happiercows.entities.User;
import edu.ucsb.cs156.happiercows.errors.EntityNotFoundException;
import edu.ucsb.cs156.happiercows.errors.NotEnrolledInCourseAssociatedWithGameException;
import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.repositories.GameRepository;
import edu.ucsb.cs156.happiercows.repositories.HerdRepository;
import edu.ucsb.cs156.happiercows.services.CourseAccessService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Herds")
@RequestMapping("/api/herds")
@RestController
public class HerdController extends ApiController {

  @Autowired
  private HerdRepository herdRepository;

  @Autowired
  private FarmerRepository farmerRepository;

  @Autowired
  private GameRepository gameRepository;

  @Autowired
  private CourseAccessService courseAccessService;

  @Operation(summary = "Get the herds of a specific farmer in a game (admin only)")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("")
  public Iterable<Herd> getHerdsByGameIdAndUserId(
      @Parameter(name = "gameId") @RequestParam Long gameId,
      @Parameter(name = "userId") @RequestParam Long userId) {

    farmerRepository.findByGameIdAndUserId(gameId, userId)
        .orElseThrow(
            () -> new EntityNotFoundException(Farmer.class, "gameId", gameId, "userId", userId));
    return herdRepository.findByGameIdAndUserId(gameId, userId);
  }

  /**
   * The userId is always taken from the logged-in user, never from a request
   * parameter, so a farmer can only ever read their own herds.
   */
  @Operation(summary = "Get the current user's herds in a game")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/forcurrentuser")
  public Iterable<Herd> getHerdsForCurrentUser(
      @Parameter(name = "gameId") @RequestParam Long gameId) {

    User u = getCurrentUser().getUser();
    Long userId = u.getId();
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new EntityNotFoundException("Game", gameId));
    if (game.getCourseId() != null && !courseAccessService.isEligibleForGame(u, game)) {
      throw new NotEnrolledInCourseAssociatedWithGameException();
    }
    farmerRepository.findByGameIdAndUserId(gameId, userId)
        .orElseThrow(
            () -> new EntityNotFoundException(Farmer.class, "gameId", gameId, "userId", userId));
    return herdRepository.findByGameIdAndUserId(gameId, userId);
  }
}
