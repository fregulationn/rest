package com.example.data.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "zu")
public class Zu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Integer id;

    @Column(nullable = false)
    private String zuId;

    @ManyToMany(mappedBy = "zus",fetch = FetchType.EAGER)
    private List<User> users;

    public Zu() {
        super();
        this.users = new ArrayList<>();
    }

    public Zu(String zuId) {
        super();
        this.users = new ArrayList<>();
        this.zuId = zuId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getZuId() {
        return zuId;
    }

    public void setZuId(String zuId) {
        this.zuId = zuId;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}