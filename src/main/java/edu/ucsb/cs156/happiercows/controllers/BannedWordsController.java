package edu.ucsb.cs156.happiercows.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ucsb.cs156.happiercows.entities.BannedWord;
import edu.ucsb.cs156.happiercows.errors.EntityNotFoundException;
import edu.ucsb.cs156.happiercows.repositories.BannedWordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Banned words for chat (admin only)")
@RequestMapping("/api/admin/bannedwords")
@RestController
public class BannedWordsController extends ApiController {
    @Autowired
    BannedWordRepository bannedWordRepository;

    @Operation(summary = "Get all banned words")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<BannedWord> allBannedWords() {
        return bannedWordRepository.findAll();
    }

    @Operation(summary = "Add a banned word or phrase (stored lowercase)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Object addBannedWord(@Parameter(name = "word") @RequestParam String word) {
        // Collapse runs of whitespace so "darn   it" and "darn it" count as the same phrase
        String normalized = word.trim().replaceAll("\\s+", " ").toLowerCase();
        if (normalized.isEmpty()) {
            return ResponseEntity.badRequest().body(genericMessage("Banned word cannot be blank"));
        }
        if (bannedWordRepository.findByWord(normalized).isPresent()) {
            return ResponseEntity.badRequest().body(genericMessage("'%s' is already banned".formatted(normalized)));
        }
        return bannedWordRepository.save(BannedWord.builder().word(normalized).build());
    }

    @Operation(summary = "Delete a banned word by id")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public Object deleteBannedWord(@Parameter(name = "id") @RequestParam long id) {
        BannedWord bannedWord = bannedWordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(BannedWord.class, id));
        bannedWordRepository.delete(bannedWord);
        return genericMessage("BannedWord with id %d deleted".formatted(id));
    }
}
