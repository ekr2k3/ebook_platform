package com.erk.ebookPlatform.repository;

import com.erk.ebookPlatform.entity.EbookValidationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EbookValidationLogRepository extends JpaRepository<EbookValidationLog, Long> {
}
