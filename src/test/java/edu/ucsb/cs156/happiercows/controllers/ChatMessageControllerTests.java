package edu.ucsb.cs156.happiercows.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;

import edu.ucsb.cs156.happiercows.repositories.BannedWordRepository;
import edu.ucsb.cs156.happiercows.repositories.UserRepository;
import edu.ucsb.cs156.happiercows.testconfig.TestConfig;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import edu.ucsb.cs156.happiercows.entities.BannedWord;
import edu.ucsb.cs156.happiercows.entities.User;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import edu.ucsb.cs156.happiercows.ControllerTestCase;
import edu.ucsb.cs156.happiercows.repositories.ChatMessageRepository;
import edu.ucsb.cs156.happiercows.entities.ChatMessage;

import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.entities.Farmer;
import edu.ucsb.cs156.happiercows.entities.Game;
import edu.ucsb.cs156.happiercows.services.ProfanityFilterService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(controllers = ChatMessageController.class)
public class ChatMessageControllerTests extends ControllerTestCase {
    
    @MockBean
    ChatMessageRepository chatMessageRepository;

    @MockBean
    FarmerRepository farmerRepository;

    @MockBean
    UserRepository userRepository;

    @MockBean
    BannedWordRepository bannedWordRepository;
    ProfanityFilterService profanityFilterService;

    @Autowired
    ObjectMapper mapper;

    // Existing tests use content with no banned words, so the filter is a
    // pass-through unless a test stubs it explicitly.
    @BeforeEach
    public void setUp() {
        when(profanityFilterService.censor(anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }


    //* */ get tests
    @WithMockUser(roles = {"USER"})
    @Test
    public void userInGameCanGetChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        int page = 0;
        int size = 10;

        ChatMessage chatMessage1 = ChatMessage.builder().id(1L).gameId(gameId).userId(userId).build();
        ChatMessage chatMessage2 = ChatMessage.builder().id(2L).gameId(gameId).userId(userId).build();

        Page<ChatMessage> pageOfChatMessages = new PageImpl<ChatMessage>(Arrays.asList(chatMessage1, chatMessage2));

        when(chatMessageRepository.findByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()))).thenReturn(pageOfChatMessages);
        
