package com.example.data.device;


import com.example.data.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class UserService {

    @Autowired
    private UserRepository UserRepository;

    public List<User> findAll() {
        return UserRepository.findAll();
    }

    public User findByUserId(String user_id) {
        User UserList = UserRepository.findByUId(user_id);
        System.out.println("FeatureList1:" + UserList);
        return UserList;
    }

    public void saveFeature(User uuser) {
        UserRepository.save(uuser);
    }

    @Cacheable("Users")
    public User findOne(long id) {
        System.out.println("Cached Pages");
        return UserRepository.findOne(id);
    }

    public void delete(long id) {
        UserRepository.delete(id);
    }

}