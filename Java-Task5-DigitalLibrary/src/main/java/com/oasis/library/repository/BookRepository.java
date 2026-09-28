package com.oasis.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oasis.library.model.Book;

import jakarta.persistence.LockModeType;

public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Search by title / author / ISBN and filter by category.
     * Empty strings mean "no filter" (avoids null-parameter quirks in JPQL).
     */
    @Query("""
            select b from Book b
            where (:category = '' or b.category = :category)
              and (:q = ''
                   or lower(b.title) like lower(concat('%', :q, '%'))
                   or lower(b.author) like lower(concat('%', :q, '%'))
                   or b.isbn = :q)
            order by b.title
            """)
    List<Book> search(@Param("q") String q, @Param("category") String category);

    @Query("select distinct b.category from Book b order by b.category")
    List<String> findCategories();

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /** Row lock so two members can't grab the last copy at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(b.totalQuantity), 0) from Book b")
    long sumTotalCopies();

    @Query("select coalesce(sum(b.availableQuantity), 0) from Book b")
    long sumAvailableCopies();
}
