import java.util.*;

// Test to verify convertToTree returns complete tree structure
public class TestTreeStructure {
    
    // Copy of Permission class for testing
    static class Permission {
        private long permissionId;
        private String permissionCode;
        private String permissionName;
        private String category;
        private String description;
        private int status;
        private Long parentId;
        private int level;
        private int sortOrder;
        private Date createTime;
        private Date updateTime;
        
        public boolean isParent = false;
        public boolean isSelect = false;
        public boolean isExpand = false;
        
        private List<Permission> childList;
        
        public Permission() {
            this.childList = new ArrayList<>();
        }
        
        public Permission(long permissionId, String permissionCode, String permissionName, 
                        String category, String description, int status, Long parentId, 
                        int level, int sortOrder) {
            this.permissionId = permissionId;
            this.permissionCode = permissionCode;
            this.permissionName = permissionName;
            this.category = category;
            this.description = description;
            this.status = status;
            this.parentId = parentId;
            this.level = level;
            this.sortOrder = sortOrder;
            this.childList = new ArrayList<>();
        }
        
        // Getters and setters
        public long getPermissionId() { return permissionId; }
        public void setPermissionId(long permissionId) { this.permissionId = permissionId; }
        public String getPermissionCode() { return permissionCode; }
        public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
        public String getPermissionName() { return permissionName; }
        public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        public Date getUpdateTime() { return updateTime; }
        public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
        
        public List<Permission> getChild() { return childList; }
        public void setChild(List<Permission> child) { this.childList = child; }
        public List<Permission> getChildList() { return childList; }
        public void setChildList(List<Permission> childList) { this.childList = childList; }
        
        public boolean isParent() { return isParent; }
        public void setParent(boolean parent) { isParent = parent; }
        public boolean isSelect() { return isSelect; }
        public void setSelect(boolean select) { isSelect = select; }
        public boolean isExpand() { return isExpand; }
        public void setExpand(boolean expand) { isExpand = expand; }
    }
    
    // Copy of PermissionTreeConverter for testing
    static class PermissionTreeConverter {
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
                
                // Set parentId (including null cases)
                node.setParentId(permission.getParentId());

                nodeMap.put(permission.getPermissionId(), node);
            }

            // Second pass: establish parent-child relationships
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

            // Sort current level by sortOrder
            nodes.sort((n1, n2) -> Integer.compare(n1.getSortOrder(), n2.getSortOrder()));

