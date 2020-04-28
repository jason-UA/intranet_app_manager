package org.yzr.dropbox;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ListFolderRequest {
    String path;
    Boolean recursive;
    Boolean include_media_info;
    Boolean include_deleted;
    Boolean include_has_explicit_shared_members;
    Boolean include_mounted_folders;
    Boolean include_non_downloadable_files;
}