package org.yzr;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class DropboxRequest {

    public static void postDownloadFile(String urlPath, String fileSavePath) throws Exception {

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(urlPath)
                .addHeader("Authorization", "Bearer wzVJmnwtx2AAAAAAAAAAn6aJPZ8la-NL5X9eIJvdRw_JbWaIqZqqAYn2Sjl0cTkA")
                .addHeader("Dropbox-API-Arg", "{\"path\": \"/UARun/cnfeature_FIR/44617703/MapMyRun.ipa\"}")
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error(e.toString());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream());
                String dropboxPackage = System.getProperty("user.dir") + File.separator + "dropboxPackage";
                File dbDir = new File(dropboxPackage);
                if (!dbDir.exists()) {
                    dbDir.mkdirs();
                }
                File file = new File(dbDir, "map.ipa");
                log.info(file.getAbsolutePath());

                OutputStream outputStream = new FileOutputStream(file);
                long fileSize = response.body().contentLength();
                int size = 0;
                float len = 0;
                byte[] buf = new byte[10240];
                while ((size = inputStream.read(buf)) != -1) {
                    len += size;
                    outputStream.write(buf, 0, size);
//                    log.info("文件: {} ,下载了: {}",fileSize,  len / fileSize);
                }
                inputStream.close();
                outputStream.close();
                log.info("下载成功");

            }
        });
    }

    public static void upload() {
        String dropboxPackage = System.getProperty("user.dir") + File.separator + "dropboxPackage";
        File dbDir = new File(dropboxPackage);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        File file = new File(dbDir, "map.ipa");
        log.info(file.getAbsolutePath());
        postUploadFile("http://127.0.0.1/app/upload", "map.ipa", file.getAbsolutePath());
    }

    public static void postUploadFile(String urlPath, String fileName, String filePath) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName,
                        RequestBody.create(new File(filePath), MediaType.parse("multipart/form-data")))
                .build();

        Request request = new Request.Builder()
                .url(urlPath)
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

    @Data
    @NoArgsConstructor
    public static class ListFolderRequest {
        String path;
        Boolean recursive;
        Boolean include_media_info;
        Boolean include_deleted;
        Boolean include_has_explicit_shared_members;
        Boolean include_mounted_folders;
        Boolean include_non_downloadable_files;
    }

    public static void fileFolder() {
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("path", "/UARUN")
                .add("recursive", "true")
                .add("include_media_info", "false")
                .add("include_deleted", "false")
                .add("include_has_explicit_shared_members", "false")
                .add("include_mounted_folders", "false")
                .add("include_non_downloadable_files", "false")
                .build();
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
                .url("https://api.dropboxapi.com/2/files/list_folder")
                .addHeader("Authorization", "Bearer wzVJmnwtx2AAAAAAAAAAn6aJPZ8la-NL5X9eIJvdRw_JbWaIqZqqAYn2Sjl0cTkA")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        Call call = okHttpClient.newCall(request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //同步调用,返回Response,会抛出IO异常
                    Response response = call.execute();
                    JSONObject result = JSON.parseObject(response.body().string());
                    List<DPFile> files = getTagFile(result);
                    if (result.getBoolean("has_more")) {
                        files.addAll(listFolderContinue(result));
                    }
                    log.info("all files: {}", files.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    static List<DPFile> listFolderContinue(JSONObject jsonObject) {
        String cursor = jsonObject.getString("cursor");
        OkHttpClient okHttpClient = new OkHttpClient();
        Map dict = new HashMap();
        dict.put("cursor", cursor);
        RequestBody body = RequestBody.create(JSON.toJSON(dict).toString(),  MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://api.dropboxapi.com/2/files/list_folder/continue")
                .addHeader("Authorization", "Bearer wzVJmnwtx2AAAAAAAAAAn6aJPZ8la-NL5X9eIJvdRw_JbWaIqZqqAYn2Sjl0cTkA")
                .post(body)
                .addHeader("Content-Type", "application/json")
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
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DPFile {
        String name;
        String id;
        String pathDisplay;
        String clientModified;
        String rev;

    }

    static List<DPFile> getTagFile(JSONObject result) {
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





}
