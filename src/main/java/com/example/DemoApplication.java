package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		HelloTF tf = new HelloTF();
		Rcnn rcnn = new Rcnn();
		SpringApplication.run(DemoApplication.class, args);
	}
}
