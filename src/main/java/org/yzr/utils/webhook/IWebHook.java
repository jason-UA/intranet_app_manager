package org.yzr.utils.webhook;

import org.yzr.model.App;
import org.yzr.utils.file.PathManager;

public interface IWebHook {
    void sendMessage(App app, PathManager pathManager);
}
