package com.example.procurement.repository;

import com.example.procurement.domain.idempotency.IdempotencyRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequest, UUID> {

    Optional<IdempotencyRequest> findByIdempotencyKey(UUID idempotencyKey);
}
