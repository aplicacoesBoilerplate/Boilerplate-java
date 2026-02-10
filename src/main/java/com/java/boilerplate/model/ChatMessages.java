package com.java.boilerplate.model;

import com.java.boilerplate.modelQueryJPA.ChatMessagesQueriesJPA;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMessages extends ChatMessagesQueriesJPA {
    @Id
    @Column(name = "id_message")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMessage;

    @ManyToOne
    @JoinColumn(name = "id_user_send")
    private Users sender;

    @ManyToOne
    @JoinColumn(name = "id_user_receiver")
    private Users receiver;

    @Column(name = "message_content")
    private String content;

    @CreationTimestamp
    @Column(name = "message_timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "message_is_read")
    private Boolean read;
}
