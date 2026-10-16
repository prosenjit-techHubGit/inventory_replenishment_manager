package com.example.procurement.repository;

import com.example.procurement.domain.outbox.OutboxStatus;
import com.example.procurement.domain.outbox.ProcessStartOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessStartOutboxRepository extends JpaRepository<ProcessStartOutbox, UUID> {

    Optional<ProcessStartOutbox> findByPoRequestId(String poRequestId);

    List<ProcessStartOutbox> findByStatusIn(List<OutboxStatus> statuses);
}
