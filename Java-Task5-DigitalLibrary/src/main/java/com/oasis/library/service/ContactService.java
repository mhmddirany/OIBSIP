package com.oasis.library.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oasis.library.dto.Requests;
import com.oasis.library.dto.Responses;
import com.oasis.library.model.ContactMessage;
import com.oasis.library.model.User;
import com.oasis.library.repository.ContactMessageRepository;
import com.oasis.library.repository.UserRepository;

@Service
public class ContactService {

    private final ContactMessageRepository messages;
    private final UserRepository users;
    private final DtoMapper mapper;

    public ContactService(ContactMessageRepository messages, UserRepository users, DtoMapper mapper) {
        this.messages = messages;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional
    public Responses.MessageInfo send(Long userId, Requests.Contact req) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        ContactMessage saved = messages.save(new ContactMessage(user, req.subject().trim(), req.message().trim()));
        return mapper.message(saved);
    }
}
