package com.orderflow.repository;

import com.orderflow.entity.Reservation;
import com.orderflow.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant time);

    Optional<Reservation> findByReservationToken(UUID reservationToken);
}
