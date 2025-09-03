package com.lora.cn.utils;

import com.lora.cn.database.entity.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionTreeConverter {

    public static List<Permission> convertToTree(List<Permission> permissions) {
        // 创建存储节点的Map和结果列表
        Map<Long, Permission> nodeMap = new HashMap<>();
        List<Permission> rootNodes = new ArrayList<>();

        // 第一遍遍历：创建所有节点并存入Map
        for (Permission permission : permissions) {
            Permission node = new Permission();
            node.setPermissionId(permission.getPermissionId());
            node.setPermissionName(permission.getPermissionName());
            node.setPermissionCode(permission.getPermissionCode());
            node.setDescription(permission.getDescription());
            node.setCategory(permission.getCategory());
            node.setLevel(permission.getLevel());
            node.setSortOrder(permission.getSortOrder());
            node.setStatus(permission.getStatus());
//            node.setCreateTime(permission.getCreateTime());
//            node.setUpdateTime(permission.getUpdateTime());
            node.setExpand(permission.isExpand());
            node.setParent(permission.isParent());
            node.setSelect(permission.isSelect());
            node.setChild(new ArrayList<>());

            // 如果有parentId，设置parentId
            if (permission.getLevel() > 0) {
                node.setParentId(permission.getParentId());
            }

            nodeMap.put(permission.getPermissionId(), node);
        }

        // 第二遍遍历：建立父子关系
        for (Permission permission : permissions) {
            Permission currentNode = nodeMap.get(permission.getPermissionId());

            if (permission.getLevel() == 0) {
                // 顶级节点，添加到根节点列表
                rootNodes.add(currentNode);
            } else {
                // 子节点，找到父节点并添加到父节点的children列表
                Long parentId = permission.getParentId();
                Permission parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    parentNode.getChild().add(currentNode);
                    // 确保父节点的isParent设置为true
                    parentNode.setParent(true);
                }
            }
        }

        // 按sortOrder排序
        sortNodes(rootNodes);

        return rootNodes;
    }

    private static void sortNodes(List<Permission> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 按sortOrder排序当前层级
        nodes.sort((n1, n2) -> Integer.compare(n1.getSortOrder(), n2.getSortOrder()));

        // 递归排序子节点
        for (Permission node : nodes) {
            if (node.getChild() != null && !node.getChild().isEmpty()) {
                sortNodes(node.getChild());
            }
        }
    }


}