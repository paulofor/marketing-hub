package com.marketinghub.ads;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "fb_page")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacebookPage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private FacebookAccount account;

    @Column(name = "page_id", nullable = false, length = 128)
    private String pageId;

    @Column(nullable = false, length = 255)
    private String name;
}
