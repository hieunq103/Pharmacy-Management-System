package com.pharmacy.account;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
public class LoginHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Result {
        SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "login_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime loginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private Result result;

    @Column(name = "client_info", length = 255)
    private String clientInfo;

    public LoginHistory() {
    }

    public LoginHistory(Account account, Result result, String clientInfo) {
        this.account = account;
        this.result = result;
        this.clientInfo = clientInfo;
    }
}

