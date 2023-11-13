package com.rocket.pan.server.modules.file.controller;

import com.rocket.pan.response.R;
import com.rocket.pan.server.modules.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * 文件模块控制器
 *
 * @author 19750
 * @version 1.0
 */
@RestController
public class FileController {
    @Autowired
    private IUserService iUserService;

    @GetMapping("/files")
    private R list(@NotBlank(message = "父文件夹ID不可以为空") @RequestParam(value = "parentId",required = false) ) {

    }
}
