package com.coinguard.user.entity;

import com.coinguard.common.entity.BaseEntity;
import com.coinguard.common.enums.UserRole;
import com.coinguard.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends BaseEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    @EqualsAndHashCode.Include
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "is_active")
    private boolean isActive=true;

    @Column(name = "is_email_verified")
    private Boolean isEmailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role = UserRole.USER;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;
}
