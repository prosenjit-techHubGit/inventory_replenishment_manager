package com.example.procurement.repository;

import com.example.procurement.domain.replenishment.ReplenishmentClaim;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReplenishmentClaimRepository extends JpaRepository<ReplenishmentClaim, UUID> {

    /**
     * Atomic claim acquisition/reclaim (plan section 39.3): inserts a new claim row, or steals an
     * existing one only if it was released or has expired. Returns 1 if the caller now holds the
     * claim, 0 if another active, non-expired claim already exists.
     */
    @Modifying
    @Query(value = "INSERT INTO replenishment_claim (item_id, run_id, status, claimed_at, expires_at) "
            + "VALUES (:itemId, :runId, 'ACTIVE', :claimedAt, :expiresAt) "
            + "ON CONFLICT (item_id) DO UPDATE SET run_id = :runId, status = 'ACTIVE', claimed_at = :claimedAt, expires_at = :expiresAt "
            + "WHERE replenishment_claim.status = 'RELEASED' OR replenishment_claim.expires_at < :claimedAt",
            nativeQuery = true)
    int tryAcquireClaim(
            @Param("itemId") UUID itemId,
            @Param("runId") String runId,
            @Param("claimedAt") OffsetDateTime claimedAt,
            @Param("expiresAt") OffsetDateTime expiresAt);

    @Modifying
    @Query(value = "UPDATE replenishment_claim SET status = 'RELEASED' WHERE item_id = :itemId AND run_id = :runId", nativeQuery = true)
    int releaseClaim(@Param("itemId") UUID itemId, @Param("runId") String runId);
}
