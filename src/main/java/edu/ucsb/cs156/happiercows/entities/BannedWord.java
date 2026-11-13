package edu.ucsb.cs156.happiercows.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "banned_words")
public class BannedWord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  // Stored lowercase; matching against chat messages is case-insensitive
  @Column(nullable = false, unique = true)
  private String word;
}
