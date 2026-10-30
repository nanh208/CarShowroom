package com.ares.model;

import jakarta.persistence.*;

/** Adapted from CarShowroom-copy's category model, keeping existing storefront codes. */
@Entity
@Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public String code;
    @Column(nullable = false)
    public String name;
    public String grp;
    public String image;
    public String icon;
    public int sortOrder;

    public Category() {}

    public Category(String code, String name, String grp, String icon, int sortOrder) {
        this.code = code;
        this.name = name;
        this.grp = grp;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }
}
