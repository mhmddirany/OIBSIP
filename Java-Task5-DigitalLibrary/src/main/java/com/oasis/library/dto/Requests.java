package com.oasis.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** JSON request bodies, validated with Bean Validation before reaching the services. */
public final class Requests {

    private Requests() {
    }

    public record Register(
            @NotBlank(message = "Full name is required")
            @Size(max = 100, message = "Full name must be at most 100 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Email is not valid")
            @Size(max = 120, message = "Email must be at most 120 characters")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
            @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one number")
            String password) {
    }

    public record Login(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record BookForm(
            @NotBlank(message = "Title is required")
            @Size(max = 200, message = "Title must be at most 200 characters")
            String title,

            @NotBlank(message = "Author is required")
            @Size(max = 120, message = "Author must be at most 120 characters")
            String author,

            @NotBlank(message = "ISBN is required")
            @Pattern(regexp = "^[0-9Xx-]{10,17}$", message = "ISBN must be 10-17 characters: digits, hyphens or X")
            String isbn,

            @NotBlank(message = "Category is required")
            @Size(max = 60, message = "Category must be at most 60 characters")
            String category,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 1000, message = "Quantity must be at most 1000")
            Integer totalQuantity) {
    }

    public record BookRef(@NotNull(message = "bookId is required") Long bookId) {
    }

    public record Contact(
            @NotBlank(message = "Subject is required")
            @Size(max = 150, message = "Subject must be at most 150 characters")
            String subject,

            @NotBlank(message = "Message is required")
            @Size(max = 2000, message = "Message must be at most 2000 characters")
            String message) {
    }

    public record MemberStatus(@NotNull(message = "active is required") Boolean active) {
    }
}
