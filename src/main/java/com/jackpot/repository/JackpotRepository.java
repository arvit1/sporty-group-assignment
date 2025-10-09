package com.jackpot.repository;

import com.jackpot.model.Jackpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface JackpotRepository extends JpaRepository<Jackpot, Long> {
    Optional<Jackpot> findByJackpotId(String jackpotId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT j FROM Jackpot j WHERE j.jackpotId = :jackpotId")
    Optional<Jackpot> findByJackpotIdWithLock(@Param("jackpotId") String jackpotId);

    boolean existsByJackpotId(String jackpotId);
}