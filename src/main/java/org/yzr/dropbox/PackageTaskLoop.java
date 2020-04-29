package org.yzr.dropbox;

import java.util.ArrayList;
import java.util.List;

public enum  PackageTaskLoop {
    INSTANCE;

    private PackageTaskLoop() {
        packageTaskList = new ArrayList<PackageTask>();
        runTask();
    }

    private List<PackageTask> packageTaskList;

    public boolean isExistedInProcessQueue(DPFile file) {
        for (PackageTask task : packageTaskList) {
            if (task.isEqual(file)) {
                return true;
            }
        }
        return false;
    }

    public void pushPackageTask(PackageTask task) {
        packageTaskList.add(task);
    }

    private PackageTask getNextTask() {
        if (packageTaskList.isEmpty()) {
            return null;
        }
        return packageTaskList.get(0);
    }

    private void removeTask(PackageTask task) {
        packageTaskList.remove(task);
    }

    private void runTask() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    PackageTask task = getNextTask();
                    if (task != null){
                        task.start();
                        removeTask(task);
                    }
                }
            }
        }).start();
    }
}
