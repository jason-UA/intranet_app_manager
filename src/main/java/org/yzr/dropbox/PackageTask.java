package org.yzr.dropbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.tomcat.util.http.fileupload.ProgressListener;

import java.io.*;

import static org.yzr.dropbox.DropboxTimerTask.ACCESSTOKEN;

@Data
@AllArgsConstructor
@Slf4j
@NoArgsConstructor
public class PackageTask {

    public static final String DOWNLOADFILEURL = "https://content.dropboxapi.com/2/files/download";
    public static final String UPLOADFILEURL = "http://127.0.0.1:8081/app/upload";
    public static final String PGYERUPLOADURL = "https://www.pgyer.com/apiv2/app/upload";
    public static final String PGYERUSERKEY = "de9534c03ca2a8998560bed05ee77d36";
    public static final String PGYERAPIKEY = "4926af72bab90e1efeb1b7c9c72da5be";
    private DPFile dpFile;


    public boolean isEqual(DPFile file) {
        return dpFile.getCommitID().equals(file.getCommitID());
    }


    public void start() {
        File downloadFile = downloadFile(dpFile.getPathDisplay(), dpFile.getName());
        localhostServiceUpload(downloadFile);

    }

    public File downloadFile(String fileSavePath, String fileName) {
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
            File file = new File(dbDir, fileName);
            OutputStream outputStream = new FileOutputStream(file);
            long fileSize = response.body().contentLength();
            log.info("{}, {}", file.getAbsolutePath(), fileSize);
            int size = 0;
            long len = 0;
            byte[] buf = new byte[1024];
            int lastProcess = 0;
            while ((size = inputStream.read(buf)) != -1) {
                len += size;
                outputStream.write(buf, 0, size);
                float p = len / (float)fileSize;
                int process = (int) (p * 100);
                if (process > lastProcess) {
                    lastProcess = process;
                    log.info("文件: {} ,下载了: {}%", fileSize, process);
                }
            }
            inputStream.close();
            outputStream.close();
            response.close();
            if (fileSize == len) {
                log.info("下载成功");
                return file;
            } else {
                log.error("下载失败！");
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void localhostServiceUpload(File file) {
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

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                log.info("localhost Service Upload is Success");
            } else {
                log.info("localhost Service Upload is fail");
            }
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pgyerUpload(File file) {
        OkHttpClient client = new OkHttpClient();
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "UARun.ipa",
                        RequestBody.create(file, MediaType.parse("multipart/form-data")))
                .addFormDataPart("_api_key", PGYERAPIKEY)
                .addFormDataPart("buildInstallType", "2")
                .addFormDataPart("buildPassword", "123456")
                .build();

        ExMultipartBody exMultipartBody = new ExMultipartBody(requestBody, new UploadProgressListener() {
            @Override
            public void onProgress(long total, long current) {
                float process =  (((float) current / total) * 100);
                log.info("pgyer upload precess: {}%", process);
            }
        });

        Request request = new Request.Builder()
                .url(PGYERUPLOADURL)
                .post(exMultipartBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败
                log.info("pgyer upload is fail");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 请求成功
                log.info("pgyer upload is Success");
            }
        });
    }


    public static void main(String[] args) {
        PackageTask task = new PackageTask();
        File file = new File("/Users/User2/Desktop/intranet_app_manager/dropboxPackage/MapMyRun.ipa");
        task.pgyerUpload(file);
    }

}
