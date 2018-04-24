package com.example.data.domain;


import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Integer id;

    @Column(nullable = false)
    private String uId;

    @Column(nullable = false)
    private String userInfo;

    @ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @JoinTable(name = "zu_user", joinColumns = {
            @JoinColumn(name = "userId", referencedColumnName = "id")}, inverseJoinColumns = {
            @JoinColumn(name = "zuId", referencedColumnName = "id")})
    private List<Zu> zus;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    //拥有mappedBy注解的实体类为关系被维护端
    //mappedBy="face"中的face是Face中的face属性
    private List<Face> faces;

    public User() {
        super();
        this.zus = new ArrayList<>();
        this.faces = new ArrayList<>();
    }

    public User(String uid, String uerInfo) {
        super();
        this.zus = new ArrayList<>();
        this.faces = new ArrayList<>();
        this.uId = uid;
        this.userInfo = uerInfo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public List<Zu> getZus() {
        return zus;
    }

    public void setZus(List<Zu> zus) {
        this.zus = zus;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public void setFaces(List<Face> faces) {
        this.faces = faces;
    }
}