package com.jackpot.repository;

import com.jackpot.model.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    Optional<Contribution> findByBetId(String betId);
    List<Contribution> findByUserId(String userId);
    List<Contribution> findByJackpotId(String jackpotId);
    boolean existsByBetId(String betId);
}