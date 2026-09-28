package com.oasis.library.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oasis.library.model.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    List<ContactMessage> findAllByOrderByResolvedAscCreatedAtDesc();

    long countByResolvedFalse();

    /** Keep the message (name/email are copied on it) but unlink the deleted user. */
    @Modifying
    @Query("update ContactMessage m set m.user = null where m.user.id = :userId")
    void detachUser(@Param("userId") Long userId);
}
