package org.yzr.dropbox;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;


@Slf4j
public class DropboxTimerTask extends TimerTask {

    public static final String LISTFOLDERURL = "https://api.dropboxapi.com/2/files/list_folder";
    public static final String LISTFOLDERCONTINUEURL = "https://api.dropboxapi.com/2/files/list_folder/continue";
    public static final String ACCESSTOKEN = "Bearer qPGuc1YP09AAAAAAAAAAJTFayt7eJpu7mJWxA7g1h-mjMCM9THSrYJMunSyZBzK4";

    @Override
    public void run() {
        OkHttpClient okHttpClient = new OkHttpClient();
        ListFolderRequest listRequest = new ListFolderRequest();
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
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                List<DPFile> files = getTagFile(result);
                if (result.getBoolean("has_more")) {
                    files.addAll(listFolderContinue(result));
                }
                createPackageTask(files);
            } else {
                log.error("deopbox folder list fail code: {}", response.code());
            }
            response.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
            response.close();
            if (result.getBoolean("has_more")) {
                files.addAll(listFolderContinue(result));
            }
            return files;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void createPackageTask( List<DPFile> files) {
        for (DPFile file: files) {
            if (AppDatabase.INSTANCE.isPackageExisted(file.getCommitID())) {
                log.info(file.getPathDisplay() + " is exist in database!");
            } else if (PackageTaskLoop.INSTANCE.isExistedInProcessQueue(file.getCommitID())){
                log.info(file.getPathDisplay() + " is exist in process queue!");
            } else {
                log.info(file.getPathDisplay() + " will download and upload!");
                PackageTaskLoop.INSTANCE.pushPackageTask(new PackageTask(file));
            }
        }
    }




}
