package org.yzr.dropbox;

import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;
@Slf4j
public class DropboxTimerTask extends TimerTask {

    @Override
    public void run() {
        log.info("DropboxTimerTask run");
        DropboxThread thread = new DropboxThread();
        thread.start();
    }
}
