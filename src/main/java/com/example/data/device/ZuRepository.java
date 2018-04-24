package com.example.data.device;

import com.example.data.domain.Zu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZuRepository extends JpaRepository<Zu, Long> {

    Zu findByZuId(String groupId);
//    List<Zu> findBy(String name);

}