package com.java.boilerplate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users_otp")
@Data
public class UserOtp {
    @Id
    @Column(name = "id_user")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @MapsId
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user")
    private Users user;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "used")
    private Boolean used;
}