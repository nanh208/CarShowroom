package com.ares.controller;

import com.ares.model.BlogPost;
import com.ares.model.User;
import com.ares.service.BlogPostService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** API contract consumed by the existing public blog pages and CMS editor. */
@RestController
public class BlogPostApiController {
    private final BlogPostService blog;
    private final Set<String> adminUsernames;

    public BlogPostApiController(BlogPostService blog,
            @Value("${app.cms.admin-usernames:admin}") String configuredAdminUsernames) {
        this.blog = blog;
        this.adminUsernames = Arrays.stream(configuredAdminUsernames.split(","))
                .map(String::trim).filter(name -> !name.isBlank())
                .map(name -> name.toLowerCase(java.util.Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    @GetMapping("/api/public/posts")
    public Map<String, Object> publicList(@RequestParam(required = false) String kind,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        return blog.listPublic(kind, tag, q, page, size);
    }

    @GetMapping("/api/public/posts/tags")
    public Object publicTags() {
        return blog.popularTags();
    }

    @GetMapping("/api/public/posts/{slug}")
    public Map<String, Object> publicDetail(@PathVariable String slug,
            @RequestParam(defaultValue = "true") boolean count) {
        return blog.publicDetail(slug, count);
    }

    @GetMapping("/api/admin/posts")
    public ResponseEntity<?> adminList(HttpSession session) {
        ResponseEntity<Map<String, String>> gate = adminGate(session);
        if (gate != null) return gate;
        return ResponseEntity.ok(blog.adminList());
    }

    @GetMapping("/api/admin/posts/{id}")
    public ResponseEntity<?> adminDetail(@PathVariable Long id, HttpSession session) {
        ResponseEntity<Map<String, String>> gate = adminGate(session);
        if (gate != null) return gate;
        return ResponseEntity.ok(blog.adminDetail(id));
    }

    @PostMapping("/api/admin/posts")
    public ResponseEntity<?> create(@RequestBody BlogPost input, HttpSession session) {
        User user = currentUser(session);
        ResponseEntity<Map<String, String>> gate = adminGate(session);
        if (gate != null) return gate;
        return ResponseEntity.status(HttpStatus.CREATED).body(blog.create(input, user));
    }

    @PutMapping("/api/admin/posts/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BlogPost input, HttpSession session) {
        User user = currentUser(session);
        ResponseEntity<Map<String, String>> gate = adminGate(session);
        if (gate != null) return gate;
        return ResponseEntity.ok(blog.update(id, input, user));
    }

    @DeleteMapping("/api/admin/posts/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        ResponseEntity<Map<String, String>> gate = adminGate(session);
        if (gate != null) return gate;
        blog.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> apiError(ResponseStatusException error) {
        String message = error.getReason() == null ? "Yêu cầu không hợp lệ." : error.getReason();
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", message));
    }

    private static User currentUser(HttpSession session) {
        if (session == null) return null;
        Object user = session.getAttribute("currentUser");
        return user instanceof User current ? current : null;
    }

    private ResponseEntity<Map<String, String>> adminGate(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase(java.util.Locale.ROOT);
        if (!adminUsernames.contains(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Tài khoản không có quyền quản lý bài viết."));
        }
        return null;
    }

    private static ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Vui lòng đăng nhập để quản lý bài viết."));
    }
}
