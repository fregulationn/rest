package com.example.data.device;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.data.domain.Face;

import java.util.List;


public interface FaceRepository extends JpaRepository<Face, Long> {


}
