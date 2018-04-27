package com.example;

import com.example.face_library.FaceNet;
import com.example.face_library.Rcnn;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DemoApplication {

	public static void main(String[] args) {
		FaceNet tf = new FaceNet();
		Rcnn rcnn = new Rcnn();
//		MultiIdentify mul = new MultiIdentify();
		SpringApplication.run(DemoApplication.class, args);
	}
}
