package com.pm.moment;

import java.io.File;
import java.util.List;

public class WorkWithFiles {

    public static void checkDefaultDir(String defaultDirPath) {
        File defaultDir = new File(defaultDirPath);
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }
    }

    public static int clear(List<String> filePaths) {
        for (int i = 0; i < filePaths.size(); i++) {
            File file = new File(filePaths.get(i));
            if (!file.exists()) {
                return ResultsCodes.FILE_NOT_EXIST;
            }
            file.delete();
        }
        return ResultsCodes.CLEAR_DONE;
    }

}
