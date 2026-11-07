package com.ares.repository;

import com.ares.model.BlogPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<BlogPost> findByStatusOrderByPublishedAtDesc(String status);
    List<BlogPost> findAllByOrderByIdDesc();
}
