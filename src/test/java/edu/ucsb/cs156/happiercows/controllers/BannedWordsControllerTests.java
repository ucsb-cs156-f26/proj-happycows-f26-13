package edu.ucsb.cs156.happiercows.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.happiercows.ControllerTestCase;
import edu.ucsb.cs156.happiercows.entities.BannedWord;
import edu.ucsb.cs156.happiercows.repositories.BannedWordRepository;
import edu.ucsb.cs156.happiercows.repositories.UserRepository;
import edu.ucsb.cs156.happiercows.testconfig.TestConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebMvcTest(controllers = BannedWordsController.class)
@Import(TestConfig.class)
public class BannedWordsControllerTests extends ControllerTestCase {

  @MockBean
  BannedWordRepository bannedWordRepository;

  // Needed by CurrentUserService, which ApiController depends on
  @MockBean
  UserRepository userRepository;

  // Authorization

  @Test
  public void all__logged_out() throws Exception {
    mockMvc.perform(get("/api/admin/bannedwords/all"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = { "USER" })
  @Test
  public void all__user_logged_in() throws Exception {
    mockMvc.perform(get("/api/admin/bannedwords/all"))
        .andExpect(status().is(403));
  }

  @Test
  public void post__logged_out() throws Exception {
    mockMvc.perform(post("/api/admin/bannedwords/post").param("word", "darn").with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = { "USER" })
  @Test
  public void post__user_logged_in() throws Exception {
    mockMvc.perform(post("/api/admin/bannedwords/post").param("word", "darn").with(csrf()))
        .andExpect(status().is(403));
    verify(bannedWordRepository, never()).save(any(BannedWord.class));
  }

  @Test
  public void delete__logged_out() throws Exception {
    mockMvc.perform(delete("/api/admin/bannedwords").param("id", "1").with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = { "USER" })
  @Test
  public void delete__user_logged_in() throws Exception {
    mockMvc.perform(delete("/api/admin/bannedwords").param("id", "1").with(csrf()))
        .andExpect(status().is(403));
    verify(bannedWordRepository, never()).delete(any(BannedWord.class));
  }

  // GET /all

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_get_all_banned_words() throws Exception {
    List<BannedWord> words = List.of(
        BannedWord.builder().id(1L).word("darn").build(),
        BannedWord.builder().id(2L).word("heck").build());
    when(bannedWordRepository.findAll()).thenReturn(words);

    MvcResult response = mockMvc.perform(get("/api/admin/bannedwords/all"))
        .andExpect(status().isOk()).andReturn();

    verify(bannedWordRepository, times(1)).findAll();
    assertEquals(mapper.writeValueAsString(words), response.getResponse().getContentAsString());
  }

  // POST /post

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_add_word_trimmed_and_lowercased() throws Exception {
    BannedWord expected = BannedWord.builder().word("darn").build();
    when(bannedWordRepository.findByWord("darn")).thenReturn(Optional.empty());
    when(bannedWordRepository.save(expected)).thenReturn(expected);

    MvcResult response = mockMvc.perform(post("/api/admin/bannedwords/post").param("word", "  DaRn ").with(csrf()))
        .andExpect(status().isOk()).andReturn();

    verify(bannedWordRepository, times(1)).findByWord("darn");
    verify(bannedWordRepository, times(1)).save(expected);
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_cannot_add_blank_word() throws Exception {
    MvcResult response = mockMvc.perform(post("/api/admin/bannedwords/post").param("word", "   ").with(csrf()))
        .andExpect(status().isBadRequest()).andReturn();

    verify(bannedWordRepository, never()).save(any(BannedWord.class));
    Map<String, Object> json = responseToJson(response);
    assertEquals("Banned word cannot be blank", json.get("message"));
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_add_phrase_with_whitespace_collapsed() throws Exception {
    BannedWord expected = BannedWord.builder().word("darn it").build();
    when(bannedWordRepository.findByWord("darn it")).thenReturn(Optional.empty());
    when(bannedWordRepository.save(expected)).thenReturn(expected);

    MvcResult response = mockMvc.perform(post("/api/admin/bannedwords/post").param("word", " Darn \t  IT ").with(csrf()))
        .andExpect(status().isOk()).andReturn();

    verify(bannedWordRepository, times(1)).findByWord("darn it");
    verify(bannedWordRepository, times(1)).save(expected);
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_cannot_add_duplicate_word() throws Exception {
    when(bannedWordRepository.findByWord("darn"))
        .thenReturn(Optional.of(BannedWord.builder().id(1L).word("darn").build()));

    MvcResult response = mockMvc.perform(post("/api/admin/bannedwords/post").param("word", "DARN").with(csrf()))
        .andExpect(status().isBadRequest()).andReturn();

    verify(bannedWordRepository, never()).save(any(BannedWord.class));
    Map<String, Object> json = responseToJson(response);
    assertEquals("'darn' is already banned", json.get("message"));
  }

  // DELETE

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_delete_word() throws Exception {
    BannedWord word = BannedWord.builder().id(7L).word("darn").build();
    when(bannedWordRepository.findById(7L)).thenReturn(Optional.of(word));

    MvcResult response = mockMvc.perform(delete("/api/admin/bannedwords").param("id", "7").with(csrf()))
        .andExpect(status().isOk()).andReturn();

    verify(bannedWordRepository, times(1)).delete(word);
    Map<String, Object> json = responseToJson(response);
    assertEquals("BannedWord with id 7 deleted", json.get("message"));
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_cannot_delete_missing_word() throws Exception {
    when(bannedWordRepository.findById(15L)).thenReturn(Optional.empty());

    MvcResult response = mockMvc.perform(delete("/api/admin/bannedwords").param("id", "15").with(csrf()))
        .andExpect(status().isNotFound()).andReturn();

    verify(bannedWordRepository, never()).delete(any(BannedWord.class));
    Map<String, Object> json = responseToJson(response);
    assertEquals("BannedWord with id 15 not found", json.get("message"));
  }
}
