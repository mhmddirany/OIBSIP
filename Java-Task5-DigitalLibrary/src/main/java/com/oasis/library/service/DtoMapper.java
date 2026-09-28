package com.oasis.library.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.oasis.library.config.LibraryProperties;
import com.oasis.library.dto.Responses;
import com.oasis.library.model.Book;
import com.oasis.library.model.ContactMessage;
import com.oasis.library.model.Loan;
import com.oasis.library.model.Reservation;
import com.oasis.library.model.ReservationStatus;
import com.oasis.library.model.User;
import com.oasis.library.repository.ReservationRepository;

/** Converts entities to response DTOs. */
@Component
public class DtoMapper {

    private final LibraryProperties props;
    private final ReservationRepository reservations;

    public DtoMapper(LibraryProperties props, ReservationRepository reservations) {
        this.props = props;
        this.reservations = reservations;
    }

    public Responses.UserInfo user(User u) {
        return new Responses.UserInfo(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
    }

    public Responses.BookInfo book(Book b) {
        long waiting = reservations.countByBook_IdAndStatus(b.getId(), ReservationStatus.WAITING);
        return new Responses.BookInfo(b.getId(), b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCategory(),
                b.getTotalQuantity(), b.getAvailableQuantity(), waiting);
    }

    public Responses.LoanInfo loan(Loan l) {
        LocalDate today = LocalDate.now();
        String status;
        if (!l.isActive()) {
            status = "RETURNED";
        } else if (l.getDueDate().isBefore(today)) {
            status = "OVERDUE";
        } else {
            status = "ACTIVE";
        }
        return new Responses.LoanInfo(l.getId(), l.getBook().getId(), l.getBook().getTitle(),
                l.getBook().getAuthor(), l.getUser().getId(), l.getUser().getFullName(), l.getUser().getEmail(),
                l.getIssueDate(), l.getDueDate(), l.getReturnDate(), status, l.overdueDays(today),
                l.currentFine(props.finePerDay(), today), l.isFinePaid());
    }

    public Responses.ReservationInfo reservation(Reservation r) {
        long position = 0;
        if (r.getStatus() == ReservationStatus.WAITING) {
            position = reservations.countByBook_IdAndStatusAndIdLessThan(
                    r.getBook().getId(), ReservationStatus.WAITING, r.getId()) + 1;
        }
        return new Responses.ReservationInfo(r.getId(), r.getBook().getId(), r.getBook().getTitle(),
                r.getBook().getAuthor(), r.getUser().getId(), r.getUser().getFullName(), r.getUser().getEmail(),
                r.getReservedAt(), r.getStatus().name(), position);
    }

    public Responses.MessageInfo message(ContactMessage m) {
        return new Responses.MessageInfo(m.getId(), m.getName(), m.getEmail(), m.getSubject(), m.getMessage(),
                m.getCreatedAt(), m.isResolved());
    }
}
