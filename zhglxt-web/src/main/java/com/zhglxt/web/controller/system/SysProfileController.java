package com.zhglxt.web.controller.system;

import com.zhglxt.common.annotation.Log;
import com.zhglxt.common.config.GlobalConfig;
import com.zhglxt.common.core.controller.BaseController;
import com.zhglxt.common.core.entity.AjaxResult;
import com.zhglxt.common.core.entity.sys.SysUser;
import com.zhglxt.common.enums.BusinessType;
import com.zhglxt.common.utils.DateUtils;
import com.zhglxt.common.utils.ShiroUtils;
import com.zhglxt.common.utils.StringUtils;
import com.zhglxt.common.utils.file.FileUploadUtils;
import com.zhglxt.common.utils.file.FileUtils;
import com.zhglxt.common.utils.file.MimeTypeUtils;
import com.zhglxt.framework.shiro.service.SysPasswordService;
import com.zhglxt.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人信息 业务处理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(SysProfileController.class);

    private String prefix = "system/user/profile";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPasswordService passwordService;

    /**
     * 个人信息
     */
    @GetMapping()
    public String profile(ModelMap mmap) {
        SysUser user = getSysUser();
        mmap.put("user", user);
        mmap.put("roleGroup", userService.selectUserRoleGroup(user.getUserId()));
        mmap.put("postGroup", userService.selectUserPostGroup(user.getUserId()));
        return prefix + "/profile";
    }

    @GetMapping("/checkPassword")
    @ResponseBody
    public boolean checkPassword(String password) {
        SysUser user = getSysUser();
        return passwordService.matches(user, password);
    }

    @GetMapping("/resetPwd")
    public String resetPwd(ModelMap mmap) {
        SysUser user = getSysUser();
        mmap.put("user", userService.selectUserById(user.getUserId()));
        return prefix + "/resetPwd";
    }

    @Log(title = "重置密码", businessType = BusinessType.UPDATE)
    @PostMapping("/resetPwd")
    @ResponseBody
    public AjaxResult resetPwd(String oldPassword, String newPassword) {
        SysUser user = getSysUser();

        //超级管理员用户&角色不允许操作
        userService.checkUserAllowed(user);

        if (!passwordService.matches(user, oldPassword)) {
            return error("修改密码失败，旧密码错误");
        }
        if (passwordService.matches(user, newPassword)) {
            return error("新密码不能与旧密码相同");
        }
        user.setSalt(ShiroUtils.randomSalt());
        user.setPassword(passwordService.encryptPassword(user.getLoginName(), newPassword, user.getSalt()));
        user.setPwdUpdateDate(DateUtils.getNowDate());
        if (userService.resetUserPwd(user) > 0) {
            ShiroUtils.setSysUser(userService.selectUserById(user.getUserId()));
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 修改用户
     */
    @GetMapping("/edit")
    public String edit(ModelMap mmap) {
        SysUser user = getSysUser();
        mmap.put("user", userService.selectUserById(user.getUserId()));
        return prefix + "/edit";
    }

    /**
     * 修改头像
     */
    @GetMapping("/avatar")
    public String avatar(ModelMap mmap) {
        SysUser user = getSysUser();
        mmap.put("user", userService.selectUserById(user.getUserId()));
        return prefix + "/avatar";
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    @ResponseBody
    public AjaxResult update(SysUser user) {
        SysUser currentUser = getSysUser();
        currentUser.setUserName(user.getUserName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        // 个人信息添加手机&邮箱重复验证
       /* if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + currentUser.getLoginName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + currentUser.getLoginName() + "'失败，邮箱账号已存在");
        }*/

        if (userService.updateUserInfo(currentUser) > 0) {
            setSysUser(userService.selectUserById(currentUser.getUserId()));
            return success();
        }
        return error();
    }

    /**
     * 保存头像
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PostMapping("/updateAvatar")
    @ResponseBody
    public AjaxResult updateAvatar(@RequestParam("avatarfile") MultipartFile file) {
        try {
            if (!file.isEmpty()) {
                SysUser currentUser = getSysUser();
                String avatar = FileUploadUtils.upload(GlobalConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION, true);
                if (userService.updateUserAvatar(currentUser.getUserId(), avatar))
                {
                    String oldAvatar = currentUser.getAvatar();
                    if (StringUtils.isNotEmpty(oldAvatar))
                    {
                        FileUtils.deleteFile(GlobalConfig.getProfile() + FileUtils.stripPrefix(oldAvatar));
                    }
                    currentUser.setAvatar(avatar);
                    setSysUser(currentUser);
                    return success();
                }
            }
            return error();
        } catch (Exception e) {
            log.error("修改头像失败！", e);
            return error(e.getMessage());
        }
    }
}
