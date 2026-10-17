package com.campushub.modules.user.repository;

import com.campushub.common.enums.UserStatus;
import com.campushub.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameWithLock(@Param("username") String username);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByCitizenId(String citizenId);
    Optional<User> findByCitizenId(String citizenId);

    // Admin dashboard queries
    long countByStatus(UserStatus status);
    long countByCreatedAtAfter(LocalDateTime date);
    @Query("SELECT COUNT(u) FROM User u WHERE u.identityVerified = true AND u.identityReviewedAt IS NOT NULL")
    long countByIdentityVerifiedTrue();

    @Query("SELECT u FROM User u WHERE " +
            "(CAST(:search AS string) IS NULL OR LOWER(u.username) LIKE CAST(:search AS string) OR LOWER(u.email) LIKE CAST(:search AS string) OR LOWER(u.fullName) LIKE CAST(:search AS string)) AND " +
            "(:status IS NULL OR u.status = :status)")
    Page<User> findAllWithFilters(
            @Param("search") String search,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
