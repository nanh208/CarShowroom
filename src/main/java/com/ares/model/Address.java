package com.ares.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String label;
    @Column(nullable = false, length = 120)
    private String receiverName;
    @Column(nullable = false, length = 30)
    private String phone;
    @Column(nullable = false, length = 255)
    private String line;
    @Column(length = 120)
    private String ward;
    @Column(length = 120)
    private String district;
    @Column(nullable = false, length = 120)
    private String city;
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
