package org.yzr.dropbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.*;

import static org.yzr.dropbox.DropboxTimerTask.ACCESSTOKEN;

@Data
@AllArgsConstructor
@Slf4j
public class PackageTask {

    public static final String DOWNLOADFILEURL = "https://content.dropboxapi.com/2/files/download";
    public static final String UPLOADFILEURL = "http://127.0.0.1/app/upload";
    private DPFile dpFile;


    public boolean isEqualWithCommitID(String commitID) {
        return dpFile.isEqualWithCommitID(commitID);
    }


    public void start() {
        File downloadFile = downloadFile(dpFile.getPathDisplay(), dpFile.getName());
        log.info("download file size: {}", downloadFile.getTotalSpace());
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
            return file;
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
        } catch (IOException e) {
            e.printStackTrace();
        }


    }




}
