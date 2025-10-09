package com.jackpot.repository;

import com.jackpot.model.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    Optional<Reward> findByBetId(String betId);
    List<Reward> findByUserId(Long userId);
    List<Reward> findByJackpotId(String jackpotId);
    boolean existsByBetId(String betId);
    boolean existsByJackpotId(String jackpotId);
}