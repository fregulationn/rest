package com.example.data.device;

import com.example.data.domain.Zu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ZuService {

    @Autowired
    private ZuRepository ZuRepository;

    public List<Zu> findAll() {
        return ZuRepository.findAll();
    }

    public Zu findByZuId(String Zu_id) {
        Zu zuList = ZuRepository.findByZuId(Zu_id);
        System.out.println("FeatureList1:" + zuList);
        return zuList;
    }

    public void saveFeature(Zu zu) {
        ZuRepository.save(zu);
    }

    @Cacheable("Zus")
    public Zu findOne(long id) {
        System.out.println("Cached Pages");
        return ZuRepository.findOne(id);
    }

    public void delete(long id) {
        ZuRepository.delete(id);
    }

}