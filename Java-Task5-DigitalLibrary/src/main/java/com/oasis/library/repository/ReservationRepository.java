package com.oasis.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oasis.library.model.Reservation;
import com.oasis.library.model.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Next member in the queue for a book (first come, first served). */
    Optional<Reservation> findFirstByBook_IdAndStatusOrderByIdAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByUser_IdAndBook_IdAndStatus(Long userId, Long bookId, ReservationStatus status);

    boolean existsByUser_IdAndBook_IdAndStatus(Long userId, Long bookId, ReservationStatus status);

    List<Reservation> findByUser_IdOrderByReservedAtDesc(Long userId);

    long countByBook_IdAndStatus(Long bookId, ReservationStatus status);

    long countByStatus(ReservationStatus status);

    /**
     * How many people are ahead in the queue. The queue is ordered by id (ids only grow),
     * which avoids timestamp-precision problems when two reservations happen close together.
     */
    long countByBook_IdAndStatusAndIdLessThan(Long bookId, ReservationStatus status, Long id);

    @Modifying
    @Query("delete from Reservation r where r.book.id = :bookId")
    void deleteByBookId(@Param("bookId") Long bookId);

    @Modifying
    @Query("delete from Reservation r where r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
