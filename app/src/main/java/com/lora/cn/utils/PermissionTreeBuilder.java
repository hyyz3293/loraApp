package com.lora.cn.utils;

import com.lora.cn.database.entity.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissionTreeBuilder {

    /**
     * 将权限列表转换为完整的树形结构
     * @param permissions 权限列表
     * @return 完整的树形结构权限列表（包含所有层级）
     */
    public static List<Permission> buildCompletePermissionTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 创建ID到权限对象的映射
        Map<Long, Permission> permissionMap = new HashMap<>();
        for (Permission permission : permissions) {
            permissionMap.put(permission.getPermissionId(), permission);
        }

        // 构建树形结构
        List<Permission> rootPermissions = new ArrayList<>();

        for (Permission permission : permissions) {
            Long parentId = permission.getParentId();

            if (parentId == null) {
                // 这是顶级节点
                rootPermissions.add(permission);
            } else {
                // 找到父节点并添加到其子节点列表
                Permission parent = permissionMap.get(parentId);
                if (parent != null) {
                    if (parent.getChild() == null) {
                        parent.setChild(new ArrayList<>());
                    }
                    parent.getChild().add(permission);
                }
            }
        }

        // 对树进行排序
        sortPermissionTree(rootPermissions);

        return rootPermissions;
    }

    /**
     * 对树形结构的权限列表进行递归排序
     * @param permissions 权限树
     */
    public static void sortPermissionTree(List<Permission> permissions) {
        if (permissions == null) return;

        // 按sortOrder排序当前层级
        permissions.sort((p1, p2) -> Integer.compare(p1.getSortOrder(), p2.getSortOrder()));

        // 递归排序子节点
        for (Permission permission : permissions) {
            if (permission.getChild() != null && !permission.getChild().isEmpty()) {
                sortPermissionTree(permission.getChild());
            }
        }
    }

    /**
     * 打印树形结构（用于调试）
     * @param permissions 权限树
     * @param depth 当前深度（用于缩进）
     */
    public static void printPermissionTree(List<Permission> permissions, int depth) {
        if (permissions == null) return;

        String indent = "  ".repeat(depth);
        for (Permission permission : permissions) {
            System.out.println(indent + "└─ " + permission.getPermissionName() +
                    " (ID: " + permission.getPermissionId() +
                    ", Level: " + permission.getLevel() +
                    ", Order: " + permission.getSortOrder() + ")");

            if (permission.getChild() != null && !permission.getChild().isEmpty()) {
                printPermissionTree(permission.getChild(), depth + 1);
            }
        }
    }

    /**
     * 获取所有权限的扁平列表（从树形结构转换回来）
     * @param tree 权限树
     * @return 扁平权限列表
     */
    public static List<Permission> flattenPermissionTree(List<Permission> tree) {
        List<Permission> result = new ArrayList<>();
        if (tree == null) return result;

        for (Permission permission : tree) {
            result.add(permission);
            if (permission.getChild() != null && !permission.getChild().isEmpty()) {
                result.addAll(flattenPermissionTree(permission.getChild()));
            }
            // 清除子节点引用，避免循环引用问题
            permission.setChild(null);
        }

        return result;
    }
}