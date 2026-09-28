package com.oasis.library.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oasis.library.model.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUser_IdOrderByIssueDateDescIdDesc(Long userId);

    List<Loan> findByReturnDateIsNullOrderByDueDateAsc();

    List<Loan> findByReturnDateIsNullAndDueDateBeforeOrderByDueDateAsc(LocalDate today);

    List<Loan> findAllByOrderByIssueDateDescIdDesc();

    /** Every loan that produced a fine, unpaid ones first. */
    @Query("select l from Loan l where l.fine > 0 order by l.finePaid asc, l.returnDate desc")
    List<Loan> findLoansWithFines();

    long countByUser_IdAndReturnDateIsNull(Long userId);

    long countByBook_IdAndReturnDateIsNull(Long bookId);

    long countByReturnDateIsNull();

    long countByReturnDateIsNullAndDueDateBefore(LocalDate today);

    boolean existsByUser_IdAndBook_IdAndReturnDateIsNull(Long userId, Long bookId);

    boolean existsByUser_IdAndReturnDateIsNullAndDueDateBefore(Long userId, LocalDate today);

    boolean existsByUser_IdAndFinePaidFalseAndFineGreaterThan(Long userId, BigDecimal amount);

    boolean existsByBook_IdAndFinePaidFalseAndFineGreaterThan(Long bookId, BigDecimal amount);

    @Query("select coalesce(sum(l.fine), 0) from Loan l where l.finePaid = false")
    BigDecimal sumUnpaidFines();

    @Query("select coalesce(sum(l.fine), 0) from Loan l where l.finePaid = false and l.user.id = :userId")
    BigDecimal sumUnpaidFinesForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Loan l where l.book.id = :bookId")
    void deleteByBookId(@Param("bookId") Long bookId);

    @Modifying
    @Query("delete from Loan l where l.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
