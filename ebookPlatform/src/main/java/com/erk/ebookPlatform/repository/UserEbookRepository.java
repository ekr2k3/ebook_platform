package com.erk.ebookPlatform.repository;

import com.erk.ebookPlatform.entity.UserEbook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEbookRepository extends JpaRepository<UserEbook, Long> {
    List<UserEbook> findByUserId(Long userId);
}
