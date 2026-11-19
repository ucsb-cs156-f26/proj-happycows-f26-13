package edu.ucsb.cs156.happiercows.controllers;

import edu.ucsb.cs156.happiercows.ControllerTestCase;
import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.entities.User;
import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.Herd;
import edu.ucsb.cs156.happiercows.repositories.HerdRepository;
import edu.ucsb.cs156.happiercows.repositories.GameRepository;
import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.repositories.UserRepository;
import edu.ucsb.cs156.happiercows.entities.FarmerActivity;
import edu.ucsb.cs156.happiercows.entities.Student;
import edu.ucsb.cs156.happiercows.models.FarmerWithStudentStatus;
import edu.ucsb.cs156.happiercows.services.CourseAccessService;
import edu.ucsb.cs156.happiercows.services.FarmerActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FarmerController.class)
public class FarmerControllerTests extends ControllerTestCase {

    @MockBean
    FarmerRepository farmerRepository;

    @MockBean
    UserRepository userRepository;

    @MockBean
    GameRepository gameRepository;

    @MockBean
    HerdRepository herdRepository;

    @MockBean
    CourseAccessService courseAccessService;

    @MockBean
    FarmerActivityService farmerActivityService;

    Game testGame = Game
            .builder()
            .name("test game")
            .cowPrice(10)
            .milkPrice(2)
            .startingBalance(300)
            .startingDate(LocalDateTime.now())
            .build();

