package com.example.data.device;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;

import com.example.data.domain.Zu;
import com.example.data.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.data.domain.Face;

@Service
public class FaceService {

    @Autowired
    private FaceRepository FaceRepository;

    @Autowired
    private UserRepository UserRepository;

    @Autowired
    private ZuRepository ZuRepository;

    public List<Face> findAll() {
        return FaceRepository.findAll();
    }

    public List<Face> findByUId(String uid) {
        User user = UserRepository.findByUId(uid);
        List<Face> faceList = user.getFaces();
        System.out.println("FeatureList1:" + faceList);
        return faceList;
    }

    public void saveFace(Face face) {
        FaceRepository.save(face);
    }

    public void saveFace(Face face, String uid, String group_id, String user_info) {

        User user;
        if (UserRepository.findByUId(uid) == null) {
            user = new User(uid, user_info);
        } else {
            user = UserRepository.findByUId(uid);
        }

        user.getFaces().add(face);

        Zu zu = null;
        for (Zu a : user.getZus()) {
            if (a.getZuId().equals(group_id)) {
                zu = a;
                break;
            }
        }

        if (zu == null) {
            zu = new Zu(group_id);
            user.getZus().add(zu);
            zu.getUsers().add(user);
            System.out.println("out of contains");
        }

        face.setUser(user);

        UserRepository.saveAndFlush(user);
        ZuRepository.saveAndFlush(zu);
        FaceRepository.saveAndFlush(face);
    }


    @Cacheable("Faces")
    public Face findOne(long id) {
        System.out.println("Cached Pages");
        return FaceRepository.findOne(id);
    }

    public void delete(long id) {
        FaceRepository.delete(id);
    }

}