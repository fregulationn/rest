package com.example;


import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import org.glassfish.jersey.media.multipart.MultiPartFeature;


@Component
@ApplicationPath("/rest/2.0")
public class JerseyConfig extends ResourceConfig {
	public JerseyConfig() {
		register(FaceSet.class);
		register(CorsFilter.class);
		register(MultiPartFeature.class);
		register(FileUploadEndpoint.class);
	}
}