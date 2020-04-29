package org.yzr.dropbox;

import lombok.Data;

@Data
public class ListFolderRequest {
    private String path;
    private Boolean recursive;
    private Boolean include_media_info;
    private Boolean include_deleted;
    private Boolean include_has_explicit_shared_members;
    private Boolean include_mounted_folders;
    private Boolean include_non_downloadable_files;
    
    public ListFolderRequest() {
        path = "/UARUN";
        recursive = Boolean.TRUE;
        include_media_info = Boolean.FALSE;
        include_deleted = Boolean.FALSE;
        include_has_explicit_shared_members = Boolean.FALSE;
        include_mounted_folders = Boolean.FALSE;
        include_non_downloadable_files = Boolean.FALSE;
    }
}