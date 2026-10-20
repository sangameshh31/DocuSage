package com.docusage.repository;

import com.docusage.model.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChunkRepository extends JpaRepository<ChunkEntity, String> {

    List<ChunkEntity> findByDocumentId(String documentId);

    void deleteByDocumentId(String documentId);
}
