package com.java.boilerplate.model;

import com.java.boilerplate.modelQueryJPA.ChatContactsQueriesJPA;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "chat_contacts")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatContacts extends ChatContactsQueriesJPA {
    @Id
    @Column(name = "id_chat_contact")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idChatContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Users contact;

    @Column(name = "contact_blocked", nullable = false)
    private Boolean contactBlocked = false;
}
