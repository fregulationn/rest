package com.example.data.device;


import com.example.data.domain.Face;
import com.example.data.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUId(String user_id);
}