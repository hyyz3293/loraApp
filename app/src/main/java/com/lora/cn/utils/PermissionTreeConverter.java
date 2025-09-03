package com.lora.cn.utils;

import com.lora.cn.database.entity.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionTreeConverter {

    public static List<Permission> convertToTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Create Map to store nodes and result list
        Map<Long, Permission> nodeMap = new HashMap<>();
        List<Permission> rootNodes = new ArrayList<>();
        
        // First pass: create all nodes and store in Map
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
            node.setExpand(permission.isExpand());
            node.setParent(permission.isParent());
            node.setSelect(permission.isSelect());
            node.setChildList(new ArrayList<>());
            
            // 设置parentId（包括null的情况）
            node.setParentId(permission.getParentId());

            nodeMap.put(permission.getPermissionId(), node);
        }

        // 第二遍遍历：建立父子关系
        for (Permission permission : permissions) {
            Permission currentNode = nodeMap.get(permission.getPermissionId());
            Long parentId = permission.getParentId();

            // Check if it's a root node (parentId is null or 0)
            if (parentId == null || parentId == 0) {
                // Root node, add to root node list
                rootNodes.add(currentNode);
            } else {
                // Child node, find parent node and add to parent's children list
                Permission parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    parentNode.getChildList().add(currentNode);
                    // Ensure parent node's isParent is set to true
                    parentNode.setParent(true);
                } else {
                    // If parent node not found, treat as root node
                    rootNodes.add(currentNode);
                }
            }
        }

        // Sort entire tree by sortOrder
        sortNodes(rootNodes);

        return rootNodes;
    }

    private static void sortNodes(List<Permission> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 按sortOrder排序当前层级
        nodes.sort((n1, n2) -> Integer.compare(n1.getSortOrder(), n2.getSortOrder()));

        // Recursively sort child nodes
        for (Permission node : nodes) {
            if (node.getChildList() != null && !node.getChildList().isEmpty()) {
                sortNodes(node.getChildList());
            }
        }
    }


}