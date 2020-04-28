package org.yzr.dropbox;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DropboxThread extends Thread {

    public static final String URL = "jdbc:mysql://127.0.0.1:3306/app_manager?useUnicode=true&characterEncoding=utf-8";
    public static final String USER = "root";
    public static final String PASSWORD = "123456";
    public static final String CLASSNAME = "com.mysql.cj.jdbc.Driver";
    public static final String ACCESSTOKEN = "Bearer qPGuc1YP09AAAAAAAAAAEqBFo2eqhBKyfkRpf8NstkoFBXI9RmTr4TBVlmwFpd9_";
    public static final String LISTFOLDERURL = "https://api.dropboxapi.com/2/files/list_folder";
    public static final String LISTFOLDERCONTINUEURL = "https://api.dropboxapi.com/2/files/list_folder/continue";
    public static final String DOWNLOADFILEURL = "https://content.dropboxapi.com/2/files/download";
    public static final String UPLOADFILEURL = "http://127.0.0.1/app/upload";

    @Override
    public void run() {
        OkHttpClient okHttpClient = new OkHttpClient();
        ListFolderRequest listRequest = new ListFolderRequest();
        listRequest.path = "/UARUN";
        listRequest.recursive = Boolean.TRUE;
        listRequest.include_media_info = Boolean.FALSE;
        listRequest.include_deleted = Boolean.FALSE;
        listRequest.include_has_explicit_shared_members = Boolean.FALSE;
        listRequest.include_mounted_folders = Boolean.FALSE;
        listRequest.include_non_downloadable_files = Boolean.FALSE;

        RequestBody body =  RequestBody.create(JSON.toJSON(listRequest).toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(LISTFOLDERURL)
                .addHeader("Authorization", ACCESSTOKEN)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            //同步调用,返回Response,会抛出IO异常
            Response response = call.execute();
            JSONObject result = JSON.parseObject(response.body().string());
            List<DPFile> files = getTagFile(result);
            if (result.getBoolean("has_more")) {
                files.addAll(listFolderContinue(result));
            }
            uploadPackage(files);
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.run();
    }


    public List<DPFile> getTagFile(JSONObject result) {
        List<DPFile> files = new ArrayList<DPFile>();
        JSONArray entries = result.getJSONArray("entries");
        for (int i = 0; i < entries.size(); i++) {
            JSONObject file = entries.getJSONObject(i);
            if (file.getString(".tag").equalsIgnoreCase("file")) {
                files.add(new DPFile(
                        file.getString("name"), file.getString("id"),
                        file.getString("path_display"), file.getString("client_modified"),
                        file.getString("rev"))
                );
            }
        }
        return files;
    }

    public List<DPFile> listFolderContinue(JSONObject jsonObject) {
        String cursor = jsonObject.getString("cursor");
        OkHttpClient okHttpClient = new OkHttpClient();
        Map dict = new HashMap();
        dict.put("cursor", cursor);
        RequestBody body = RequestBody.create(JSON.toJSON(dict).toString(),  MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(LISTFOLDERCONTINUEURL)
                .addHeader("Authorization", ACCESSTOKEN)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            //同步调用,返回Response,会抛出IO异常
            Response response = call.execute();
            JSONObject result = JSON.parseObject(response.body().string());
            List<DPFile> files = getTagFile(result);
            if (result.getBoolean("has_more")) {
                files.addAll(listFolderContinue(result));
            }
            return files;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Boolean isExitInDataBase(String commit) {

        try {
            //1.加载驱动程序
            Class.forName(CLASSNAME);
            //2. 获得数据库连接
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            //3.操作数据库，实现增删改查
            Statement stmt = conn.createStatement();
            String sql = String.format("select * from tb_package where version like '%%%s'", commit) ;
            ResultSet rs = stmt.executeQuery(sql);
            //如果有数据，rs.next()返回true
            return rs.next();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void uploadPackage(List<DPFile> files) {
        log.info("all files: {}", files.toString());
        for (DPFile file: files) {
            if (isExitInDataBase(file.getCommitID())) {
                log.info(file.pathDisplay + " is exist!");
            } else {
                log.info(file.pathDisplay + " download and upload");
                downloadAndUploadFile(file.getPathDisplay(), file.getCommitID());
            }
        }
    }

    public void downloadAndUploadFile(String fileSavePath, String fileName) {
        String path = String.format("{\"path\": \"%s\"}", fileSavePath) ;
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(DOWNLOADFILEURL)
                .addHeader("Authorization", ACCESSTOKEN)
                .addHeader("Dropbox-API-Arg", path)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream());
            String dropboxPackage = System.getProperty("user.dir") + File.separator + "dropboxPackage";
            File dbDir = new File(dropboxPackage);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            File file = new File(dbDir, fileName + ".ipa");
            log.info(file.getAbsolutePath());

            OutputStream outputStream = new FileOutputStream(file);
            long fileSize = response.body().contentLength();
            int size = 0;
            float len = 0;
            byte[] buf = new byte[10240];
            while ((size = inputStream.read(buf)) != -1) {
                len += size;
                outputStream.write(buf, 0, size);
                long process = (long) ((len / fileSize) * 100);
                if ((process % 5) == 0){
                    log.info("文件: {} ,下载了: {}",fileSize,  process);
                }
            }
            inputStream.close();
            outputStream.close();
            log.info("下载成功");
            uploadFile(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void uploadFile(File file) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "UARun.ipa",
                        RequestBody.create(file, MediaType.parse("multipart/form-data")))
                .build();

        Request request = new Request.Builder()
                .url(UPLOADFILEURL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败
                log.info("上传失败！");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 请求成功
                log.info("上传成功！");
            }
        });

    }

    public static void main(String[] args) {
        DropboxThread thread = new DropboxThread();
        thread.start();
    }


}