        Farmer farmer = Farmer.builder()
                .game(Game.builder().showChat(true).build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));


        // act
        MvcResult response = mockMvc.perform(get("/api/chat/get?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()));
        String responseString = response.getResponse().getContentAsString();
        String expectedResponseString = mapper.writeValueAsString(pageOfChatMessages);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCanGetChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        int page = 0;
        int size = 10;

        ChatMessage chatMessage1 = ChatMessage.builder().id(1L).gameId(gameId).userId(userId).build();
        ChatMessage chatMessage2 = ChatMessage.builder().id(2L).gameId(gameId).userId(userId).build();

        Page<ChatMessage> pageOfChatMessages = new PageImpl<ChatMessage>(Arrays.asList(chatMessage1, chatMessage2));

        when(chatMessageRepository.findByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()))).thenReturn(pageOfChatMessages);
        
        // act
        MvcResult response = mockMvc.perform(get("/api/chat/get?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()));
        String responseString = response.getResponse().getContentAsString();
        String expectedResponseString = mapper.writeValueAsString(pageOfChatMessages);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userNotInGameCannotGetChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        int page = 0;
        int size = 10;

        ChatMessage chatMessage1 = ChatMessage.builder().id(1L).gameId(gameId).userId(userId).build();
        ChatMessage chatMessage2 = ChatMessage.builder().id(2L).gameId(gameId).userId(userId).build();

        Page<ChatMessage> pageOfChatMessages = new PageImpl<ChatMessage>(Arrays.asList(chatMessage1, chatMessage2));

        when(chatMessageRepository.findByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()))).thenReturn(pageOfChatMessages);
        
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.empty());

        // act
        mockMvc.perform(get("/api/chat/get?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isForbidden()).andReturn();
        
        // assert
        verify(chatMessageRepository, times(0)).findByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()));

    }
    
    //* */ admin/get tests
    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCanGetAllChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        int page = 0;
        int size = 10;

        ChatMessage chatMessage1 = ChatMessage.builder().id(1L).gameId(gameId).build();
        ChatMessage chatMessage2 = ChatMessage.builder().id(2L).gameId(gameId).build();

        Page<ChatMessage> pageOfChatMessages = new PageImpl<ChatMessage>(Arrays.asList(chatMessage1, chatMessage2));

        when(chatMessageRepository.findAllByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()))).thenReturn(pageOfChatMessages);

        // act
        MvcResult response = mockMvc.perform(get("/api/chat/admin/get?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findAllByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()));
        String responseString = response.getResponse().getContentAsString();
        String expectedResponseString = mapper.writeValueAsString(pageOfChatMessages);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userCannotUseAdminGetAPIEndpoint() throws Exception {
        
        // arrange
        Long gameId = 1L;
        int page = 0;
        int size = 10;

        ChatMessage chatMessage1 = ChatMessage.builder().id(1L).gameId(gameId).build();
        ChatMessage chatMessage2 = ChatMessage.builder().id(2L).gameId(gameId).build();

        Page<ChatMessage> pageOfChatMessages = new PageImpl<ChatMessage>(Arrays.asList(chatMessage1, chatMessage2));

        when(chatMessageRepository.findAllByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()))).thenReturn(pageOfChatMessages);

        // act
        mockMvc.perform(get("/api/chat/admin/get?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isForbidden()).andReturn();

        // assert
        verify(chatMessageRepository, times(0)).findAllByGameId(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()));
    }

    //* */ admin/hidden tests
    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCanGetHiddenChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        int page = 0;
        int size = 10;

        ChatMessage chatMessage1 = ChatMessage.builder().id(1L).gameId(gameId).hidden(true).build();
        ChatMessage chatMessage2 = ChatMessage.builder().id(2L).gameId(gameId).hidden(true).build();

        Page<ChatMessage> pageOfChatMessages = new PageImpl<ChatMessage>(Arrays.asList(chatMessage1, chatMessage2));

        when(chatMessageRepository.findByGameIdAndHidden(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()))).thenReturn(pageOfChatMessages);

        // act
        MvcResult response = mockMvc.perform(get("/api/chat/admin/hidden?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findByGameIdAndHidden(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()));
        String responseString = response.getResponse().getContentAsString();
        String expectedResponseString = mapper.writeValueAsString(pageOfChatMessages);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userCannotGetHiddenChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        int page = 0;
        int size = 10;

        ChatMessage chatMessage1 = ChatMessage.builder().id(1L).gameId(gameId).hidden(true).build();
        ChatMessage chatMessage2 = ChatMessage.builder().id(2L).gameId(gameId).hidden(true).build();

        Page<ChatMessage> pageOfChatMessages = new PageImpl<ChatMessage>(Arrays.asList(chatMessage1, chatMessage2));

        when(chatMessageRepository.findByGameIdAndHidden(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()))).thenReturn(pageOfChatMessages);

        // act
        mockMvc.perform(get("/api/chat/admin/hidden?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isForbidden()).andReturn();

        // assert
        verify(chatMessageRepository, times(0)).findByGameIdAndHidden(gameId, PageRequest.of(page, size, Sort.by("timestamp").descending()));
    }

    //* */ post tests
    @WithMockUser(roles = {"USER"})
    @Test
    public void userInGameCanPostChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        String content = "Hello world!";

        ChatMessage chatMessage = ChatMessage.builder().id(0L).gameId(gameId).userId(userId).message(content).build();

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);
        
        Farmer farmer = Farmer.builder()
                .game(Game.builder().showChat(true).build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        //act 
        MvcResult response = mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", gameId, content).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
        String responseString = response.getResponse().getContentAsString();
        String expectedResponseString = mapper.writeValueAsString(chatMessage);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userNotInGameCannotPostChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        String content = "Hello world!";

        ChatMessage chatMessage = ChatMessage.builder().id(0L).gameId(gameId).userId(userId).message(content).build();

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);
        
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.empty());

        //act 
        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", gameId, content).with(csrf()))
            .andExpect(status().isForbidden()).andReturn();

        // assert
        verify(chatMessageRepository, times(0)).save(any(ChatMessage.class));
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCanPostChatMessages() throws Exception {
        
        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        String content = "Hello world!";

        ChatMessage chatMessage = ChatMessage.builder().id(0L).gameId(gameId).userId(userId).message(content).build();

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);
        
        //act 
        MvcResult response = mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", gameId, content).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
        String responseString = response.getResponse().getContentAsString();
        String expectedResponseString = mapper.writeValueAsString(chatMessage);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    //* */ censoring tests
    @WithMockUser(roles = {"USER"})
    @Test
    public void postedChatMessageContentIsSentThroughProfanityFilter() throws Exception {

        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        String content = "Hello world!";

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(ChatMessage.builder().build());

        Farmer farmer = Farmer.builder()
                .game(Game.builder().showChat(true).build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        // act
        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", gameId, content).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(profanityFilterService, times(1)).censor(content);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void postedChatMessageIsSavedWithCensoredContent() throws Exception {

        // arrange
        Long gameId = 1L;
        Long userId = 1L;
        String content = "What the heck";
        String censoredContent = "What the ****";

        when(profanityFilterService.censor(content)).thenReturn(censoredContent);
        when(chatMessageRepository.save(any(ChatMessage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Farmer farmer = Farmer.builder()
                .game(Game.builder().showChat(true).build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        // act
        MvcResult response = mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", gameId, content).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(1)).save(captor.capture());
        assertEquals(censoredContent, captor.getValue().getMessage());

        Map<String, Object> json = responseToJson(response);
        assertEquals(censoredContent, json.get("message"));
    }

    //* */ cooldown tests
    @WithMockUser(roles = {"USER"})
    @Test
    public void userOnCooldownCannotPostChatMessages() throws Exception {
        User u = User.builder().id(1L).chatCooldownUntil(Instant.now().plusSeconds(60)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        MvcResult response = mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "hi").with(csrf()))
            .andExpect(status().isForbidden()).andReturn();

        verify(chatMessageRepository, times(0)).save(any(ChatMessage.class));
        Map<String, Object> json = responseToJson(response);
        assertEquals("You are on a chat cooldown for 60 more seconds", json.get("message"));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userWithExpiredCooldownCanPostChatMessages() throws Exception {
        User u = User.builder().id(1L).chatCooldownUntil(Instant.now().minusSeconds(1)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        Farmer farmer = Farmer.builder().game(Game.builder().showChat(true).build()).build();
        when(farmerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(farmer));

        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "hi").with(csrf()))
            .andExpect(status().isOk());

        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userWithNullCooldownCanPostChatMessages() throws Exception {
        User u = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        Farmer farmer = Farmer.builder().game(Game.builder().showChat(true).build()).build();
        when(farmerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(farmer));

        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "hi").with(csrf()))
            .andExpect(status().isOk());

        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    //* */ banned word cooldown tests
    private void stubBannedWords(String... words) {
        when(bannedWordRepository.findAll()).thenReturn(
            Arrays.stream(words).map(w -> BannedWord.builder().word(w).build()).toList());
    }

    private void stubFarmerInGameWithChat() {
        Farmer farmer = Farmer.builder().game(Game.builder().showChat(true).build()).build();
        when(farmerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(farmer));
    }

    private Instant postAndCaptureCooldown(String content) throws Exception {
        Instant before = Instant.now();
        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, content).with(csrf()))
            .andExpect(status().isOk());
        Instant after = Instant.now();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(saved.capture());
        Instant cooldownUntil = saved.getValue().getChatCooldownUntil();
        assertFalse(cooldownUntil.isBefore(before.plus(ChatMessageController.BANNED_WORD_COOLDOWN)));
        assertFalse(cooldownUntil.isAfter(after.plus(ChatMessageController.BANNED_WORD_COOLDOWN)));
        return cooldownUntil;
    }

    @Test
    public void bannedWordCooldownIsFiveMinutes() {
        assertEquals(java.time.Duration.ofMinutes(5), ChatMessageController.BANNED_WORD_COOLDOWN);
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void messageWithBannedWordPutsUserOnCooldownAndIsStillPosted() throws Exception {
        User u = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        stubFarmerInGameWithChat();
        stubBannedWords("heck", "darn");

        postAndCaptureCooldown("well DARN it!");

        ArgumentCaptor<ChatMessage> message = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(1)).save(message.capture());
        assertEquals("well DARN it!", message.getValue().getMessage());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void messageWithBannedWordReturnsBannedWordAndCooldownSeconds() throws Exception {
        User u = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        stubFarmerInGameWithChat();
        stubBannedWords("heck", "darn");

        MvcResult response = mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "well DARN it!").with(csrf()))
            .andExpect(status().isOk()).andReturn();

        Map<String, Object> json = responseToJson(response);
        assertEquals("darn", json.get("bannedWord"));
        assertEquals(300, json.get("cooldownSeconds"));
        // The chat message fields are still at the top level of the response
        assertEquals("well DARN it!", json.get("message"));
        assertEquals(1, json.get("gameId"));
        assertEquals(1, json.get("userId"));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void messageWithoutBannedWordDoesNotReturnCooldownFields() throws Exception {
        User u = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        stubFarmerInGameWithChat();
        stubBannedWords("darn");

        MvcResult response = mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "hello").with(csrf()))
            .andExpect(status().isOk()).andReturn();

        Map<String, Object> json = responseToJson(response);
        assertFalse(json.containsKey("bannedWord"));
        assertFalse(json.containsKey("cooldownSeconds"));
        assertEquals("hello", json.get("message"));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void bannedWordCooldownReplacesExpiredCooldown() throws Exception {
        User u = User.builder().id(1L).chatCooldownUntil(Instant.now().minusSeconds(10)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        stubFarmerInGameWithChat();
        stubBannedWords("darn");

        postAndCaptureCooldown("darn");
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void bannedWordFallsBackToCurrentUserWhenNotInRepository() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        stubFarmerInGameWithChat();
        stubBannedWords("darn");

        postAndCaptureCooldown("darn");
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminSendingBannedWordIsAlsoPutOnCooldown() throws Exception {
        User u = User.builder().id(1L).admin(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        stubBannedWords("darn");

        postAndCaptureCooldown("darn");
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void messageWithoutBannedWordDoesNotSetCooldown() throws Exception {
        User u = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        stubFarmerInGameWithChat();
        stubBannedWords("darn");

        // "darned" contains the banned word only as part of a longer word
        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "a darned good cow").with(csrf()))
            .andExpect(status().isOk());

        verify(userRepository, never()).save(any(User.class));
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userOnLongerCooldownSendingBannedWordKeepsExistingCooldown() throws Exception {
        Instant existing = Instant.now().plusSeconds(3600);
        User u = User.builder().id(1L).chatCooldownUntil(existing).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        stubFarmerInGameWithChat();
        stubBannedWords("darn");

        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "darn").with(csrf()))
            .andExpect(status().isForbidden());

        verify(userRepository, never()).save(any(User.class));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        assertEquals(existing, u.getChatCooldownUntil());
    }

    @WithMockUser(roles = {"USER"})
    @Test
    public void userNotInGameSendingBannedWordIsNotPutOnCooldown() throws Exception {
        User u = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(farmerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        stubBannedWords("darn");

        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", 1L, "darn").with(csrf()))
            .andExpect(status().isForbidden());

        verify(userRepository, never()).save(any(User.class));
    }

    //* */ findBannedWord unit tests
    @Test
    public void findBannedWordMatchesWholeWordsCaseInsensitively() {
        ChatMessageController controller = new ChatMessageController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "bannedWordRepository", bannedWordRepository);
        stubBannedWords("darn", "dang it");

        assertEquals(Optional.of("darn"), controller.findBannedWord("darn"));
        assertEquals(Optional.of("darn"), controller.findBannedWord("Oh, DaRn!"));
        assertEquals(Optional.of("dang it"), controller.findBannedWord("well dang it."));
        assertEquals(Optional.empty(), controller.findBannedWord("darned"));
        assertEquals(Optional.empty(), controller.findBannedWord("undarn"));
        assertEquals(Optional.empty(), controller.findBannedWord("darn2"));
        assertEquals(Optional.empty(), controller.findBannedWord("hello"));
    }

    @Test
    public void findBannedWordIsEmptyWhenNoBannedWords() {
        ChatMessageController controller = new ChatMessageController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "bannedWordRepository", bannedWordRepository);
        when(bannedWordRepository.findAll()).thenReturn(List.of());

        assertEquals(Optional.empty(), controller.findBannedWord("darn"));
    }

    @Test
    public void findBannedWordTreatsRegexCharactersLiterally() {
        ChatMessageController controller = new ChatMessageController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "bannedWordRepository", bannedWordRepository);
        stubBannedWords("a.b");

        assertEquals(Optional.of("a.b"), controller.findBannedWord("x a.b y"));
        assertEquals(Optional.empty(), controller.findBannedWord("x axb y"));
    }

    @Test
    public void findBannedWordReturnsTheWordThatMatched() {
        ChatMessageController controller = new ChatMessageController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "bannedWordRepository", bannedWordRepository);
        stubBannedWords("heck", "darn");

        assertEquals(Optional.of("darn"), controller.findBannedWord("oh darn"));
    }

    //* */ hide tests
    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCannotHideChatMessagesThatDontExist() throws Exception {
        
        // arrange
        Long messageId = 0L;

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

        //act 
        mockMvc.perform(put("/api/chat/hide?chatMessageId={messageId}", messageId).with(csrf()))
            .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findById(messageId);
        verify(chatMessageRepository, times(0)).save(any(ChatMessage.class));
    }

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCanHideChatMessages() throws Exception {
        
        // arrange
        Long messageId = 0L;
        Long gameId = 1L;
        Long userId = 1L;

        ChatMessage chatMessage = ChatMessage.builder().id(messageId).userId(1L).gameId(1L).build();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(chatMessage));

        Farmer farmer = Farmer.builder()
            .game(Game.builder().build())
            .build();
