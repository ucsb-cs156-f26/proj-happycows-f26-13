package edu.ucsb.cs156.happiercows.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.ucsb.cs156.happiercows.entities.BannedWord;
import edu.ucsb.cs156.happiercows.repositories.BannedWordRepository;

@ExtendWith(SpringExtension.class)
@Import(ProfanityFilterService.class)
@ContextConfiguration
public class ProfanityFilterServiceTests {

    @MockBean
    BannedWordRepository bannedWordRepository;

    @Autowired
    ProfanityFilterService profanityFilterService;

    private void banned(String... words) {
        List<BannedWord> bannedWords = List.of(words).stream()
            .map(word -> BannedWord.builder().word(word).build())
            .toList();
        when(bannedWordRepository.findAll()).thenReturn(bannedWords);
    }

    @Test
    public void censorReturnsNullForNullMessage() {
        assertEquals(null, profanityFilterService.censor(null));
    }

    @Test
    public void censorReturnsEmptyStringForEmptyMessage() {
        assertEquals("", profanityFilterService.censor(""));
    }

    @Test
    public void censorReturnsMessageUnchangedWhenThereAreNoBannedWords() {
        when(bannedWordRepository.findAll()).thenReturn(List.of());
        assertEquals("Hello world!", profanityFilterService.censor("Hello world!"));
    }

    @Test
    public void censorReturnsMessageUnchangedWhenNoBannedWordAppears() {
        banned("heck");
        assertEquals("Hello world!", profanityFilterService.censor("Hello world!"));
    }

    @Test
    public void censorReplacesBannedWordCaseInsensitively() {
        banned("heck");
        assertEquals("what the ****", profanityFilterService.censor("what the heck"));
        assertEquals("what the ****!", profanityFilterService.censor("what the HECK!"));
        assertEquals("What the ****", profanityFilterService.censor("What the HeCk"));
    }

    @Test
    public void censorReplacesEveryOccurrenceOfABannedWord() {
        banned("heck");
        assertEquals("**** and more ****", profanityFilterService.censor("heck and more heck"));
    }

    @Test
    public void censorReplacesEveryBannedWordInTheDatabase() {
        banned("heck", "darn");
        assertEquals("**** you ****", profanityFilterService.censor("heck you darn"));
    }

    @Test
    public void censorKeepsWordsAroundBannedWordIntact() {
        banned("heck");
        assertEquals("go **** to school", profanityFilterService.censor("go heck to school"));
    }

    @Test
    public void censorDoesNotMatchBannedWordInsideAnotherWord() {
        banned("ass");
        assertEquals("class is in session", profanityFilterService.censor("class is in session"));
    }

    @Test
    public void censorPreservesSpacesInsideBannedPhrase() {
        banned("darn it");
        assertEquals("oh **** ** now", profanityFilterService.censor("oh darn it now"));
    }

    @Test
    public void censorMasksLongestBannedEntryFirst() {
        banned("darn", "darn it");
        assertEquals("oh **** **", profanityFilterService.censor("oh darn it"));
    }

    @Test
    public void censorPreservesLengthOfCensoredMessage() {
        banned("heck");
        String message = "a heck b";
        assertEquals(message.length(), profanityFilterService.censor(message).length());
    }
}
