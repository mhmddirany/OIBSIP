package com.oasis.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One copy of a book issued to one member. */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    /** Null while the book is still with the member. */
    private LocalDate returnDate;

    /** Final fine, fixed at the moment the book is returned. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fine = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean finePaid = false;

    protected Loan() {
    }

    public Loan(Book book, User user, LocalDate issueDate, LocalDate dueDate) {
        this.book = book;
        this.user = user;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    public boolean isActive() {
        return returnDate == null;
    }

    /** Days past the due date, up to the return date (or today if not returned yet). */
    public long overdueDays(LocalDate today) {
        LocalDate end = returnDate != null ? returnDate : today;
        long days = ChronoUnit.DAYS.between(dueDate, end);
        return Math.max(0, days);
    }

    /** Stored fine for returned books; running (accrued) fine for books still out. */
    public BigDecimal currentFine(BigDecimal finePerDay, LocalDate today) {
        if (!isActive()) {
            return fine;
        }
        return finePerDay.multiply(BigDecimal.valueOf(overdueDays(today)));
    }

    public void markReturned(LocalDate date, BigDecimal finePerDay) {
        this.returnDate = date;
        this.fine = finePerDay.multiply(BigDecimal.valueOf(overdueDays(date)));
        this.finePaid = this.fine.signum() == 0;
    }

    public void markFinePaid() {
        this.finePaid = true;
    }

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public User getUser() { return user; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public BigDecimal getFine() { return fine; }
    public boolean isFinePaid() { return finePaid; }
}
