package com.oasis.library.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.library.dto.Responses;
import com.oasis.library.service.BookService;

/** Catalogue browsing for any logged-in user. */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** GET /api/books?q=java&category=Programming — both parameters optional. */
    @GetMapping
    public List<Responses.BookInfo> search(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String category) {
        return bookService.search(q, category);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return bookService.categories();
    }

    @GetMapping("/{id}")
    public Responses.BookInfo get(@PathVariable Long id) {
        return bookService.get(id);
    }
}
