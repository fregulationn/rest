package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		HelloTF tf = new HelloTF();
		SpringApplication.run(DemoApplication.class, args);
	}
}
