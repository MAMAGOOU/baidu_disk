package com.rocket.pan.server.modules.file.controller;

import com.google.common.base.Splitter;
import com.rocket.pan.core.constants.RPanConstants;
import com.rocket.pan.core.response.R;
import com.rocket.pan.core.utils.SpringContextUtil;
import com.rocket.pan.server.common.utils.UserIdUtil;
import com.rocket.pan.server.modules.file.constants.FileConstants;
import com.rocket.pan.server.modules.file.context.*;
import com.rocket.pan.server.modules.file.converter.FileConverter;
import com.rocket.pan.server.modules.file.enums.DelFlagEnum;
import com.rocket.pan.server.modules.file.po.*;
import com.rocket.pan.server.modules.file.service.IUserFileService;
import com.rocket.pan.server.modules.file.vo.*;
import com.rocket.pan.core.utils.IdUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文件模块控制器
 *
 * @author 19750
 * @version 1.0
 */
@RestController
@Validated
@Api(tags = "文件模块")
public class FileController {

    @Autowired
    private IUserFileService iUserFileService;

    @Autowired
    private FileConverter fileConverter;


    @ApiOperation(
            value = "查看文件列表",
            notes = "改接口提供了用户查询某文件夹下面某些文件类型的文件列表的功能",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @GetMapping("/files")
    public R<List<RPanUserFileVO>> list(@NotBlank(message = "父文件夹ID不可以为空") @RequestParam(value = "parentId", required = false) String parentId,
                                        @RequestParam(value = "fileTypes", required = false,
                                                defaultValue = FileConstants.ALL_FILE_TYPE) String fileTypes) {
        Long realParentId = -1L;
        // 对父文件Id进行解密
        if (!FileConstants.ALL_FILE_TYPE.equals(parentId)) {
            realParentId = IdUtil.decrypt(parentId);
        }
        // 文件类型数组
        List<Integer> fileTypeArray = null;
        // 将文件类型数组字符串转为文件类型集合
        if (!Objects.equals(FileConstants.ALL_FILE_TYPE, fileTypes)) {
            fileTypeArray = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileTypes)
                    .stream().map(Integer::valueOf).collect(Collectors.toList());
        }
        // 查询文件列表上下文
        QueryFileListContext context = new QueryFileListContext();
        context.setParentId(realParentId);
        context.setFileTypeArray(fileTypeArray);
        // UserIdUtil，用户id存储工具类，使用ThreadLocal进行保存
        context.setUserId(UserIdUtil.get());
        // DelFlagEnum，文件是否删除枚举类，0 未删除 1 已删除
        context.setDelFlag(DelFlagEnum.NO.getCode());

        // 进行文件列表条件查询
        //IUserFileService userFileService = SpringContextUtil.getBean(IUserFileService.class);
        List<RPanUserFileVO> result = iUserFileService.getFileList(context);
        System.out.println();
        //List<RPanUserFileVO> result = userFileService.getFileList(context);
        return R.data(result);
    }

