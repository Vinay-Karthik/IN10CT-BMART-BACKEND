package com.bmart.repository;

import com.bmart.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    Optional<JwtToken> findByUserId(Long userId);

    Optional<JwtToken> findByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM JwtToken t WHERE t.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
