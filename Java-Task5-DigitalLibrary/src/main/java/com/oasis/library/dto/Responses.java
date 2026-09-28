package com.oasis.library.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** JSON response bodies. Entities are never returned directly (no password hashes leak). */
public final class Responses {

    private Responses() {
    }

    public record UserInfo(Long id, String fullName, String email, String role) {
    }

    public record Member(Long id, String fullName, String email, String role, boolean active,
                         LocalDateTime createdAt, long activeLoans, BigDecimal unpaidFines) {
    }

    public record BookInfo(Long id, String title, String author, String isbn, String category,
                           int totalQuantity, int availableQuantity, long waitingReservations) {
    }

    /** status is ACTIVE, OVERDUE or RETURNED. fine is the running fine for books still out. */
    public record LoanInfo(Long id, Long bookId, String bookTitle, String bookAuthor,
                           Long userId, String userName, String userEmail,
                           LocalDate issueDate, LocalDate dueDate, LocalDate returnDate,
                           String status, long overdueDays, BigDecimal fine, boolean finePaid) {
    }

    /** queuePosition is 1 for the next in line; 0 when the reservation is no longer waiting. */
    public record ReservationInfo(Long id, Long bookId, String bookTitle, String bookAuthor,
                                  Long userId, String userName, String userEmail,
                                  LocalDateTime reservedAt, String status, long queuePosition) {
    }

    public record MessageInfo(Long id, String name, String email, String subject, String message,
                              LocalDateTime createdAt, boolean resolved) {
    }

    public record Stats(long titles, long totalCopies, long availableCopies, long activeLoans,
                        long overdueLoans, long members, long waitingReservations,
                        BigDecimal unpaidFines, long openMessages) {
    }

    /** Summary shown on the member dashboard banner. */
    public record MemberSummary(long activeLoans, long overdueLoans, BigDecimal unpaidFines,
                                BigDecimal accruingFines, int maxActiveLoans, int loanDays,
                                BigDecimal finePerDay) {
    }

    public record Message(String message) {
    }
}
