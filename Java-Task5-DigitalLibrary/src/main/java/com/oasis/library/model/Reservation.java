package com.oasis.library.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Advance booking of a book that is currently issued to someone else. */
@Entity
@Table(name = "reservations")
public class Reservation {

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
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReservationStatus status = ReservationStatus.WAITING;

    private LocalDateTime closedAt;

    protected Reservation() {
    }

    public Reservation(Book book, User user) {
        this.book = book;
        this.user = user;
        this.reservedAt = LocalDateTime.now();
    }

    public void fulfil() {
        this.status = ReservationStatus.FULFILLED;
        this.closedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.closedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public User getUser() { return user; }
    public LocalDateTime getReservedAt() { return reservedAt; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
