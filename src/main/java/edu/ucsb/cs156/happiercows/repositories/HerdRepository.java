package edu.ucsb.cs156.happiercows.repositories;

import edu.ucsb.cs156.happiercows.entities.Herd;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HerdRepository extends CrudRepository<Herd, Long> {
    @Query("SELECT h FROM herds h WHERE h.farmer.game.id = :gameId AND h.farmer.user.id = :userId ORDER BY h.id")
    Iterable<Herd> findByGameIdAndUserId(Long gameId, Long userId);
}
