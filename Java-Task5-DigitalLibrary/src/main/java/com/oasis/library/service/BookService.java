package com.oasis.library.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oasis.library.dto.Requests;
import com.oasis.library.dto.Responses;
import com.oasis.library.model.Book;
import com.oasis.library.repository.BookRepository;
import com.oasis.library.repository.LoanRepository;
import com.oasis.library.repository.ReservationRepository;

/** Catalogue: browsing and searching (members) plus add / edit / delete (admin). */
@Service
@Transactional
public class BookService {

    private final BookRepository books;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final CirculationService circulation;
    private final DtoMapper mapper;

    public BookService(BookRepository books, LoanRepository loans, ReservationRepository reservations,
                       CirculationService circulation, DtoMapper mapper) {
        this.books = books;
        this.loans = loans;
        this.reservations = reservations;
        this.circulation = circulation;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Responses.BookInfo> search(String q, String category) {
        String query = q == null ? "" : q.trim();
        String cat = category == null ? "" : category.trim();
        return books.search(query, cat).stream().map(mapper::book).toList();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return books.findCategories();
    }

    @Transactional(readOnly = true)
    public Responses.BookInfo get(Long id) {
        return books.findById(id).map(mapper::book).orElseThrow(() -> ApiException.notFound("Book"));
    }

    public Responses.BookInfo create(Requests.BookForm form) {
        String isbn = normaliseIsbn(form.isbn());
        if (books.existsByIsbn(isbn)) {
            throw ApiException.conflict("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book(form.title().trim(), form.author().trim(), isbn,
                form.category().trim(), form.totalQuantity());
        return mapper.book(books.save(book));
    }

    public Responses.BookInfo update(Long id, Requests.BookForm form) {
        Book book = books.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Book"));
        String isbn = normaliseIsbn(form.isbn());
        if (books.existsByIsbnAndIdNot(isbn, id)) {
            throw ApiException.conflict("Another book already uses ISBN " + isbn);
        }
        long issued = loans.countByBook_IdAndReturnDateIsNull(id);
        if (form.totalQuantity() < issued) {
            throw ApiException.conflict("Quantity cannot be less than the " + issued + " copies currently issued");
        }
        book.updateDetails(form.title().trim(), form.author().trim(), isbn, form.category().trim());
        book.changeTotalQuantity(form.totalQuantity(), issued);
        // New copies may let waiting members get the book right away.
        circulation.fulfilWaitingReservations(book);
        return mapper.book(book);
    }

    public void delete(Long id) {
        Book book = books.findById(id).orElseThrow(() -> ApiException.notFound("Book"));
        if (loans.countByBook_IdAndReturnDateIsNull(id) > 0) {
            throw ApiException.conflict("This book has copies issued. It can be deleted after they are returned");
        }
        if (loans.existsByBook_IdAndFinePaidFalseAndFineGreaterThan(id, BigDecimal.ZERO)) {
            throw ApiException.conflict("This book has unpaid fines linked to it. Settle them before deleting");
        }
        reservations.deleteByBookId(id);
        loans.deleteByBookId(id);
        books.delete(book);
    }

    private static String normaliseIsbn(String isbn) {
        return isbn.trim().toUpperCase(Locale.ROOT);
    }
}
