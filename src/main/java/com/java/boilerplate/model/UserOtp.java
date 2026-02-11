package com.java.boilerplate.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users_otp")
@Data
public class UserOtp {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private Users user;

    private String otpCode;
    private LocalDateTime expiryDate;
}