package com.admin4j.oss.impl;

import com.admin4j.oss.MockMultipartFile;
import com.admin4j.oss.OssProperties;
import com.admin4j.oss.OssTemplate;
import com.admin4j.oss.UploadFileService;
import com.admin4j.oss.entity.vo.UploadFileVO;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @author andanyang
 * @since 2023/4/14 9:27
 */
// @RequiredArgsConstructor
public class SimpleOSSUploadFileService implements UploadFileService {

    protected final static DateTimeFormatter FILEPATH_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    protected final OssTemplate ossTemplate;

    protected final OssProperties ossProperties;

    public SimpleOSSUploadFileService(OssTemplate ossTemplate, OssProperties ossProperties) {
        this.ossTemplate = ossTemplate;
        this.ossProperties = ossProperties;
    }


    /**
     * 上传文件
     *
     * @param file 文件
     * @param path 路径，可以是文件类型
     */
    @Override
    public UploadFileVO upload(String path, MultipartFile file) throws IOException {

        UploadFileVO uploadFileVO = new UploadFileVO();
        uploadFileVO.setOriginalFilename(file.getOriginalFilename());
        uploadFileVO.setSize(file.getSize());
        uploadFileVO.setContentType(file.getContentType());
        uploadFileVO.setCreateTime(LocalDateTime.now());
        uploadFileVO.setBucket(defaultBucketName());
        // 计算文件md5
        String md5 = DigestUtils.md5Hex(file.getBytes());
        uploadFileVO.setMd5(md5);

        // 生成文件存储路径
        if (StringUtils.isNotBlank(path)) {
            uploadFileVO.setPrefix(path);
        }
        path = generateFilePath(uploadFileVO);
        uploadFileVO.setKey(path);

        return upload(uploadFileVO, file.getInputStream());
    }

    @Override
    public UploadFileVO upload(UploadFileVO uploadFileVO, InputStream is) throws IOException {

        UploadFileVO beforeUploadFileVO = beforeUpload(uploadFileVO);
        if (beforeUploadFileVO != null) {
            beforeUploadFileVO.setOriginalFilename(uploadFileVO.getOriginalFilename());
            return beforeUploadFileVO;
        }

        PutObjectResult putObjectResult = ossTemplate.putObject(defaultBucketName(), uploadFileVO.getKey(), is, uploadFileVO.getContentType());

        if (uploadFileVO.getMd5() != null && !putObjectResult.getETag().equals(uploadFileVO.getMd5())) {
            // md5 不一样存在丢包行为
            throw new IllegalStateException("Upload file md5 error");
        }
        uploadFileVO.setPreviewUrl(getPreviewUrl(uploadFileVO.getKey()));
        afterUpload(uploadFileVO, putObjectResult);

        return uploadFileVO;
    }

    /**
     * 上传文件
     *
     * @param key 指定路径（key）
     * @param is  InputStream
     * @return
     * @throws IOException
     */
    @Override
    public UploadFileVO upload(String key, InputStream is) throws IOException {


        return upload(key, null, null, is);
    }

    /**
     * 上传文件
     *
     * @param key              指定路径（key）
     * @param originalFilename 文件原始名称
     * @param contentType      文件类型
     * @param is               上传流
     * @return
     * @throws IOException
     */
    @Override
    public UploadFileVO upload(String key, String originalFilename, String contentType, InputStream is) throws IOException {

        MockMultipartFile file = new MockMultipartFile(key, is);

        UploadFileVO uploadFileVO = new UploadFileVO();
        uploadFileVO.setOriginalFilename(originalFilename);
        uploadFileVO.setSize(file.getSize());
        uploadFileVO.setContentType(contentType);
        uploadFileVO.setCreateTime(LocalDateTime.now());
        uploadFileVO.setBucket(defaultBucketName());
        // 计算文件md5
        String md5 = DigestUtils.md5Hex(file.getBytes());
        uploadFileVO.setMd5(md5);

        uploadFileVO.setKey(key);

        return upload(uploadFileVO, file.getInputStream());
    }