    @ApiOperation(
            value = "创建文件夹",
            notes = "该接口提供了创建文件夹的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("/file/folder")
    public R<String> createFolder(@Validated @RequestBody CreateFolderPO createFolderPO) {
        CreateFolderContext context = fileConverter.createFolderPO2CreateFolderContext(createFolderPO);
        Long fileId = iUserFileService.createFolder(context);
        return R.data(IdUtil.encrypt(fileId));
    }

    @ApiOperation(
            value = "文件重命名",
            notes = "该接口提供了文件重命名的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PutMapping("/file")
    public R updateFilename(@Validated @RequestBody UpdateFilenamePO updateFilenamePO) {
        UpdateFilenameContext context = fileConverter.updateFilenamePO2UpdateFilenameContext(updateFilenamePO);
        iUserFileService.updateFilename(context);
        return R.success();
    }

    @ApiOperation(
            value = "批量删除文件",
            notes = "该接口提供了批量删除文件的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @DeleteMapping("/file")
    public R deleteFile(@Validated @RequestBody DeleteFilePO deleteFilePO) {
        DeleteFileContext context = fileConverter.deleteFilePO2DeleteFileContext(deleteFilePO);

        String fileIds = deleteFilePO.getFileIds();
        List<Long> fileIdList = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileIds)
                .stream().map(IdUtil::decrypt).collect(Collectors.toList());

        context.setFileIdList(fileIdList);
        iUserFileService.deleteFile(context);
        return R.success();
    }

    @ApiOperation(
            value = "文件秒传",
            notes = "该接口提供了文件秒传的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("/file/sec-upload")
    public R secUpload(@Validated @RequestBody SecUploadFilePO secUploadFilePO) {
        SecUploadFileContext context = fileConverter.secUploadFilePO2SecUploadFileContext(secUploadFilePO);
        boolean result = iUserFileService.secUpload(context);
        if (result) {
            return R.success();
        }
        return R.fail("文件唯一标识不存在，请手动执行文件上传");
    }

    @ApiOperation(
            value = "单文件上传",
            notes = "该接口提供了单文件上传的功能",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("/file/upload")
    public R upload(@Validated FileUploadPO fileUploadPO) {
        FileUploadContext context = fileConverter.fileUploadPO2FileUploadContext(fileUploadPO);
        iUserFileService.upload(context);
        return R.success();
    }

    @ApiOperation(
            value = "文件分片上传",
            notes = "该接口提供了文件分片上传的功能",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("/file/chunk-upload")
    public R<FileChunkUploadVO> chunkUpload(@Validated FileChunkUploadPO fileChunkUploadPO) {
        FileChunkUploadContext context = fileConverter.fileChunkUploadPO2FileChunkUploadContext(fileChunkUploadPO);
        FileChunkUploadVO vo = iUserFileService.chunkUpload(context);
        return R.data(vo);
    }

    @ApiOperation(
            value = "查询已经上传的文件分片列表",
            notes = "该接口提供了查询已经上传的文件分片列表的功能",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @GetMapping("/file/chunk-upload")
    public R<UploadedChunksVO> getUploadedChunks(@Validated QueryUploadedChunksPO queryUploadedChunksPO) {
        QueryUploadedChunksContext context = fileConverter.queryUploadedChunksPO2QueryUploadedChunksContext(queryUploadedChunksPO);
        UploadedChunksVO vo = iUserFileService.getUploadedChunks(context);
        return R.data(vo);
    }

    @ApiOperation(
            value = "文件分片合并",
            notes = "该接口提供了文件分片合并的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("/file/merge")
    public R mergeFile(@Validated @RequestBody FileChunkMergePO fileChunkMergePO) {
        FileChunkMergeContext context = fileConverter.fileChunkMergePO2FileChunkMergeContext(fileChunkMergePO);
        iUserFileService.mergeFile(context);
        return R.success();
    }

    @ApiOperation(
            value = "文件下载",
            notes = "该接口提供了文件下载的功能",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @GetMapping("/file/download")
    public void download(@NotBlank(message = "文件ID不能为空") @RequestParam(value = "fileId", required = false) String fileId,
                         HttpServletResponse response) {
        FileDownloadContext context = new FileDownloadContext();

        context.setFileId(IdUtil.decrypt(fileId));
        context.setResponse(response);
        context.setUserId(UserIdUtil.get());
        iUserFileService.download(context);
    }

    @ApiOperation(
            value = "文件预览",
            notes = "该接口提供了文件预览的功能",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @GetMapping("/file/preview")
    public void preview(@NotBlank(message = "文件ID不能为空") @RequestParam(value = "fileId", required = false) String fileId,
                        HttpServletResponse response) {
        FilePreviewContext context = new FilePreviewContext();
        context.setFileId(IdUtil.decrypt(fileId));
        context.setResponse(response);
        context.setUserId(UserIdUtil.get());
        iUserFileService.preview(context);
    }

    @ApiOperation(
            value = "查询文件夹树",
            notes = "该接口提供了查询文件夹树的功能",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @GetMapping("/file/folder/tree")
    public R<List<FolderTreeNodeVO>> getFolderTree() {
        QueryFolderTreeContext context = new QueryFolderTreeContext();
        context.setUserId(UserIdUtil.get());
        List<FolderTreeNodeVO> result = iUserFileService.getFolderTree(context);
        return R.data(result);
    }

    @ApiOperation(
            value = "文件转移",
            notes = "该接口提供了文件转移的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("/file/transfer")
    public R transfer(@Validated @RequestBody TransferFilePO transferFilePO) {
        String fileIds = transferFilePO.getFileIds();
        String targetParentId = transferFilePO.getTargetParentId();
        List<Long> fileIdList = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileIds).stream().map(IdUtil::decrypt).collect(Collectors.toList());

        TransferFileContext context = new TransferFileContext();
        context.setFileIdList(fileIdList);
        context.setTargetParentId(IdUtil.decrypt(targetParentId));
        context.setUserId(UserIdUtil.get());

        iUserFileService.transfer(context);

        return R.success();
    }

    @ApiOperation(
            value = "文件复制",
            notes = "该接口提供了文件复制的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("/file/copy")
    public R copy(@Validated @RequestBody CopyFilePO copyFilePO) {
        String fileIds = copyFilePO.getFileIds();
        String targetParentId = copyFilePO.getTargetParentId();
        List<Long> fileIdList = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileIds).stream().map(IdUtil::decrypt).collect(Collectors.toList());
        CopyFileContext context = new CopyFileContext();
        context.setFileIdList(fileIdList);
        context.setTargetParentId(IdUtil.decrypt(targetParentId));
        context.setUserId(UserIdUtil.get());
        iUserFileService.copy(context);
        return R.success();
    }

    @ApiOperation(
            value = "文件搜索",
            notes = "该接口提供了文件搜索的功能",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @GetMapping("/file/search")
    public R<List<FileSearchResultVO>> search(@Validated FileSearchPO fileSearchPO) {
        FileSearchContext context = new FileSearchContext();
        context.setKeyword(fileSearchPO.getKeyword());
        context.setUserId(UserIdUtil.get());
        String fileTypes = fileSearchPO.getFileTypes();
        if (StringUtils.isNotBlank(fileTypes) && !Objects.equals(FileConstants.ALL_FILE_TYPE, fileTypes)) {
            List<Integer> fileTypeArray = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileTypes).stream().map(Integer::valueOf).collect(Collectors.toList());
            context.setFileTypeArray(fileTypeArray);
        }
        List<FileSearchResultVO> result = iUserFileService.search(context);
        return R.data(result);
    }

    @ApiOperation(
            value = "查询面包屑列表",
            notes = "该接口提供了查询面包屑列表的功能",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @GetMapping("/file/breadcrumbs")
    public R<List<BreadcrumbVO>> getBreadcrumbs(@NotBlank(message = "文件ID不能为空") @RequestParam(value = "fileId", required = false) String fileId) {
        QueryBreadcrumbsContext context = new QueryBreadcrumbsContext();
        context.setFileId(IdUtil.decrypt(fileId));
        context.setUserId(UserIdUtil.get());
        List<BreadcrumbVO> result = iUserFileService.getBreadcrumbs(context);
        return R.data(result);
    }

}
