package com.anhui.fabricbaascommon.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import java.io.*;
import java.util.Enumeration;

public class ZipUtils {
    private static void handle(File zip, ZipOutputStream zipOut, File src, String path) throws IOException {
        if (!StringUtils.isBlank(path) && !path.endsWith(File.separator)) {
            path += File.separator;
        }
        if (!src.getPath().equals(zip.getPath())) {
            if (src.isDirectory()) {
                File[] files = src.listFiles();
                assert files != null;
                if (files.length == 0) {
                    zipOut.putNextEntry(new ZipEntry(path + src.getName() + File.separator));
                    zipOut.closeEntry();
                    return;
                }
                for (File file : files) {
                    handle(zip, zipOut, file, path + src.getName());
                }
            } else {
                InputStream inputStream = new FileInputStream(src);
                zipOut.putNextEntry(new ZipEntry(path + src.getName()));
                byte[] bytes = inputStream.readAllBytes();
                zipOut.write(bytes);
                inputStream.close();
                zipOut.closeEntry();
            }
        }
    }

    /**
     * @param dst            Zip压缩包输出路径
     * @param srcFilesOrDirs Zip压缩包根目录下的文件或文件夹
     */
    public static void zip(File dst, File... srcFilesOrDirs) throws IOException {
        assert dst.getPath().endsWith(".zip");
        try (FileOutputStream fileOut = new FileOutputStream(dst);
             ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
            zipOut.setEncoding("GBK");
            for (File file : srcFilesOrDirs) {
                handle(dst, zipOut, file, "");
            }
        }
    }

    /**
     * 如果目标路径为文件则会抛出异常
     * 如果目标路径不存在内容则会自动创建文件夹
     *
     * @param src 需要解压的Zip文件
     * @param dst Zip文件根目录下内容的输出路径
     */
    @SuppressWarnings("rawtypes")
    public static void unzip(File src, File dst) throws IOException {
        if (!dst.exists()) {
            boolean mkdirs = dst.mkdirs();
        } else if (!dst.isDirectory()) {
            throw new IOException("目标路径已存在文件");
        }
        ZipFile zipFile = new ZipFile(src, "GBK");
        for (Enumeration entries = zipFile.getEntries(); entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String path = dst + File.separator + entry.getName();
            File file = new File(path);
            if (entry.isDirectory()) {
                boolean mkdirs = file.mkdirs();
            } else {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    boolean mkdirs = parent.mkdirs();
                }
                try (InputStream inputStream = zipFile.getInputStream(entry);
                     OutputStream outputStream = new FileOutputStream(file)) {
                    byte[] bytes = inputStream.readAllBytes();
                    outputStream.write(bytes);
                    outputStream.flush();
                }
            }
        }
        zipFile.close();
    }
}
