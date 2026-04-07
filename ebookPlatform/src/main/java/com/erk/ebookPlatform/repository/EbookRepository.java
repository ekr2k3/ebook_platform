package com.erk.ebookPlatform.repository;

import com.erk.ebookPlatform.entity.Ebook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EbookRepository extends JpaRepository<Ebook, Long> {
    List<Ebook> findBySellerId(Long sellerId);
}
