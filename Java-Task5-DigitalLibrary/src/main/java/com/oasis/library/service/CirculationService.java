package com.oasis.library.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oasis.library.config.LibraryProperties;
import com.oasis.library.dto.Responses;
import com.oasis.library.model.Book;
import com.oasis.library.model.Loan;
import com.oasis.library.model.Reservation;
import com.oasis.library.model.ReservationStatus;
import com.oasis.library.model.User;
import com.oasis.library.repository.BookRepository;
import com.oasis.library.repository.LoanRepository;
import com.oasis.library.repository.ReservationRepository;
import com.oasis.library.repository.UserRepository;

/**
 * Issuing, returning and reserving books — the rules that move copies between
 * the shelf and the members.
 */
@Service
@Transactional
public class CirculationService {

    private final BookRepository books;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final UserRepository users;
    private final LibraryProperties props;
    private final DtoMapper mapper;

    public CirculationService(BookRepository books, LoanRepository loans, ReservationRepository reservations,
                              UserRepository users, LibraryProperties props, DtoMapper mapper) {
        this.books = books;
        this.loans = loans;
        this.reservations = reservations;
        this.users = users;
        this.props = props;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ issue

    public Responses.LoanInfo issue(Long userId, Long bookId) {
        LocalDate today = LocalDate.now();
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (!user.isActive()) {
            throw ApiException.forbidden("Your account is deactivated");
        }
        Book book = books.findByIdForUpdate(bookId).orElseThrow(() -> ApiException.notFound("Book"));

        if (loans.existsByUser_IdAndBook_IdAndReturnDateIsNull(userId, bookId)) {
            throw ApiException.conflict("You already have a copy of this book");
        }
        if (loans.existsByUser_IdAndReturnDateIsNullAndDueDateBefore(userId, today)) {
            throw ApiException.conflict("Please return your overdue books before issuing new ones");
        }
        if (loans.existsByUser_IdAndFinePaidFalseAndFineGreaterThan(userId, BigDecimal.ZERO)) {
            throw ApiException.conflict("You have unpaid fines. Please pay them at the library desk first");
        }
        if (loans.countByUser_IdAndReturnDateIsNull(userId) >= props.maxActiveLoans()) {
            throw ApiException.conflict("You can hold at most " + props.maxActiveLoans() + " books at a time");
        }
        if (book.getAvailableQuantity() <= 0) {
            throw ApiException.conflict("No copies available right now. You can reserve this book instead");
        }

        book.takeCopy();
        // If this member had a reservation for the book, it is now fulfilled.
        reservations.findFirstByUser_IdAndBook_IdAndStatus(userId, bookId, ReservationStatus.WAITING)
                .ifPresent(Reservation::fulfil);
        Loan loan = loans.save(new Loan(book, user, today, today.plusDays(props.loanDays())));
        return mapper.loan(loan);
    }

    // ----------------------------------------------------------------- return

    /**
     * Returns a loan. Members may only return their own loans; admins may return any.
     * The fine (if overdue) is calculated and stored, then the copy goes to the next
     * member in the reservation queue — or back on the shelf.
     */
    public Responses.LoanInfo returnBook(Long loanId, Long requesterId, boolean requesterIsAdmin) {
        Loan loan = loans.findById(loanId).orElseThrow(() -> ApiException.notFound("Loan"));
        if (!requesterIsAdmin && !loan.getUser().getId().equals(requesterId)) {
            throw ApiException.notFound("Loan");
        }
        if (!loan.isActive()) {
            throw ApiException.conflict("This book has already been returned");
        }
        Book book = books.findByIdForUpdate(loan.getBook().getId())
                .orElseThrow(() -> ApiException.notFound("Book"));

        loan.markReturned(LocalDate.now(), props.finePerDay());
        book.returnCopy();
        fulfilWaitingReservations(book);
        return mapper.loan(loan);
    }

    /**
     * While copies are on the shelf and people are waiting, issue copies to the
     * members at the front of the queue (advance booking).
     */
    public void fulfilWaitingReservations(Book book) {
        LocalDate today = LocalDate.now();
        while (book.getAvailableQuantity() > 0) {
            Optional<Reservation> next = reservations
                    .findFirstByBook_IdAndStatusOrderByIdAsc(book.getId(), ReservationStatus.WAITING);
            if (next.isEmpty()) {
                return;
            }
            Reservation reservation = next.get();
            User member = reservation.getUser();
            boolean alreadyHasCopy = loans.existsByUser_IdAndBook_IdAndReturnDateIsNull(member.getId(), book.getId());
            if (!member.isActive() || alreadyHasCopy) {
                reservation.cancel();
            } else {
                reservation.fulfil();
                book.takeCopy();
                loans.save(new Loan(book, member, today, today.plusDays(props.loanDays())));
            }
            reservations.saveAndFlush(reservation);
        }
    }

    // ------------------------------------------------------------ reservation

    public Responses.ReservationInfo reserve(Long userId, Long bookId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (!user.isActive()) {
            throw ApiException.forbidden("Your account is deactivated");
        }
        Book book = books.findByIdForUpdate(bookId).orElseThrow(() -> ApiException.notFound("Book"));
        if (book.getAvailableQuantity() > 0) {
            throw ApiException.conflict("This book is available now. You can issue it directly");
        }
        if (loans.existsByUser_IdAndBook_IdAndReturnDateIsNull(userId, bookId)) {
            throw ApiException.conflict("You already have a copy of this book");
        }
        if (reservations.existsByUser_IdAndBook_IdAndStatus(userId, bookId, ReservationStatus.WAITING)) {
            throw ApiException.conflict("You have already reserved this book");
        }
        Reservation saved = reservations.save(new Reservation(book, user));
        return mapper.reservation(saved);
    }

    public Responses.ReservationInfo cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservations.findById(reservationId)
                .filter(r -> r.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Reservation"));
        if (reservation.getStatus() != ReservationStatus.WAITING) {
            throw ApiException.conflict("Only waiting reservations can be cancelled");
        }
        reservation.cancel();
        return mapper.reservation(reservation);
    }

    // ------------------------------------------------------------ member views

    @Transactional(readOnly = true)
    public List<Responses.LoanInfo> loansOf(Long userId) {
        return loans.findByUser_IdOrderByIssueDateDescIdDesc(userId).stream().map(mapper::loan).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.ReservationInfo> reservationsOf(Long userId) {
        return reservations.findByUser_IdOrderByReservedAtDesc(userId).stream().map(mapper::reservation).toList();
    }

    @Transactional(readOnly = true)
    public Responses.MemberSummary summaryOf(Long userId) {
        LocalDate today = LocalDate.now();
        List<Loan> mine = loans.findByUser_IdOrderByIssueDateDescIdDesc(userId);
        long active = mine.stream().filter(Loan::isActive).count();
        long overdue = mine.stream().filter(l -> l.isActive() && l.getDueDate().isBefore(today)).count();
        BigDecimal accruing = mine.stream().filter(Loan::isActive)
                .map(l -> l.currentFine(props.finePerDay(), today))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaid = loans.sumUnpaidFinesForUser(userId);
        return new Responses.MemberSummary(active, overdue, unpaid, accruing,
                props.maxActiveLoans(), props.loanDays(), props.finePerDay());
    }
}
