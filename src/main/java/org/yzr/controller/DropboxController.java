package org.yzr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yzr.DropboxRequest;
import org.yzr.vo.AppViewModel;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class DropboxController {

    @GetMapping("/dropbox")
    public String dropbox() throws Exception {
        DropboxRequest.postDownloadFile("https://content.dropboxapi.com/2/files/download", "/UA/MapMyRun.ipa");
        return "dropbox download";
    }

    @GetMapping("/uploadPackage")
    public String uploadPackage() {
        DropboxRequest.fileFolder();
        return "upload";
    }
}
