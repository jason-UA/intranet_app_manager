package org.yzr.dropbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class DPFile {
    String name;
    String id;
    String pathDisplay;
    String clientModified;
    String rev;


    String getCommitID() {
        String[] strArr = this.pathDisplay.split("/");
        return strArr[3].substring(0, 4);
    }

}