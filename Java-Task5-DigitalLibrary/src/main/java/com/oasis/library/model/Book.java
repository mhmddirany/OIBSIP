package com.oasis.library.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 60)
    private String category;

    /** Number of physical copies the library owns. */
    @Column(nullable = false)
    private int totalQuantity;

    /** Copies currently on the shelf (total minus issued). */
    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Book() {
    }

    public Book(String title, String author, String isbn, String category, int totalQuantity) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
        this.createdAt = LocalDateTime.now();
    }

    public void updateDetails(String title, String author, String isbn, String category) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
    }

    /** Changes the number of copies, keeping the issued copies out of the available count. */
    public void changeTotalQuantity(int newTotal, long issuedCopies) {
        this.totalQuantity = newTotal;
        this.availableQuantity = (int) (newTotal - issuedCopies);
    }

    public void takeCopy() {
        if (availableQuantity <= 0) {
            throw new IllegalStateException("No copies available");
        }
        availableQuantity--;
    }

    public void returnCopy() {
        if (availableQuantity < totalQuantity) {
            availableQuantity++;
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getCategory() { return category; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
