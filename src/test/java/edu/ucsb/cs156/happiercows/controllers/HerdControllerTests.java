package edu.ucsb.cs156.happiercows.controllers;

import edu.ucsb.cs156.happiercows.ControllerTestCase;
import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.entities.Herd;
import edu.ucsb.cs156.happiercows.entities.User;
import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.repositories.GameRepository;
import edu.ucsb.cs156.happiercows.repositories.HerdRepository;
import edu.ucsb.cs156.happiercows.repositories.UserRepository;
import edu.ucsb.cs156.happiercows.services.CourseAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HerdController.class)
public class HerdControllerTests extends ControllerTestCase {

    @MockBean
    HerdRepository herdRepository;

    @MockBean
    FarmerRepository farmerRepository;

    @MockBean
    GameRepository gameRepository;

    @MockBean
    UserRepository userRepository;

    @MockBean
    CourseAccessService courseAccessService;

    Game testGame = Game.builder()
            .id(1L)
            .name("test game")
            .startingDate(LocalDateTime.now())
            .build();

    private Farmer farmerFor(User user) {
        return Farmer.builder().user(user).game(testGame).numOfCows(8).cowHealth(90).build();
    }

    private List<Herd> herdsFor(Farmer farmer) {
        return List.of(
                Herd.builder().id(1L).farmer(farmer).numCows(5).health(90).build(),
                Herd.builder().id(2L).farmer(farmer).numCows(3).health(60).build());
    }

    // ---------- GET /api/herds (admin) ----------

    @Test
    public void logged_out_users_cannot_get_herds_admin() throws Exception {
        mockMvc.perform(get("/api/herds?gameId=1&userId=1"))
                .andExpect(status().is(403));
        verifyNoInteractions(herdRepository);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void regular_users_cannot_get_another_farmers_herds_via_admin_endpoint() throws Exception {
        mockMvc.perform(get("/api/herds?gameId=1&userId=2"))
                .andExpect(status().is(403));
        verifyNoInteractions(herdRepository);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void admin_can_get_herds_for_a_farmer() throws Exception {
        User other = User.builder().id(2L).build();
        Farmer farmer = farmerFor(other);
        List<Herd> herds = herdsFor(farmer);
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(2L))).thenReturn(Optional.of(farmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(2L))).thenReturn(herds);

        MvcResult response = mockMvc.perform(get("/api/herds?gameId=1&userId=2"))
                .andExpect(status().isOk()).andReturn();

        verify(herdRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(2L));
        assertEquals(mapper.writeValueAsString(herds), response.getResponse().getContentAsString());
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void admin_gets_404_when_farmer_does_not_exist() throws Exception {
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(2L))).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/herds?gameId=1&userId=2"))
                .andExpect(status().isNotFound()).andReturn();

        verifyNoInteractions(herdRepository);
        Map<String, Object> expected = Map.of(
                "message", "Farmer with gameId 1 and userId 2 not found",
                "type", "EntityNotFoundException");
        assertEquals(expected, responseToJson(response));
    }

    // ---------- GET /api/herds/forcurrentuser ----------

    @Test
    public void logged_out_users_cannot_get_herds_for_current_user() throws Exception {
        mockMvc.perform(get("/api/herds/forcurrentuser?gameId=1"))
                .andExpect(status().is(403));
        verifyNoInteractions(herdRepository);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void user_gets_their_own_herds() throws Exception {
        User currentUser = currentUserService.getUser();
        Farmer farmer = farmerFor(currentUser);
        List<Herd> herds = herdsFor(farmer);
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(currentUser.getId())))
                .thenReturn(Optional.of(farmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(currentUser.getId()))).thenReturn(herds);

        MvcResult response = mockMvc.perform(get("/api/herds/forcurrentuser?gameId=1"))
                .andExpect(status().isOk()).andReturn();

        verify(herdRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(currentUser.getId()));
        verify(courseAccessService, never()).isEligibleForGame(any(), any());
        assertEquals(mapper.writeValueAsString(herds), response.getResponse().getContentAsString());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void user_cannot_read_another_farmers_herds_by_passing_userId() throws Exception {
        User currentUser = currentUserService.getUser();
        Farmer farmer = farmerFor(currentUser);
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(currentUser.getId())))
                .thenReturn(Optional.of(farmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(currentUser.getId()))).thenReturn(List.of());

        mockMvc.perform(get("/api/herds/forcurrentuser?gameId=1&userId=999"))
                .andExpect(status().isOk());

        verify(herdRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(currentUser.getId()));
        verify(herdRepository, never()).findByGameIdAndUserId(anyLong(), eq(999L));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void user_gets_404_when_game_does_not_exist() throws Exception {
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/herds/forcurrentuser?gameId=1"))
                .andExpect(status().isNotFound()).andReturn();

        verifyNoInteractions(herdRepository);
        Map<String, Object> expected = Map.of(
                "message", "Game with id 1 not found",
                "type", "EntityNotFoundException");
        assertEquals(expected, responseToJson(response));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void user_gets_404_when_not_a_farmer_in_the_game() throws Exception {
        User currentUser = currentUserService.getUser();
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(currentUser.getId())))
                .thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/herds/forcurrentuser?gameId=1"))
                .andExpect(status().isNotFound()).andReturn();

        verifyNoInteractions(herdRepository);
        Map<String, Object> expected = Map.of(
                "message", "Farmer with gameId 1 and userId " + currentUser.getId() + " not found",
                "type", "EntityNotFoundException");
        assertEquals(expected, responseToJson(response));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void user_not_enrolled_in_linked_course_is_forbidden() throws Exception {
        Game courseGame = Game.builder().id(1L).name("course game").courseId(17L).build();
        User currentUser = currentUserService.getUser();
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(courseGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(courseGame))).thenReturn(false);

        MvcResult response = mockMvc.perform(get("/api/herds/forcurrentuser?gameId=1"))
                .andExpect(status().isForbidden()).andReturn();

        verify(farmerRepository, never()).findByGameIdAndUserId(anyLong(), anyLong());
        verifyNoInteractions(herdRepository);
        Map<String, Object> expected = Map.of(
                "message", "Not enrolled in course associated with game",
                "type", "NotEnrolledInCourseAssociatedWithGameException");
        assertEquals(expected, responseToJson(response));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void user_enrolled_in_linked_course_gets_herds() throws Exception {
        Game courseGame = Game.builder().id(1L).name("course game").courseId(17L).build();
        User currentUser = currentUserService.getUser();
        Farmer farmer = Farmer.builder().user(currentUser).game(courseGame).build();
        List<Herd> herds = herdsFor(farmer);
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(courseGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(courseGame))).thenReturn(true);
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(currentUser.getId())))
                .thenReturn(Optional.of(farmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(currentUser.getId()))).thenReturn(herds);

        MvcResult response = mockMvc.perform(get("/api/herds/forcurrentuser?gameId=1"))
                .andExpect(status().isOk()).andReturn();

        assertEquals(mapper.writeValueAsString(herds), response.getResponse().getContentAsString());
    }
}
