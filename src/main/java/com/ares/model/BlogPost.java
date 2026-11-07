package com.ares.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Managed article content for news, blog posts, and video reviews. */
@Entity
@Table(name = "blog_posts")
public class BlogPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 180)
    private String titleEn;

    @Column(length = 500)
    private String excerpt;

    @Column(length = 500)
    private String excerptEn;

    @Lob
    private String contentHtml;

    @Lob
    private String contentHtmlEn;

    @Column(length = 2048)
    private String coverImage;

    @Column(nullable = false, length = 16)
    private String kind = "BLOG";

    @Column(length = 2048)
    private String videoUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "blog_post_tags", joinColumns = @JoinColumn(name = "post_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag"}))
    @Column(name = "tag", nullable = false, length = 40)
    private Set<String> tags = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "blog_post_products", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "car_id", nullable = false)
    private List<Long> productIds = new ArrayList<>();

    @Column(nullable = false, length = 16)
    private String status = "DRAFT";

    private LocalDateTime publishedAt;

    @Column(length = 120)
    private String author;

    @Column(length = 100)
    private String seoTitle;

    @Column(length = 300)
    private String seoDescription;

    @Column(nullable = false)
    private int views;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Transient
    private int readMinutes;

    public BlogPost() {}

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getExcerptEn() { return excerptEn; }
    public void setExcerptEn(String excerptEn) { this.excerptEn = excerptEn; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
    public String getContentHtmlEn() { return contentHtmlEn; }
    public void setContentHtmlEn(String contentHtmlEn) { this.contentHtmlEn = contentHtmlEn; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags); }
    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds == null ? new ArrayList<>() : new ArrayList<>(productIds); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public int getViews() { return views; }
    public void setViews(int views) { this.views = Math.max(0, views); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getReadMinutes() { return readMinutes; }
    public void setReadMinutes(int readMinutes) { this.readMinutes = Math.max(1, readMinutes); }
}