            // Recursively sort child nodes
            for (Permission node : nodes) {
                if (node.getChildList() != null && !node.getChildList().isEmpty()) {
                    sortNodes(node.getChildList());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Testing PermissionTreeConverter.convertToTree() ===");
        
        // Create comprehensive test data
        List<Permission> allPermissions = Arrays.asList(
            // Level 1 - Root permissions
            new Permission(1L, "SYSTEM", "System Management", "SYSTEM", "System management permissions", 1, null, 1, 1),
            new Permission(9L, "BUSINESS", "Business Management", "BUSINESS", "Business management permissions", 1, null, 1, 2),
            new Permission(15L, "REPORT", "Report Management", "REPORT", "Report management permissions", 1, null, 1, 3),
            
            // Level 2 - System children
            new Permission(2L, "USER_MGMT", "User Management", "SYSTEM", "User management", 1, 1L, 2, 1),
            new Permission(3L, "ROLE_MGMT", "Role Management", "SYSTEM", "Role management", 1, 1L, 2, 2),
            new Permission(8L, "SETTING_MGMT", "Setting Management", "SYSTEM", "Setting management", 1, 1L, 2, 3),
            
            // Level 3 - User Management children
            new Permission(4L, "USER_ADD", "Add User", "SYSTEM", "Add new user", 1, 2L, 3, 1),
            new Permission(5L, "USER_EDIT", "Edit User", "SYSTEM", "Edit user information", 1, 2L, 3, 2),
            new Permission(6L, "USER_DELETE", "Delete User", "SYSTEM", "Delete user", 1, 2L, 3, 3),
            new Permission(7L, "USER_VIEW", "View User", "SYSTEM", "View user details", 1, 2L, 3, 4),
            
            // Level 3 - Role Management children
            new Permission(10L, "ROLE_ADD", "Add Role", "SYSTEM", "Add new role", 1, 3L, 3, 1),
            new Permission(11L, "ROLE_EDIT", "Edit Role", "SYSTEM", "Edit role information", 1, 3L, 3, 2),
            new Permission(12L, "ROLE_DELETE", "Delete Role", "SYSTEM", "Delete role", 1, 3L, 3, 3),
            
            // Level 2 - Business children
            new Permission(13L, "PRODUCT_MGMT", "Product Management", "BUSINESS", "Product management", 1, 9L, 2, 1),
            new Permission(14L, "ORDER_MGMT", "Order Management", "BUSINESS", "Order management", 1, 9L, 2, 2),
            
            // Level 3 - Product Management children
            new Permission(16L, "PRODUCT_ADD", "Add Product", "BUSINESS", "Add new product", 1, 13L, 3, 1),
            new Permission(17L, "PRODUCT_EDIT", "Edit Product", "BUSINESS", "Edit product information", 1, 13L, 3, 2),
            
            // Level 2 - Report children
            new Permission(18L, "SALES_REPORT", "Sales Report", "REPORT", "Sales report management", 1, 15L, 2, 1)
        );
        
        System.out.println("\n1. Input flat permissions (" + allPermissions.size() + " total):");
        for (Permission p : allPermissions) {
            System.out.println(String.format("  ID: %2d, Name: %-20s, ParentID: %s, Level: %d, Sort: %d", 
                p.getPermissionId(), p.getPermissionName(), 
                p.getParentId() == null ? "null" : p.getParentId().toString(), 
                p.getLevel(), p.getSortOrder()));
        }
        
        // Convert to tree
        System.out.println("\n2. Converting to tree structure...");
        List<Permission> permissionTree = PermissionTreeConverter.convertToTree(allPermissions);
        
        System.out.println("\n3. Tree structure result (" + permissionTree.size() + " root nodes):");
        printTreeStructure(permissionTree, 0);
        
        // Detailed verification
        System.out.println("\n4. Detailed childList verification:");
        verifyTreeStructure(permissionTree, "");
        
        // Summary statistics
        System.out.println("\n5. Tree Statistics:");
        TreeStats stats = calculateTreeStats(permissionTree);
        System.out.println("  - Root nodes: " + stats.rootCount);
        System.out.println("  - Total nodes: " + stats.totalNodes);
        System.out.println("  - Nodes with children: " + stats.nodesWithChildren);
        System.out.println("  - Leaf nodes: " + stats.leafNodes);
        System.out.println("  - Max depth: " + stats.maxDepth);
        
        // Verify completeness
        System.out.println("\n6. Completeness Check:");
        boolean isComplete = verifyCompleteness(allPermissions, permissionTree);
        System.out.println("  - All permissions included: " + isComplete);
        
        System.out.println("\n=== Test Result ===");
        if (isComplete && stats.totalNodes == allPermissions.size()) {
            System.out.println("SUCCESS: convertToTree() returns complete tree structure!");
            System.out.println("All " + allPermissions.size() + " permissions are properly organized in tree format.");
            System.out.println("Each parent node contains complete childList with all its children.");
        } else {
            System.out.println("FAILURE: Tree structure is incomplete or incorrect.");
        }
    }
    
    private static void printTreeStructure(List<Permission> permissions, int level) {
        String indent = "  ".repeat(level);
        for (Permission permission : permissions) {
            System.out.println(String.format("%s├─ %s (ID: %d, Children: %d)", 
                indent, permission.getPermissionName(), permission.getPermissionId(),
                permission.getChildList() != null ? permission.getChildList().size() : 0));
            
            if (permission.getChildList() != null && !permission.getChildList().isEmpty()) {
                printTreeStructure(permission.getChildList(), level + 1);
            }
        }
    }
    
    private static void verifyTreeStructure(List<Permission> permissions, String indent) {
        for (Permission permission : permissions) {
            List<Permission> children = permission.getChildList();
            System.out.println(String.format("%s%s: childList size = %d, isParent = %s", 
                indent, permission.getPermissionName(), 
                children != null ? children.size() : 0,
                permission.isParent()));
            
            if (children != null && !children.isEmpty()) {
                for (int i = 0; i < children.size(); i++) {
                    Permission child = children.get(i);
                    System.out.println(String.format("%s  [%d] %s (ID: %d, ParentID: %s)", 
                        indent, i, child.getPermissionName(), child.getPermissionId(),
                        child.getParentId()));
                }
                verifyTreeStructure(children, indent + "  ");
            }
        }
    }
    
    private static TreeStats calculateTreeStats(List<Permission> permissions) {
        TreeStats stats = new TreeStats();
        calculateStatsRecursive(permissions, stats, 1);
        stats.rootCount = permissions.size();
        return stats;
    }
    
    private static void calculateStatsRecursive(List<Permission> permissions, TreeStats stats, int depth) {
        stats.maxDepth = Math.max(stats.maxDepth, depth);
        
        for (Permission permission : permissions) {
            stats.totalNodes++;
            
            List<Permission> children = permission.getChildList();
            if (children != null && !children.isEmpty()) {
                stats.nodesWithChildren++;
                calculateStatsRecursive(children, stats, depth + 1);
            } else {
                stats.leafNodes++;
            }
        }
    }
    
    private static boolean verifyCompleteness(List<Permission> original, List<Permission> tree) {
        Set<Long> originalIds = new HashSet<>();
        for (Permission p : original) {
            originalIds.add(p.getPermissionId());
        }
        
        Set<Long> treeIds = new HashSet<>();
        collectAllIds(tree, treeIds);
        
        return originalIds.equals(treeIds);
    }
    
    private static void collectAllIds(List<Permission> permissions, Set<Long> ids) {
        for (Permission permission : permissions) {
            ids.add(permission.getPermissionId());
            if (permission.getChildList() != null && !permission.getChildList().isEmpty()) {
                collectAllIds(permission.getChildList(), ids);
            }
        }
    }
    
    static class TreeStats {
        int rootCount = 0;
        int totalNodes = 0;
        int nodesWithChildren = 0;
        int leafNodes = 0;
        int maxDepth = 0;
    }
}