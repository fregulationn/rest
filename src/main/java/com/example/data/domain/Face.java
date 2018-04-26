package com.example.data.domain;

import java.io.Serializable;
import javax.persistence.*;


@Entity
@Table(name = "face")
public class Face implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue
    private int id;

    @Lob
    @Column(length = 1000000)
    private byte[] feature;

    @Column(nullable = false)
    private String imgPath;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH},fetch = FetchType.EAGER)//可选属性optional=false,表示company不能为空
    @JoinColumn(name = "userId")//设置在user表中的关联字段(外键)
    private User user;

    public Face() {
    }

    public Face( byte[] feature, String imgPath) {
        super();
        this.feature = feature;
        this.imgPath = imgPath;
    }

    public Face(Face face) {
        super();
        this.feature = face.feature;
        this.imgPath = face.imgPath;
        this.user = face.user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public byte[] getFeature() {
        return feature;
    }

    public void setFeature(byte[] feature) {
        this.feature = feature;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}


