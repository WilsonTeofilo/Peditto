package com.peditto.common.persistence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {}