    /**
     * 文件预览路径
     *
     * @param key oss key
     * @return 文件阅览路径
     */
    @Override
    public String getPreviewUrl(String key) {
        if (StringUtils.isNotBlank(ossProperties.getPreviewUrl())) {
            if (ossProperties.getExpires() == -1) {
                return ossProperties.getPreviewUrl() + key;
            } else {
                return getPrivateUrl(ossProperties.getPreviewUrl(), key, ossProperties.getExpires());
            }
        }
        return getPrivateUrl(key, ossProperties.getExpires() == -1 ? 300 : ossProperties.getExpires());
    }


    /**
     * 通过OSS直接查看文件预览路径
     * 获取私有链接
     *
     * @param key     oss key
     * @param expires 私有链接有效秒数
     * @return 文件阅览路径
     */
    @Override
    public String getPrivateUrl(String key, Integer expires) {
        return ossTemplate.getObjectURL(defaultBucketName(), key, expires, TimeUnit.SECONDS);
    }

    /**
     * 使用预览域名，通过OSS直接查看文件预览路径
     * 获取私有链接
     *
     * @param url     域名前缀
     * @param key     oss key
     * @param expires 私有链接有效秒数
     * @return 文件阅览路径
     */
    // @Override
    public String getPrivateUrl(String url, String key, Integer expires) {
        String objectURL = ossTemplate.getObjectURL(defaultBucketName(), key, expires, TimeUnit.SECONDS);
        if (StringUtils.isNotBlank(url)) {

            return objectURL.replace(ossProperties.getEndpoint() + "/" + defaultBucketName() + "/", url);
        }
        return objectURL;
    }

    /**
     * 文件内网阅览路径
     *
     * @param key oss key
     * @return 文件阅览路径
     */
    @Override
    public String getPreviewIntranetUrl(String key) {
        if (StringUtils.isNotBlank(ossProperties.getIntranetUrl())) {
            if (ossProperties.getExpires() == -1) {
                return ossProperties.getIntranetUrl() + key;
            } else {
                return getPrivateUrl(ossProperties.getIntranetUrl(), key, ossProperties.getExpires());
            }
        }
        return getPreviewUrl(key);
    }

    /**
     * 删除文件
     *
     * @param key 文件可以
     */
    @Override
    public void delete(String key) {
        ossTemplate.removeObject(defaultBucketName(), key);
    }

    /**
     * 生成文件路径
     *
     * @param uploadFileVO
     * @return 生成oss key
     */
    protected String generateFilePath(UploadFileVO uploadFileVO) {

        if (StringUtils.isNotBlank(uploadFileVO.getKey())) {
            return uploadFileVO.getKey();
        }
        // 后缀
        String postfix = null;
        if (StringUtils.isNotBlank(uploadFileVO.getOriginalFilename()) && StringUtils.contains(uploadFileVO.getOriginalFilename(), ".")) {

            postfix = StringUtils.substringAfterLast(uploadFileVO.getOriginalFilename(), ".");
        }

        StringBuilder filePathBuilder = new StringBuilder();

        // 文件前缀
        if (StringUtils.isNotBlank(uploadFileVO.getPrefix())) {
            filePathBuilder.append(uploadFileVO.getPrefix());
            if (!StringUtils.endsWith(uploadFileVO.getPrefix(), "/")) {
                filePathBuilder.append('/');
            }
        }

        filePathBuilder.append(uploadFileVO.getCreateTime().format(FILEPATH_DATETIME_FORMATTER))
                .append('/').append(uploadFileVO.getMd5());
        if (postfix != null) {
            filePathBuilder.append(".").append(postfix);
        }
        return filePathBuilder.toString();
    }


    /**
     * @return 默认的桶名称
     */
    protected String defaultBucketName() {
        return ossProperties.getBucket();
    }

    /**
     * 上传前钩子
     *
     * @param uploadFileVO 上传文件信息
     * @return true 可以上传 false 不用上传（比如根据md5/path 检查文件已存在）
     */
    protected UploadFileVO beforeUpload(UploadFileVO uploadFileVO) {

        return null;
    }

    /**
     * 上传完成钩子，可以保存上传记录等
     *
     * @param uploadFileVO
     * @param putObjectResult
     */
    protected void afterUpload(UploadFileVO uploadFileVO, PutObjectResult putObjectResult) {
    }
}