when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        //act 
        MvcResult response = mockMvc.perform(put("/api/chat/hide?chatMessageId={messageId}", messageId).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findById(messageId);
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
        String responseString = response.getResponse().getContentAsString();
        chatMessage.setHidden(true);
        String expectedResponseString = mapper.writeValueAsString(chatMessage);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    // Users can hide messages that are their own
    @WithMockUser(roles = {"USER"})
    @Test
    public void userCanDeleteTheirOwnChatMessages() throws Exception {
        
        // arrange
        Long messageId = 0L;
        Long gameId = 1L;
        Long userId = 1L;

        ChatMessage chatMessage = ChatMessage.builder().id(messageId).userId(1L).gameId(1L).build();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(chatMessage));

        Farmer farmer = Farmer.builder()
                .game(Game.builder().showChat(true).build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        //act 
        MvcResult response = mockMvc.perform(put("/api/chat/hide?chatMessageId={messageId}", messageId).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findById(messageId);
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
        String responseString = response.getResponse().getContentAsString();
        chatMessage.setHidden(true);
        String expectedResponseString = mapper.writeValueAsString(chatMessage);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    // Users cannot hide messages that aren't their own
    @WithMockUser(roles = {"USER"})
    @Test
    public void userCannotDeleteOtherUsersChatMessages() throws Exception {
        
        // arrange
        Long messageId = 0L;

        ChatMessage chatMessage = ChatMessage.builder().id(messageId).userId(2L).build();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(chatMessage));

        //act 
        mockMvc.perform(put("/api/chat/hide?chatMessageId={messageId}", messageId).with(csrf()))
            .andExpect(status().isForbidden()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findById(messageId);
        verify(chatMessageRepository, times(0)).save(any(ChatMessage.class));
    }

    // Users cannot hide messages that aren't their own
    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCanDeleteOtherUsersChatMessages() throws Exception {
        
        // arrange
        Long messageId = 0L;
        Long gameId = 1L;
        Long userId = 1L;

        ChatMessage chatMessage = ChatMessage.builder().id(messageId).userId(2L).gameId(1L).build();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(chatMessage));

        Farmer farmer = Farmer.builder()
                .game(Game.builder().build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        //act 
        MvcResult response = mockMvc.perform(put("/api/chat/hide?chatMessageId={messageId}", messageId).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findById(messageId);
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
        String responseString = response.getResponse().getContentAsString();
        chatMessage.setHidden(true);
        String expectedResponseString = mapper.writeValueAsString(chatMessage);
        log.info("Got back from API: {}",responseString);
        assertEquals(expectedResponseString, responseString);
    }

    
    @WithMockUser(roles = {"USER"})
    @Test
    public void userCannotInteractWithChatIfShowChatIsFalse() throws Exception {
        
        // arrange
        Long messageId = 0L;
        Long gameId = 1L;
        Long userId = 1L;
        int page = 0;
        int size = 10;
        String content = "Hello world!";

        ChatMessage chatMessage = ChatMessage.builder().id(messageId).userId(1L).gameId(1L).build();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(chatMessage));

        Farmer farmer = Farmer.builder()
                .game(Game.builder().showChat(false).build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        //act 
        mockMvc.perform(put("/api/chat/hide?chatMessageId={messageId}", messageId).with(csrf()))
            .andExpect(status().isForbidden()).andReturn();
        mockMvc.perform(get("/api/chat/get?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isForbidden()).andReturn();
        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", gameId, content).with(csrf()))
            .andExpect(status().isForbidden()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findById(messageId);
        verify(chatMessageRepository, times(0)).save(any(ChatMessage.class));
    }
    
    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void adminCanInteractWithChatIfShowChatIsFalse() throws Exception {
        
        // arrange
        Long messageId = 0L;
        Long gameId = 1L;
        Long userId = 1L;
        int page = 0;
        int size = 10;
        String content = "Hello world!";

        ChatMessage chatMessage = ChatMessage.builder().id(messageId).userId(1L).gameId(1L).build();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(chatMessage));

        Farmer farmer = Farmer.builder()
                .game(Game.builder().showChat(false).build())
                .build();
        when(farmerRepository.findByGameIdAndUserId(gameId, userId)).thenReturn(Optional.of(farmer));

        //act 
        mockMvc.perform(put("/api/chat/hide?chatMessageId={messageId}", messageId).with(csrf()))
            .andExpect(status().isOk()).andReturn();
        mockMvc.perform(get("/api/chat/get?gameId={gameId}&page={page}&size={size}", gameId, page, size))
            .andExpect(status().isOk()).andReturn();
        mockMvc.perform(post("/api/chat/post?gameId={gameId}&content={content}", gameId, content).with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(chatMessageRepository, atLeastOnce()).findById(messageId);
    }
}