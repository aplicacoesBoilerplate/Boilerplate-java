package com.java.boilerplate.model;

import com.java.boilerplate.enums.MatchPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_contexts")
@Getter
@Setter
public class AppContext {
    @Id
    @Column(name = "context_key", length = 50)
    private String contextKey;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "url_path", nullable = false)
    private String urlPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_policy", nullable = false)
    private MatchPolicy matchPolicy;
}
