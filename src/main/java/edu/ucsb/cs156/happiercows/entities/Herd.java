package edu.ucsb.cs156.happiercows.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "herds")
public class Herd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
        @JoinColumn(name = "game_id", referencedColumnName = "game_id")
    })
    @JsonIgnore
    private Farmer farmer;

    private int numCows;

    private double health;

    @JsonInclude
    public long getUserId() {
        return farmer.getUserId();
    }

    @JsonInclude
    public long getGameId() {
        return farmer.getGameId();
    }
}
