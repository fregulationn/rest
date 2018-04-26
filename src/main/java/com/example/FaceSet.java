package com.example;


import com.example.data.device.FaceService;
import com.example.data.domain.Face;
import com.example.face_library.FaceNet;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.ws.rs.core.Context;

@Component
@Path("face/v2/faceset/user")
public class FaceSet {
    private static final Logger LOGGER = Logger.getLogger(FaceSet.class);

    @Autowired
    private FaceService feature_service;

    @GET
    @Produces("application/json")
    public List<String> hi() {
        List<String> result = new ArrayList<>();
        result.add("hello spring boot");
        result.add("hello micro services");
        return result;
    }

    @POST
    @Path("f")
    public File postFile(final File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String s;
            do {
                s = br.readLine();
                LOGGER.debug("f_read");
                LOGGER.debug(s);
            } while (s != null);

            LOGGER.debug("f_end");
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(new File("./save.txt")));
            out.write(f.toString().getBytes());
            out.flush();
            out.close();

            return f;
        }
    }


    @POST
    @Path("add")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> compare(@Context HttpServletRequest request) {
        Map<String, String> result = new HashMap<String, String>();

        String imgStr2 = request.getParameter("images");
        byte[] imgData2 = Base64Utils.decodeFromString(imgStr2);
        Date dt = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        File file = new File(System.getProperty("user.dir") + "/img/" + sdf.format(dt) + ".png");
        //判断目标文件所在的目录是否存在
        if(!file.getParentFile().exists()) {
            //如果目标文件所在的目录不存在，则创建父目录
            System.out.println("目标文件所在目录不存在，准备创建它！");
            if(!file.getParentFile().mkdirs()) {
                System.out.println("创建目标文件所在目录失败！");
                result.put("error_code", "216616");
                result.put("Id", UUID.randomUUID().toString().toUpperCase().replaceAll("-", ""));
                result.put("error_msg", "make dir error");
            }
        }

        try {

            FileOutputStream fops = new FileOutputStream(file);
            fops.write(imgData2);
            fops.flush();
            fops.close();
            System.out.println("图片已经写入" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {

            float[] feature = FaceNet.executeInceptionGraph1(file.getAbsolutePath());
            DataOutputStream dout = new DataOutputStream(bout);
            for (float d : feature) {
                dout.writeDouble(d);
            }
            dout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] feature = bout.toByteArray();


        Face face = new Face(feature, file.getAbsolutePath());

        String uid = request.getParameter("uid");
        String group_id = request.getParameter("group_id");
        String user_info = request.getParameter("user_info");
        try {
            feature_service.saveFace(face, uid, group_id, user_info);
            result.put("Id", UUID.randomUUID().toString().toUpperCase().replaceAll("-", ""));
        } catch (Exception e) {
            result.put("error_code", "216616");
            result.put("Id", UUID.randomUUID().toString().toUpperCase().replaceAll("-", ""));
            result.put("error_msg", "image exist");
        }


        return result;


        //            for (int i = 0; i < feature.length; i++) {
//                feature_str += Float.toString(feature[i]);
//                if(i == feature.length -1)
//                    continue;
//                feature_str += "|";
//
//            }
    }


}

