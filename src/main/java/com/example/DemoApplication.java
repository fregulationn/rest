package com.example;

import com.example.face_library.FaceNet;
import com.example.face_library.Rcnn;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		FaceNet tf = new FaceNet();
		Rcnn rcnn = new Rcnn();
		SpringApplication.run(DemoApplication.class, args);
	}
}
