package com.anhui.fabricbaascommon.util;

import cn.hutool.core.lang.Assert;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

public class MyFileUtils {
    public static void assertFileExists(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("文件不存在：" + file.getAbsolutePath());
        }
    }

    public static void assertDirExists(File dir) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException("文件夹不存在：" + dir.getAbsolutePath());
        }
    }

    /**
     * @return 当前程序运行的目录
     */
    public static String getWorkingDir() {
        return System.getProperty("user.dir");
    }

    /**
     * @param path 任意文件路径
     * @return 成功被创建的文件夹
     */
    public static File createDir(String path) throws IOException {
        File dir = new File(path);
        if (dir.exists()) {
            throw new IOException("路径已存在文件或文件夹：" + path);
        }
        boolean mkdirs = dir.mkdirs();
        Assert.isTrue(mkdirs);
        return dir;
    }

    /**
     * @return 在临时文件目录下的空文件夹，文件夹名称为UUID
     */
    public static File createTempDir() throws IOException {
        return createDir(String.format("%s/temp/%s", getWorkingDir(), UUID.randomUUID()));
    }

    /**
     * @param filetype 文件类型
     * @return 在临时文件目录下的空文件，文件名为UUID，后缀为文件类型
     */
    public static File createTempFile(String filetype) {
        return new File(String.format("%s/temp/%s.%s", getWorkingDir(), UUID.randomUUID(), filetype));
    }

    /**
     * @param path 任意路径
     * @return 该路径是否存在文件或文件夹
     */
    public static boolean exists(String path) {
        return new File(path).exists();
    }

    /**
     * @param path 相对路径
     * @return 绝对路径
     */
    public static String toCanonicalPath(String path) throws IOException {
        return new File(path).getCanonicalPath();
    }

    public static MultipartFile toMultipartFile(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        return new MockMultipartFile(file.getName(), fileInputStream);
    }
}
