package com.jackpot.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jackpot.model.Jackpot;

@Repository
public interface JackpotRepository extends JpaRepository<Jackpot, Long> {
  boolean existsByJackpotId(String jackpotId);

  Optional<Jackpot> findByJackpotId(String jackpotId);

  @Lock(LockModeType.OPTIMISTIC)
  @Query("SELECT j FROM Jackpot j WHERE j.jackpotId = :jackpotId")
  Optional<Jackpot> findByJackpotIdWithLock(@Param("jackpotId") String jackpotId);
}