    public Farmer getTestFarmer() {
        return Farmer.builder()
                .user(currentUserService.getUser())
                .game(testGame)
                .totalWealth(300)
                .numOfCows(1)
                .cowHealth(100)
                .build();
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void test_getFarmerById_exists_admin() throws Exception {

        Farmer expectedFarmer = getTestFarmer();
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(expectedFarmer));

        MvcResult response = mockMvc.perform(get("/api/farmer?userId=1&gameId=1"))
                .andExpect(status().isOk()).andReturn();

        verify(farmerRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(1L));

        String expectedJson = mapper.writeValueAsString(expectedFarmer);
        String responseString = response.getResponse().getContentAsString();

        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void test_getFarmerById_nonexists_admin() throws Exception {

        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/farmer?userId=1&gameId=1"))
                .andExpect(status().is(404)).andReturn();

        verify(farmerRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(1L));

        String expectedString = "{\"message\":\"Farmer with gameId 1 and userId 1 not found\",\"type\":\"EntityNotFoundException\"}";

        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_getFarmerById_exists() throws Exception {

        Farmer expectedFarmer = getTestFarmer();
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(expectedFarmer));

        MvcResult response = mockMvc.perform(get("/api/farmer/forcurrentuser?gameId=1"))
                .andExpect(status().isOk()).andReturn();

        verify(gameRepository, times(1)).findById(eq(1L));
        verify(farmerRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(1L));

        String expectedJson = mapper.writeValueAsString(expectedFarmer);
        String responseString = response.getResponse().getContentAsString();

        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_getFarmerById_nonexists() throws Exception {

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/farmer/forcurrentuser?gameId=1"))
                .andExpect(status().is(404)).andReturn();

        verify(gameRepository, times(1)).findById(eq(1L));
        verify(farmerRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(1L));

        String responseString = response.getResponse().getContentAsString();
        String expectedString = "{\"message\":\"Farmer with gameId 1 and userId 1 not found\",\"type\":\"EntityNotFoundException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_game_exists() throws Exception {

        // arrange

        User currentUser = currentUserService.getUser();

        Farmer origFarmer = getTestFarmer();
        origFarmer.setCowsBought(1);

        Farmer updateFarmer = getTestFarmer();
        updateFarmer.setNumOfCows(3);
        updateFarmer.setTotalWealth(300 - (testGame.getCowPrice() * 2));
        updateFarmer.setCowsBought(3);

        String expectedReturn = mapper.writeValueAsString(updateFarmer);

        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        // the farmer already has herds #1 and #2 in this game
        when(herdRepository.findMaxHerdNumber(eq(1L), eq(1L))).thenReturn(2);

        // act
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=2")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(farmerRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(1L));
        verify(farmerRepository, times(1)).save(updateFarmer);
        verify(herdRepository, times(1)).save(
                Herd.builder().farmer(updateFarmer).herdNumber(3).numCows(2).health(100).build());
        verify(farmerActivityService, times(1)).recordActivityIfStudentMatch(
                eq(currentUser), eq(testGame), eq(updateFarmer),
                eq(FarmerActivity.ACTIVITY_TYPE_BUY), eq(2));
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedReturn, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_zero_cows_rejected() throws Exception {
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=0")
                        .with(csrf()))
                .andExpect(status().is(400)).andReturn();

        String expectedString = "{\"message\":\"You must buy at least 1 cow!\",\"type\":\"InvalidNumberOfCowsException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        assertEquals(expectedJson, responseToJson(response));
        verify(farmerRepository, times(0)).save(any());
        verify(herdRepository, times(0)).save(any());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_negative_cows_rejected() throws Exception {
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=-3")
                        .with(csrf()))
                .andExpect(status().is(400)).andReturn();

        String expectedString = "{\"message\":\"You must buy at least 1 cow!\",\"type\":\"InvalidNumberOfCowsException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        assertEquals(expectedJson, responseToJson(response));
        verify(farmerRepository, times(0)).save(any());
        verify(herdRepository, times(0)).save(any());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_buyCow_chargesPurchaseFeePerCow() throws Exception {
        testGame.setCowPrice(100);
        testGame.setPurchaseFee(1);

        Farmer origFarmer = getTestFarmer();
        origFarmer.setTotalWealth(300);

        Farmer updatedFarmer = getTestFarmer();
        updatedFarmer.setTotalWealth(98);
        updatedFarmer.setNumOfCows(3);
        updatedFarmer.setCowsBought(2);

        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L)))
                .thenReturn(Optional.of(origFarmer));
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));

        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=2")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        verify(farmerRepository, times(1)).save(updatedFarmer);
        assertEquals(mapper.writeValueAsString(updatedFarmer),
                response.getResponse().getContentAsString());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_getFarmerById_course_linked_game_requires_current_enrollment() throws Exception {
        testGame.setCourseId(17L);
        User currentUser = currentUserService.getUser();

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(testGame))).thenReturn(false);

        MvcResult response = mockMvc.perform(get("/api/farmer/forcurrentuser?gameId=1"))
                .andExpect(status().isForbidden()).andReturn();

        verify(gameRepository, times(1)).findById(eq(1L));
        verify(courseAccessService, times(1)).isEligibleForGame(eq(currentUser), eq(testGame));
        verify(farmerRepository, times(0)).findByGameIdAndUserId(anyLong(), anyLong());

        String expectedString = "{\"message\":\"Not enrolled in course associated with game\",\"type\":\"NotEnrolledInCourseAssociatedWithGameException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_getFarmerById_course_linked_game_allows_current_enrollment() throws Exception {
        testGame.setCourseId(17L);
        User currentUser = currentUserService.getUser();
        Farmer expectedFarmer = getTestFarmer();

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(testGame))).thenReturn(true);
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(expectedFarmer));

        MvcResult response = mockMvc.perform(get("/api/farmer/forcurrentuser?gameId=1"))
                .andExpect(status().isOk()).andReturn();

        verify(gameRepository, times(1)).findById(eq(1L));
        verify(courseAccessService, times(1)).isEligibleForGame(eq(currentUser), eq(testGame));
        verify(farmerRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(1L));

        String expectedJson = mapper.writeValueAsString(expectedFarmer);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_getFarmerById_when_game_does_not_exist() throws Exception {
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/farmer/forcurrentuser?gameId=1"))
                .andExpect(status().isNotFound()).andReturn();

        verify(gameRepository, times(1)).findById(eq(1L));
        verify(farmerRepository, times(0)).findByGameIdAndUserId(anyLong(), anyLong());

        String expectedString = "{\"message\":\"Game with id 1 not found\",\"type\":\"EntityNotFoundException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_game_exists_user_has_exact_amount_needed() throws Exception {
        // arrange

        testGame.setCowPrice(300);

        Farmer origFarmer = getTestFarmer();
        origFarmer.setTotalWealth(300);
        origFarmer.setNumOfCows(1);
        origFarmer.setCowsBought(1);

        Farmer updatedFarmer = getTestFarmer();
        updatedFarmer.setTotalWealth(0);
        updatedFarmer.setNumOfCows(2);
        updatedFarmer.setCowsBought(2);

        String expectedReturn = mapper.writeValueAsString(updatedFarmer);

        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));

        // act
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=1")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(farmerRepository, times(1)).findByGameIdAndUserId(eq(1L), eq(1L));
        verify(farmerRepository, times(1)).save(updatedFarmer);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedReturn, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_buyCow_for_user_not_in_game() throws Exception {
        when(gameRepository.findById(234L)).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.empty());
        // act
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=234&numCows=2")
                        .with(csrf()))
                .andExpect(status().is(404)).andReturn();

        // assert

        String expectedString = "{\"message\":\"Farmer with gameId 234 and userId 1 not found\",\"type\":\"EntityNotFoundException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_buyCow_course_linked_game_requires_current_enrollment() throws Exception {
        testGame.setCourseId(17L);
        User currentUser = currentUserService.getUser();

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(testGame))).thenReturn(false);

        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=2")
                        .with(csrf()))
                .andExpect(status().isForbidden()).andReturn();

        verify(courseAccessService, times(1)).isEligibleForGame(eq(currentUser), eq(testGame));
        verify(farmerRepository, times(0)).findByGameIdAndUserId(anyLong(), anyLong());

        String expectedString = "{\"message\":\"Not enrolled in course associated with game\",\"type\":\"NotEnrolledInCourseAssociatedWithGameException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_course_linked_game_allows_current_enrollment() throws Exception {
        testGame.setCourseId(17L);
        User currentUser = currentUserService.getUser();

        Farmer origFarmer = getTestFarmer();
        origFarmer.setCowsBought(1);

        Farmer updatedFarmer = getTestFarmer();
        updatedFarmer.setNumOfCows(3);
        updatedFarmer.setTotalWealth(300 - (testGame.getCowPrice() * 2));
        updatedFarmer.setCowsBought(3);

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(testGame))).thenReturn(true);
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));

        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=2")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        verify(courseAccessService, times(1)).isEligibleForGame(eq(currentUser), eq(testGame));
        verify(farmerRepository, times(1)).save(updatedFarmer);

        String expectedReturn = mapper.writeValueAsString(updatedFarmer);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedReturn, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_buyCow_game_does_not_exist() throws Exception {
        when(gameRepository.findById(234L)).thenReturn(Optional.empty());
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(getTestFarmer()));

        // act
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=234&numCows=3")
                        .with(csrf()))
                .andExpect(status().is(404)).andReturn();

        // assert
        String expectedString = "{\"message\":\"Game with id 234 not found\",\"type\":\"EntityNotFoundException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    // Put tests for edge cases (not enough money to buy, or no cow to sell)
    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_game_exists_not_enough_money() throws Exception {

        // arrange
        Farmer origFarmer = getTestFarmer();
        origFarmer.setTotalWealth(5);

        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));

        // act
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=1")
                        .with(csrf()))
                .andExpect(status().is(400)).andReturn();

        // assert
        String expectedString = "{\"message\":\"You need more money!\",\"type\":\"NotEnoughMoneyException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
        verify(herdRepository, times(0)).save(any());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_not_enough_money() throws Exception {

        // arrange
        testGame.setCowPrice(100);

        Farmer origFarmer = getTestFarmer();
        origFarmer.setCowsBought(1);
        origFarmer.setTotalWealth(100);

        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));

        // act
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=3")
                        .with(csrf()))
                .andExpect(status().is(400)).andReturn();

        // assert
        String expectedString = "{\"message\":\"You need more money!\",\"type\":\"NotEnoughMoneyException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }


    @WithMockUser(roles = {"USER"})
    @Test
    public void test_getAllFarmerById_exists() throws Exception {
        Farmer testexpectedFarmer = getTestFarmer();
        List<Farmer> expectedFarmer = new ArrayList<>();
        expectedFarmer.add(testexpectedFarmer);
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameId(eq(1L))).thenReturn(expectedFarmer);
        when(courseAccessService.findMatchingStudentForCourseLinkedGame(
                testexpectedFarmer.getUser(), testGame)).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/farmer/game/all?gameId=1"))
                .andExpect(status().isOk()).andReturn();

        verify(farmerRepository, times(1)).findByGameId(eq(1L));

        FarmerWithStudentStatus expected = FarmerWithStudentStatus.builder()
                .userId(testexpectedFarmer.getUserId())
                .gameId(testexpectedFarmer.getGameId())
                .username(testexpectedFarmer.getUsername())
                .totalWealth(testexpectedFarmer.getTotalWealth())
                .numOfCows(testexpectedFarmer.getNumOfCows())
                .cowHealth(testexpectedFarmer.getCowHealth())
                .cowsBought(testexpectedFarmer.getCowsBought())
                .cowsSold(testexpectedFarmer.getCowsSold())
                .cowDeaths(testexpectedFarmer.getCowDeaths())
                .student(false)
                .build();
        String expectedJson = mapper.writeValueAsString(List.of(expected));
        String responseString = response.getResponse().getContentAsString();

        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void test_Admin_getAllFarmerById_exists() throws Exception {
        Farmer testexpectedFarmer = getTestFarmer();
        List<Farmer> expectedFarmer = new ArrayList<>();
        expectedFarmer.add(testexpectedFarmer);
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameId(eq(1L))).thenReturn(expectedFarmer);
        when(courseAccessService.findMatchingStudentForCourseLinkedGame(
                testexpectedFarmer.getUser(), testGame)).thenReturn(Optional.of(
                        Student.builder().id(42L).build()));

        MvcResult response = mockMvc.perform(get("/api/farmer/game/all?gameId=1").with(csrf()))
                .andExpect(status().isOk()).andReturn();

        verify(farmerRepository, times(1)).findByGameId(eq(1L));

        FarmerWithStudentStatus expected = FarmerWithStudentStatus.builder()
                .userId(testexpectedFarmer.getUserId())
                .gameId(testexpectedFarmer.getGameId())
                .username(testexpectedFarmer.getUsername())
                .totalWealth(testexpectedFarmer.getTotalWealth())
                .numOfCows(testexpectedFarmer.getNumOfCows())
                .cowHealth(testexpectedFarmer.getCowHealth())
                .cowsBought(testexpectedFarmer.getCowsBought())
                .cowsSold(testexpectedFarmer.getCowsSold())
                .cowDeaths(testexpectedFarmer.getCowDeaths())
                .student(true)
                .build();
        String expectedJson = mapper.writeValueAsString(List.of(expected));
        String responseString = response.getResponse().getContentAsString();

        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_getAllFarmerById_game_does_not_exist() throws Exception {
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.empty());

        MvcResult response = mockMvc.perform(get("/api/farmer/game/all?gameId=1"))
                .andExpect(status().is(404)).andReturn();

        verify(farmerRepository, times(0)).findByGameId(anyLong());

        String expectedString = "{\"message\":\"Game with id 1 not found\",\"type\":\"EntityNotFoundException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_BuyCow_hidden_game() throws Exception {
        // arrange
        testGame.setHidden(true);
        Farmer origFarmer = getTestFarmer();

        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));

        // act
        MvcResult response = mockMvc.perform(put("/api/farmer/buy?gameId=1&numCows=1")
                        .with(csrf()))
                .andExpect(status().is(400)).andReturn();

        // assert
        String expectedString = "{\"message\":\"Game with id 1 is hidden\",\"type\":\"GameHiddenException\"}";
        Map<String, Object> expectedJson = mapper.readValue(expectedString, Map.class);
        Map<String, Object> jsonResponse = responseToJson(response);
        assertEquals(expectedJson, jsonResponse);
    }

    // ---------- Selling cows from herds ----------

    private Herd testHerd(long id, int numCows, double health, Farmer farmer) {
        return Herd.builder().id(id).farmer(farmer).numCows(numCows).health(health).build();
    }

    private MvcResult performSell(String gameId, String body, int expectedStatus) throws Exception {
        return mockMvc.perform(put("/api/farmer/sell?gameId=" + gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    private void assertErrorResponse(MvcResult response, String message, String type) throws Exception {
        Map<String, Object> expectedJson = Map.of("message", message, "type", type);
        assertEquals(expectedJson, responseToJson(response));
    }

    private void verifyNothingSaved() {
        verify(herdRepository, times(0)).save(any());
        verify(herdRepository, times(0)).delete(any());
        verify(farmerRepository, times(0)).save(any());
        verify(farmerActivityService, times(0)).recordActivityIfStudentMatch(any(), any(), any(), anyInt(), anyInt());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_from_several_herds_uses_each_herds_health() throws Exception {
        testGame.setSellingFee(1);
        User currentUser = currentUserService.getUser();

        Farmer origFarmer = getTestFarmer();
        origFarmer.setNumOfCows(10);
        origFarmer.setCowsSold(1);
        Herd herd11 = testHerd(11L, 3, 100, origFarmer);
        Herd herd12 = testHerd(12L, 7, 50, origFarmer);

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(List.of(herd11, herd12));

        // herd 11: 3 cows * (10 * 100% - 1) = 27; herd 12: 2 cows * (10 * 50% - 1) = 8
        Farmer updatedFarmer = getTestFarmer();
        updatedFarmer.setTotalWealth(300 + 27 + 8);
        updatedFarmer.setNumOfCows(5);
        updatedFarmer.setCowsSold(6);

        MvcResult response = performSell("1", "{\"11\": 3, \"12\": 2}", 200);

        verify(herdRepository, times(1)).delete(testHerd(11L, 3, 100, origFarmer));
        verify(herdRepository, times(1)).save(testHerd(12L, 5, 50, origFarmer));
        verify(farmerRepository, times(1)).save(updatedFarmer);
        verify(farmerActivityService, times(1)).recordActivityIfStudentMatch(
                eq(currentUser), eq(testGame), eq(updatedFarmer),
                eq(FarmerActivity.ACTIVITY_TYPE_SELL), eq(5));
        assertEquals(mapper.writeValueAsString(updatedFarmer), response.getResponse().getContentAsString());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_herd_with_zero_requested_is_left_unchanged() throws Exception {
        Farmer origFarmer = getTestFarmer();
        origFarmer.setNumOfCows(10);
        Herd herd11 = testHerd(11L, 3, 100, origFarmer);
        Herd herd12 = testHerd(12L, 7, 100, origFarmer);

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(List.of(herd11, herd12));

        Farmer updatedFarmer = getTestFarmer();
        updatedFarmer.setTotalWealth(310);
        updatedFarmer.setNumOfCows(9);
        updatedFarmer.setCowsSold(1);

        performSell("1", "{\"11\": 0, \"12\": 1}", 200);

        verify(herdRepository, times(0)).delete(any());
        verify(herdRepository, times(1)).save(any());
        verify(herdRepository, times(1)).save(testHerd(12L, 6, 100, origFarmer));
        verify(farmerRepository, times(1)).save(updatedFarmer);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_more_than_in_herd_rejected() throws Exception {
        Farmer origFarmer = getTestFarmer();
        origFarmer.setNumOfCows(10);
        Herd herd11 = testHerd(11L, 3, 100, origFarmer);
        Herd herd12 = testHerd(12L, 7, 100, origFarmer);

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(List.of(herd11, herd12));

        // herd 11 is valid, but herd 12 only has 7 cows, so nothing should change
        MvcResult response = performSell("1", "{\"11\": 1, \"12\": 8}", 400);

        assertErrorResponse(response, "You do not have enough cows in that herd to sell!", "NoCowsException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_negative_count_rejected() throws Exception {
        Farmer origFarmer = getTestFarmer();
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L)))
                .thenReturn(List.of(testHerd(11L, 3, 100, origFarmer)));

        MvcResult response = performSell("1", "{\"11\": -1}", 400);

        assertErrorResponse(response, "You cannot sell a negative number of cows!", "InvalidNumberOfCowsException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_missing_count_rejected() throws Exception {
        Farmer origFarmer = getTestFarmer();
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L)))
                .thenReturn(List.of(testHerd(11L, 3, 100, origFarmer)));

        MvcResult response = performSell("1", "{\"11\": null}", 400);

        assertErrorResponse(response, "You cannot sell a negative number of cows!", "InvalidNumberOfCowsException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_zero_total_rejected() throws Exception {
        Farmer origFarmer = getTestFarmer();
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L)))
                .thenReturn(List.of(testHerd(11L, 3, 100, origFarmer)));

        MvcResult response = performSell("1", "{\"11\": 0}", 400);

        assertErrorResponse(response, "You must sell at least 1 cow!", "InvalidNumberOfCowsException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_empty_request_rejected() throws Exception {
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(getTestFarmer()));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(List.of());

        MvcResult response = performSell("1", "{}", 400);

        assertErrorResponse(response, "You must sell at least 1 cow!", "InvalidNumberOfCowsException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_herd_not_owned_by_user_rejected() throws Exception {
        Farmer origFarmer = getTestFarmer();
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        // herd 99 belongs to someone else, so it is not among the current user's herds
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L)))
                .thenReturn(List.of(testHerd(11L, 3, 100, origFarmer)));

        MvcResult response = performSell("1", "{\"99\": 1}", 404);

        assertErrorResponse(response, "Herd with id 99 not found", "EntityNotFoundException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_for_user_not_in_game() throws Exception {
        when(gameRepository.findById(234L)).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.empty());

        MvcResult response = performSell("234", "{\"11\": 1}", 404);

        assertErrorResponse(response, "Farmer with gameId 234 and userId 1 not found", "EntityNotFoundException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_game_does_not_exist() throws Exception {
        when(gameRepository.findById(234L)).thenReturn(Optional.empty());

        MvcResult response = performSell("234", "{\"11\": 1}", 404);

        assertErrorResponse(response, "Game with id 234 not found", "EntityNotFoundException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_hidden_game() throws Exception {
        testGame.setHidden(true);
        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(getTestFarmer()));

        MvcResult response = performSell("1", "{\"11\": 1}", 400);

        assertErrorResponse(response, "Game with id 1 is hidden", "GameHiddenException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_course_linked_game_requires_current_enrollment() throws Exception {
        testGame.setCourseId(17L);
        User currentUser = currentUserService.getUser();

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(testGame))).thenReturn(false);

        MvcResult response = performSell("1", "{\"11\": 1}", 403);

        verify(courseAccessService, times(1)).isEligibleForGame(eq(currentUser), eq(testGame));
        verify(farmerRepository, times(0)).findByGameIdAndUserId(anyLong(), anyLong());
        assertErrorResponse(response, "Not enrolled in course associated with game",
                "NotEnrolledInCourseAssociatedWithGameException");
        verifyNothingSaved();
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void test_sellCows_course_linked_game_allows_current_enrollment() throws Exception {
        testGame.setCourseId(17L);
        User currentUser = currentUserService.getUser();

        Farmer origFarmer = getTestFarmer();
        origFarmer.setNumOfCows(2);
        Herd herd11 = testHerd(11L, 2, 50, origFarmer);

        when(gameRepository.findById(eq(1L))).thenReturn(Optional.of(testGame));
        when(courseAccessService.isEligibleForGame(eq(currentUser), eq(testGame))).thenReturn(true);
        when(farmerRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(origFarmer));
        when(herdRepository.findByGameIdAndUserId(eq(1L), eq(1L))).thenReturn(List.of(herd11));

        Farmer updatedFarmer = getTestFarmer();
        updatedFarmer.setTotalWealth(300 + 2 * 5);
        updatedFarmer.setNumOfCows(0);
        updatedFarmer.setCowsSold(2);

        MvcResult response = performSell("1", "{\"11\": 2}", 200);

        verify(courseAccessService, times(1)).isEligibleForGame(eq(currentUser), eq(testGame));
        verify(herdRepository, times(1)).delete(herd11);
        verify(farmerRepository, times(1)).save(updatedFarmer);
        assertEquals(mapper.writeValueAsString(updatedFarmer), response.getResponse().getContentAsString());
    }
}
