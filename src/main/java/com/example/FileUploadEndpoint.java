package com.example;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import java.net.URLEncoder;

import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletResponse;


import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.data.device.FaceService;
import org.springframework.util.Base64Utils;

import com.example.Rcnn;


@Component
@Path("/face/v2")
public class FileUploadEndpoint {

    @Autowired
    private FaceService feature_service;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String upload(@FormDataParam("file") InputStream fis,
                         @FormDataParam("file") FormDataContentDisposition fileDisposition) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(fis, baos);
            String content = new String(baos.toByteArray());
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("compare")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String compare(@FormDataParam("file") InputStream uploadedInputStream,
                       @FormDataParam("file") FormDataContentDisposition disposition ,
                          @FormDataParam("file1") InputStream uploadedInputStream1,
                          @FormDataParam("file1") FormDataContentDisposition disposition1) {
        //FormDataParam是指定对应的表单的名字

        String imageName = Calendar.getInstance().getTimeInMillis()
                + disposition.getFileName();
        String imageName1 = Calendar.getInstance().getTimeInMillis() + disposition1.getFileName();

        File file = new File( imageName);
        File file1 = new File( imageName1);

        try {
            //使用common io的文件写入操作
            FileUtils.copyInputStreamToFile(uploadedInputStream, file);
            FileUtils.copyInputStreamToFile(uploadedInputStream1, file1);
            //原来自己的文件写入操作
            //saveFile(fileInputStream, file);
        } catch (IOException ex) {
            Logger.getLogger(FileUploadEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
//        System.out.println(file.getAbsolutePath());
//        System.out.println(file1.getAbsolutePath());


        String output = "";
        try {
            long startTime = System.currentTimeMillis();
            double distance = HelloTF.executeInceptionGraph(file.getAbsolutePath(),file1.getAbsolutePath());
            long endTime = System.currentTimeMillis();
            float seconds = (endTime - startTime) / 1000F;
            System.out.println("all:"+Float.toString(seconds) + " seconds.");
            output += Double.toString(distance);


        }catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            //需传入的参数
//            String py = "D:\\Z\\code\\video_detect\\compare_api.py ",a = file.getAbsolutePath(),b= file1.getAbsolutePath();
//            //设置命令行传入参数
//            String[] args = new String[] { "python", py, a,b};
//
//            String exrc = "";
//            for (int i = 0; i<args.length;i++)
//                exrc = exrc+ " "+args[i];
//            System.out.println(exrc);
//
//            Process pr = Runtime.getRuntime().exec(exrc);
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//            String line;
//            while ((line = in.readLine()) != null) {
////                line = decodeUnicode(line);
//                output += line;
//                output += "\n";
//                System.out.println(line);
//            }
//            in.close();
//
//            int status = pr.waitFor();
//            if(status != 0){
//                System.err.println("Failed to call shell's command and the return status's is: " + status);
//            }else{
//                System.out.println("call shell sucessful");
//            }
//
//            System.out.println("end");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        file.delete();
        file1.delete();

        return output;
    }

    @POST
    @Path("detect_py")
    public void detect_py(@FormDataParam("file") InputStream uploadedInputStream,
                       @FormDataParam("file") FormDataContentDisposition disposition,
                       @Context HttpServletResponse response)
            throws IOException {

//        System.out.println("showImg");
//        String path = "D:\\Z\\code\\rest\\a.zip";
//        String path1 = "D:/Z/code/video_detect/data/api/20180329_095958/2.png";
//        ArrayList<String> arrayList = new ArrayList<String>();
//        arrayList.add(path);
//        arrayList.add(path1);

        /** 1.创建临时文件夹  */
        File temDir = new File( System.getProperty("user.dir")+"/" +
                UUID.randomUUID().toString().replaceAll("-", ""));
        if(!temDir.exists()){
            temDir.mkdirs();
        }
        System.out.println(System.getProperty("user.dir"));

        //FormDataParam是指定对应的表单的名字

        String imageName = Calendar.getInstance().getTimeInMillis()
                + disposition.getFileName();

        File file = new File(imageName);
        try {
            //使用common io的文件写入操作
            FileUtils.copyInputStreamToFile(uploadedInputStream, file);
            //原来自己的文件写入操作
            //saveFile(fileInputStream, file);
        } catch (IOException ex) {
            Logger.getLogger(FileUploadEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
//
//        System.out.println(file.getAbsolutePath());
//        System.out.println(temDir.getAbsolutePath());



        try {
            //需传入的参数
            String py = "D:\\Z\\code\\video_detect\\detect_api.py ", a = file.getAbsolutePath(),c = temDir.getAbsolutePath();
            //设置命令行传入参数
            String[] args = new String[] { "python", py, a, c };

            String exrc = "";
            for (int i = 0; i<args.length;i++)
                exrc = exrc+ " "+args[i];
            System.out.println(exrc);

            Process pr = Runtime.getRuntime().exec(exrc);

            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
//                line = decodeUnicode(line);
                System.out.println(line);
            }
            in.close();

            int status = pr.waitFor();
            if(status != 0){
                System.err.println("Failed to call shell's command and the return status's is: " + status);
            }else{
                System.out.println("call shell sucessful");
            }

            System.out.println("end");
        } catch (Exception e) {
            e.printStackTrace();
        }

//        System.out.println(path);
//        //获取文件名
////        String filename = path.substring(path.lastIndexOf("\\")+1);
//        File file1 = new File(path);
//        String filename = file1.getName();
        String filename = "pic.zip";
        System.out.println(filename);
        //将文件名进行URL编码
        filename = URLEncoder.encode(filename,"utf-8");
        System.out.println(filename);
        response.reset(); // 必要地清除response中的缓存信息
        response.setContentType("application/octet-stream; charset=utf-8");
        //告诉浏览器用下载的方式打开文件
        response.setHeader("content-disposition", "attachment;filename="+filename);

//        try (InputStream in = new FileInputStream(path)) {
//            FileUtils.copyFile(new File(path), response.getOutputStream());
////             FileCopyUtils.copy(in, response.getOutputStream());
//        }


        /** 4.调用工具类，下载zip压缩包 */
        // 这里我们不需要保留目录结构
        ZipUtils.toZip(temDir.getPath(), response.getOutputStream(),false);

        /** 5.删除临时文件和文件夹 */
        // 这里我没写递归，直接就这样删除了
        File[] listFiles = temDir.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
            listFiles[i].delete();
        }
        temDir.delete();
        file.delete();

    }


    @POST
    @Path("multi-identify-batch")
    @Produces(MediaType.APPLICATION_JSON)
    public Map multiIdentify(@Context HttpServletRequest request) {
        Map<String,Object> res = new HashMap<>();
        res.put("log_id", 73473737);
        res.put("image_num", 1);

        List<Object> result = new ArrayList<>();

        Map<String,Object> res1 = new HashMap<>();
        res1.put("group_id","test1");
        res1.put("uid","u333333");
        res1.put("user_info","Test User");
        Map<String,Object> position = new HashMap<>();
        position.put("left",726.99188232422);
        position.put("top",288.37701416016);
        position.put("height",42);
        position.put("width",44);
        position.put("degree",-4);
        position.put("prob",0.91117089986801);
        res1.put("position",position);
        res1.put("scores",99.3);

        result.add(res1);

        Map<String,Object> rest2 = new HashMap<>();
        rest2.put("group_id","test1");
        rest2.put("uid","u2222222");
        rest2.put("user_info","Test User");
        Map<String,Object> position2 = new HashMap<>();
        position2.put("left",726.99188232422);
        position2.put("top",288.37701416016);
        position2.put("height",42);
        position2.put("width",44);
        position2.put("degree",-4);
        position2.put("prob",0.91117089986801);
        rest2.put("position",position2);
        rest2.put("scores",82.3);

        result.add(rest2);
        res.put("result_all", result);

        return res;

    }

    @POST
    @Path("detect")
    @Produces(MediaType.APPLICATION_JSON)
    public Map detect(@Context HttpServletRequest request) {
        Map<String,String[]> request_value =request.getParameterMap();

        String imgStr = request_value.get("image")[0];
        byte[] imgData = Base64Utils.decodeFromString(imgStr);
        int max_face_num = 1;
        if(request_value.containsKey("max_face_num")) {
            max_face_num = Integer.parseInt(request.getParameter("max_face_num"));
        }

        List<Object> detect_result = Rcnn.executeGraph(imgData);
        float [][] box = (float[][])detect_result.get(0);
        float [][] prob = (float[][])detect_result.get(1);


        Map<String,Object> res = new HashMap<>();
        res.put("log_id", 73473737);
        res.put("result_num", max_face_num);

        List<Object> result = new ArrayList<>();
        for (int i = 0; i < max_face_num; i++) {
            Map<String,Object> res_tmp = new HashMap<>();
            Map<String,Object> position = new HashMap<>();
            position.put("left",box[i][1]);
            position.put("top",box[i][0]);
            position.put("height",box[i][2]-box[i][0]);
            position.put("width",box[i][3]-box[i][1]);

            res_tmp.put("location",position);
            res_tmp.put("face_probability",prob[0][i]);
            result.add(res_tmp);
        }
        res.put("result", result);

        return res;
    }



}