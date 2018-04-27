package com.example.data.device;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.data.domain.Zu;
import com.example.data.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.data.domain.Face;

@Service
@CacheConfig(cacheNames = "faces")
public class FaceService {

    @Autowired
    private FaceRepository FaceRepository;

    @Autowired
    private UserRepository UserRepository;

    @Autowired
    private ZuRepository ZuRepository;

    @Cacheable("faces")
    public List<Face> findAll() {
        return FaceRepository.findAll();
    }

    public List<Face> findByUId(String uid) {
        User user = UserRepository.findByUId(uid);
        List<Face> faceList = user.getFaces();
        System.out.println("FeatureList1:" + faceList);
        return faceList;
    }

    @Cacheable("faces")
    public List<Face> findByZuId(String groupid) {
        List<Face> res = new ArrayList<>();
        Zu tmp = ZuRepository.findByZuId(groupid);
        List<User> tmpzu = tmp.getUsers();
        for (User user : tmpzu) {
            List<Face> tmp_user = user.getFaces();
            res.addAll(tmp_user);
        }
        return res;
    }


    public void saveFace(Face face) {
        FaceRepository.save(face);
    }

    public void saveFace(Face face, String uid, String[] group_id, String user_info) {

        User user;
        if (UserRepository.findByUId(uid) == null) {
            user = new User(uid, user_info);
            System.out.println("can't find the user");

        } else {
            user = UserRepository.findByUId(uid);
            System.out.println("find the user");
        }

        user.getFaces().add(face);

        for (String single_group_id : group_id) {
            Zu zu = null;
            for (Zu a : user.getZus()) {
                System.out.println(a.getZuId());
                if (a.getZuId().equals(single_group_id)) {
                    zu = a;
                    break;
                }
            }

            if (zu == null) {
                if (ZuRepository.findByZuId(single_group_id) == null) {
                    zu = new Zu(single_group_id);
                    System.out.println("can't find the zu");
                    ZuRepository.save(zu);
                } else {
                    zu = ZuRepository.findByZuId(single_group_id);
                    System.out.println("find the zu");
                }
                user.getZus().add(zu);
                zu.getUsers().add(user);
                System.out.println("out of contains");
            }else
                continue;

            face.setUser(user);
            ZuRepository.saveAndFlush(zu);
            UserRepository.saveAndFlush(user);
        }

        FaceRepository.saveAndFlush(face);
    }


    @Cacheable("faces")
    public Face findOne(long id) {
        System.out.println("Cached Pages");
        return FaceRepository.findOne(id);
    }

    public void delete(long id) {
        FaceRepository.delete(id);
    }

}