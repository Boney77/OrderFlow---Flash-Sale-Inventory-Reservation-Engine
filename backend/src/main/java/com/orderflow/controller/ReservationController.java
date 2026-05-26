package com.orderflow.controller;

import com.orderflow.dto.ReservationDTO;
import com.orderflow.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/reservation/{token}")
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable UUID token) {
        log.debug("Fetching reservation: {}", token);
        ReservationDTO reservation = reservationService.getReservation(token);
        return ResponseEntity.ok(reservation);
    }
}
