package org.yzr.dropbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class DPFile {
    private String name;
    private String id;
    private String pathDisplay;
    private String clientModified;
    private String rev;


    public String getCommitID() {
        String[] strArr = this.pathDisplay.split("/");
        return strArr[3].substring(0, 4);
    }

    public boolean isEqualWithCommitID(String commitID) {
        return getCommitID() == commitID;
    }


}