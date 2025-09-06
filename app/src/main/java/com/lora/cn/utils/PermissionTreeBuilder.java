package com.lora.cn.utils;

import com.lora.cn.database.entity.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限列表转树形结构工具类
 */
public class PermissionTreeBuilder {

    /**
     * 将扁平的Permission列表转换为树形结构（返回顶层节点列表）
     * @param flatPermissions 扁平的权限列表（包含所有层级的Permission）
     * @return 树形结构的顶层节点列表（每个节点的childList包含其子节点）
     */
    public static List<Permission> buildTree(List<Permission> flatPermissions) {
        // 1. 校验输入：避免空指针
        if (flatPermissions == null || flatPermissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 筛选顶层节点（level=0，无父节点）
        List<Permission> topLevelPermissions = flatPermissions.stream()
                .filter(permission -> permission.getLevel() == 0) // 顶层节点level固定为0
                .collect(Collectors.toList());

        // 3. 为每个顶层节点递归挂载子节点
        for (Permission topNode : topLevelPermissions) {
            // 标记顶层节点为"父节点"（isParent=true）
            topNode.setParent(true);
            // 递归获取当前节点的所有子节点
            List<Permission> children = findChildren(topNode, flatPermissions);
            // 设置子节点集合
            topNode.setChildList(children);
        }

        return topLevelPermissions;
    }

    /**
     * 递归查找指定父节点的所有子节点
     * @param parentNode 父节点（需挂载子节点的节点）
     * @param allPermissions 所有权限列表（用于匹配子节点）
     * @return 父节点的直接子节点列表（每个子节点已递归挂载自身的子节点）
     */
    private static List<Permission> findChildren(Permission parentNode, List<Permission> allPermissions) {
        // 筛选当前父节点的直接子节点：子节点的parentId == 父节点的permissionId
        List<Permission> directChildren = allPermissions.stream()
                .filter(permission -> {
                    // 子节点必须有parentId，且与父节点ID匹配
                    Long childParentId = permission.getParentId();
                    return childParentId != null && childParentId.equals(parentNode.getPermissionId());
                })
                // 按sortOrder排序（保证子节点顺序与配置一致）
                .sorted((p1, p2) -> Integer.compare(p1.getSortOrder(), p2.getSortOrder()))
                .collect(Collectors.toList());

        // 递归为每个直接子节点挂载其下一级子节点
        for (Permission childNode : directChildren) {
            // 查找当前子节点的子节点（递归）
            List<Permission> grandChildren = findChildren(childNode, allPermissions);
            // 若当前子节点有子节点，标记为"父节点"
            if (!grandChildren.isEmpty()) {
                childNode.setParent(true);
                childNode.setChildList(grandChildren);
            } else {
                // 叶子节点（无子节点）标记为非父节点
                childNode.setParent(false);
                childNode.setChildList(new ArrayList<>()); // 避免null，统一空集合
            }
        }

        return directChildren;
    }
}