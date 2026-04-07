package com.erk.ebookPlatform.entity;

import com.erk.ebookPlatform.enums.SourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_ebooks",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_ebook", columnNames = {"user_id", "ebook_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class UserEbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebook_id", nullable = false)
    private Ebook ebook;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType source = SourceType.PURCHASE;

    @PrePersist
    public void prePersist() {
        this.acquiredAt = LocalDateTime.now();
    }
}
