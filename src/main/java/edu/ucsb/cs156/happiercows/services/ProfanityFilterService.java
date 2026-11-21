package edu.ucsb.cs156.happiercows.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.happiercows.entities.BannedWord;
import edu.ucsb.cs156.happiercows.repositories.BannedWordRepository;

@Service("ProfanityFilterService")
public class ProfanityFilterService {

    @Autowired
    BannedWordRepository bannedWordRepository;

    public String censor(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        List<String> bannedWords = loadBannedWords();
        if (bannedWords.isEmpty()) {
            return message;
        }

        String censored = message;
        for (String bannedWord : bannedWords) {
            censored = mask(censored, bannedWord);
        }

        return censored;
    }

    // Banned words are stored lowercase (see BannedWord) and matching is case
    // insensitive. Longer entries are applied first so that a banned phrase
    // such as "darn it" is masked before a shorter banned word inside it.
    private List<String> loadBannedWords() {
        List<String> bannedWords = new ArrayList<>();
        for (BannedWord bannedWord : bannedWordRepository.findAll()) {
            bannedWords.add(bannedWord.getWord());
        }
        bannedWords.sort(Comparator.comparingInt(String::length).reversed());
        return bannedWords;
    }

    // Only the characters belonging to a banned word are replaced with '*'.
    // Surrounding words and punctuation are untouched, and any whitespace
    // inside a banned phrase keeps its original position.
    private String mask(String message, String bannedWord) {
        Pattern pattern = Pattern.compile(
                "(?<!\\w)" + Pattern.quote(bannedWord) + "(?!\\w)",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(message);
        StringBuilder censored = new StringBuilder(message);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                char c = message.charAt(i);
                censored.setCharAt(i, Character.isWhitespace(c) ? c : '*');
            }
        }

        return censored.toString();
    }
}
