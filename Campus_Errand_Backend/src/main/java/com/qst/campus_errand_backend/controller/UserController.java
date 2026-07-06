package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.User;
import com.qst.campus_errand_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户注册、登录、信息管理相关接口")
public class UserController {

    private static final String UPLOAD_DIR = "C:/images";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    @Autowired
    private UserService userService;

    // =================== 登录/注册 ===================

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String studentNo = body.get("studentNo");
        String password = body.get("password");
        if (studentNo == null || password == null) {
            result.put("code", 400);
            result.put("message", "学号和密码不能为空");
            return result;
        }
        User user = userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentNo, studentNo));
        if (user == null || user.getStatus() == 0) {
            result.put("code", 401);
            result.put("message", "用户不存在或已禁用");
            return result;
        }
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!md5Password.equals(user.getPassword())) {
            result.put("code", 401);
            result.put("message", "密码错误");
            return result;
        }
        // 生成简易Token（生产环境应使用JWT）
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setPassword(null);
        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("data", Map.of("token", token, "user", user));
        return result;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String studentNo = body.get("studentNo");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", studentNo);
        String phone = body.get("phone");

        if (studentNo == null || password == null) {
            result.put("code", 400);
            result.put("message", "学号和密码不能为空");
            return result;
        }
        User exist = userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentNo, studentNo));
        if (exist != null) {
            result.put("code", 409);
            result.put("message", "该学号已注册");
            return result;
        }
        User user = new User();
        user.setStudentNo(studentNo);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8)));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setRole(0);
        user.setStatus(1);
        userService.save(user);
        result.put("code", 200);
        result.put("message", "注册成功");
        return result;
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "修改密码")
    public Map<String, Object> changePassword(@PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        User user = userService.getById(id);
        if (user == null) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }
        String oldMd5 = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!oldMd5.equals(user.getPassword())) {
            result.put("code", 401);
            result.put("message", "原密码错误");
            return result;
        }
        User update = new User();
        update.setId(id);
        update.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8)));
        userService.updateById(update);
        result.put("code", 200);
        result.put("message", "密码修改成功");
        return result;
    }

    @PutMapping("/{id}/balance")
    @Operation(summary = "充值余额")
    public Map<String, Object> recharge(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Object amountObj = body.get("amount");
        if (amountObj == null) {
            result.put("code", 400);
            result.put("message", "请输入充值金额");
            return result;
        }
        double amount = Double.parseDouble(amountObj.toString());
        User user = userService.getById(id);
        if (user == null) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .setSql("balance = balance + " + amount));
        result.put("code", 200);
        result.put("message", "充值成功");
        result.put("data", Map.of("balance", user.getBalance().doubleValue() + amount));
        return result;
    }

    // =================== CRUD ===================

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户")
    public User getById(@PathVariable @Parameter(description = "用户ID") Long id) {
        return userService.getById(id);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表")
    public Page<User> list(@RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
                           @RequestParam(defaultValue = "10") @Parameter(description = "每页大小") int size,
                           @RequestParam(required = false) @Parameter(description = "学号搜索") String studentNo,
                           @RequestParam(required = false) @Parameter(description = "昵称搜索") String nickname,
                           @RequestParam(required = false) @Parameter(description = "状态: 0=禁用, 1=启用") Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (studentNo != null && !studentNo.isEmpty()) {
            wrapper.like(User::getStudentNo, studentNo);
        }
        if (nickname != null && !nickname.isEmpty()) {
            wrapper.like(User::getNickname, nickname);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userService.page(new Page<>(page, size), wrapper);
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public boolean save(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息")
    public boolean update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return userService.updateById(user);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public boolean updateStatus(@PathVariable Long id,
                                @RequestParam @Parameter(description = "0=禁用, 1=启用") Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return userService.updateById(user);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除用户")
    public boolean delete(@PathVariable Long id) {
        return userService.removeById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "按学号精确查询")
    public User getByStudentNo(@RequestParam @Parameter(description = "学号") String studentNo) {
        return userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentNo, studentNo));
    }

    @PostMapping("/{id}/avatar")
    @Operation(summary = "上传用户头像")
    public Map<String, Object> uploadAvatar(@PathVariable Long id,
                                             @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择文件");
                return result;
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("message", "仅支持图片格式");
                return result;
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                result.put("success", false);
                result.put("message", "文件大小不能超过2MB");
                return result;
            }
            String suffix = getSuffix(file.getOriginalFilename());
            String filename = saveFile(file.getBytes(), suffix);
            updateUserAvatar(id, filename, result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/{id}/avatar/base64")
    @Operation(summary = "上传用户头像（Base64方式）")
    public Map<String, Object> uploadAvatarBase64(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String base64Data = body.get("file");
        if (base64Data == null || base64Data.isEmpty()) {
            result.put("success", false);
            result.put("message", "请提供Base64图片数据，格式: { \"file\": \"data:image/png;base64,...\" }");
            return result;
        }
        try {
            // 解析 data:image/png;base64,xxxx 格式
            String prefix = "base64,";
            int prefixIndex = base64Data.indexOf(prefix);
            String pureBase64 = prefixIndex >= 0 ? base64Data.substring(prefixIndex + prefix.length()) : base64Data;

            byte[] bytes = Base64.getDecoder().decode(pureBase64);
            if (bytes.length > MAX_FILE_SIZE) {
                result.put("success", false);
                result.put("message", "文件大小不能超过2MB");
                return result;
            }
            // 从 data URL 中提取文件类型
            String suffix = ".png";
            if (prefixIndex >= 0) {
                String mimeType = base64Data.substring(5, base64Data.indexOf(';'));
                suffix = switch (mimeType) {
                    case "image/jpeg", "image/jpg" -> ".jpg";
                    case "image/gif" -> ".gif";
                    case "image/webp" -> ".webp";
                    default -> ".png";
                };
            }
            String filename = saveFile(bytes, suffix);
            updateUserAvatar(id, filename, result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    // -------- 私有辅助方法 --------

    private String getSuffix(String originalName) {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf("."));
        }
        return ".png";
    }

    private String saveFile(byte[] bytes, String suffix) throws IOException {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = dateStr + "_" + UUID.randomUUID().toString().substring(0, 8) + suffix;
        Path filePath = Paths.get(UPLOAD_DIR, filename);
        Files.write(filePath, bytes);
        return filename;
    }

    private void updateUserAvatar(Long userId, String filename, Map<String, Object> result) {
        String avatarUrl = "/images/" + filename;
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        userService.updateById(user);
        result.put("success", true);
        result.put("message", "上传成功");
        result.put("url", avatarUrl);
        result.put("filename", filename);
    }
}
