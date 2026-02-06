package com.java.boilerplate.model;

import com.java.boilerplate.modelQueryJPA.LogErrorsQueriesJPA;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "log_errors")
public class LogErrors extends LogErrorsQueriesJPA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_error")
    private Long idError;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "error_file", length = 150)
    private String errorFile;

    @Column(name = "error_class", length = 150)
    private String errorClass;

    @Column(name = "error_method", length = 150)
    private String errorMethod;

    @Column(name = "error_line")
    private Integer errorLine;

    @CreationTimestamp
    @Column(name = "error_date_time", nullable = false, updatable = false)
    private LocalDateTime errorDateTime;

    @Column(name = "error_status_code", nullable = false)
    private Integer errorStatusCode;
}
