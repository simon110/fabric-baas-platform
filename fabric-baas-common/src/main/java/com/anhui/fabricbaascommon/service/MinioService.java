package com.anhui.fabricbaascommon.service;

import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class MinioService {
    @Autowired
    private MinioClient minioClient;

    private void createBucket(String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidResponseException, XmlParserException, InternalException, InvalidKeyException {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * @param bucketName 之前创建的桶名，例如"asiatrip"
     * @param objectName 保存的文件对象的名称，例如"asiaphotos-2015.zip"
     * @param file       文件，例如"/home/user/Photos/asiaphotos.zip"
     */
    public void putFile(String bucketName, String objectName, File file) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        createBucket(bucketName);
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .filename(file.getAbsolutePath())
                        .build());
    }

    public void putBytes(String bucketName, String objectName, byte[] bytes) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        createBucket(bucketName);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1).
                        build()
        );
    }

    /**
     * @param bucketName 之前创建的桶名，例如"asiatrip"
     * @param objectName 保存的文件对象的名称，例如"asiaphotos-2015.zip"
     * @param file       文件，例如"/home/user/Photos/asiaphotos.zip"
     */
    public void getAsFile(String bucketName, String objectName, File file) throws InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ServerException {
        byte[] bytes = getAsBytes(bucketName, objectName);
        FileUtils.writeByteArrayToFile(file, bytes);
    }

    public byte[] getAsBytes(String bucketName, String objectName) throws InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ServerException {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
            return IOUtils.toByteArray(stream);
        }
    }
}
