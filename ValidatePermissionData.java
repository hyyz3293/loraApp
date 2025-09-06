import java.util.*;

public class ValidatePermissionData {
    
    static class Permission {
        long permissionId;
        String permissionCode;
        String permissionName;
        Long parentId;
        int level;
        int sortOrder;
        
        public Permission(long permissionId, String permissionCode, String permissionName, 
                        Long parentId, int level, int sortOrder) {
            this.permissionId = permissionId;
            this.permissionCode = permissionCode;
            this.permissionName = permissionName;
            this.parentId = parentId;
            this.level = level;
            this.sortOrder = sortOrder;
        }
        
        @Override
        public String toString() {
            return String.format("ID=%d, Code=%s, Name=%s, ParentID=%s, Level=%d, Sort=%d", 
                    permissionId, permissionCode, permissionName, 
                    parentId == null ? "NULL" : parentId.toString(), level, sortOrder);
        }
    }
    
    public static void main(String[] args) {
        List<Permission> permissions = createPermissionData();
        
        System.out.println("=== Permission Data Validation Report ===");
        System.out.println();
        
        // 1. Check parent-child relationships
        validateParentChildRelationships(permissions);
        
        // 2. Check levels
        validateLevels(permissions);
        
        // 3. Display complete tree structure
        displayTreeStructure(permissions);
    }
    
    private static List<Permission> createPermissionData() {
        List<Permission> permissions = new ArrayList<>();
        
        // Simulate database permission data (in insertion order)
        // Level 0 - Top level permissions
        permissions.add(new Permission(1, "terminal_list", "Terminal List", null, 0, 1));
        permissions.add(new Permission(2, "log_info", "Log Info", null, 0, 2));
        permissions.add(new Permission(3, "clean_terminal", "Clean Terminal", null, 0, 3));
        permissions.add(new Permission(4, "setting", "Setting", null, 0, 4));
        permissions.add(new Permission(5, "role_management", "Role Management", null, 0, 5));
        permissions.add(new Permission(6, "user_management", "User Management", null, 0, 6));
        
        // Level 1 - Terminal list sub-permissions
        permissions.add(new Permission(7, "terminal_add", "Add Terminal", 1L, 1, 1));
        permissions.add(new Permission(8, "terminal_detail", "Terminal Detail", 1L, 1, 2));
        
        // Level 2 - Terminal detail sub-permissions (Note: parent_id should be 8, not 6!)
        permissions.add(new Permission(9, "terminal_edit", "Edit", 8L, 2, 1));  // Fixed: parent_id should be 8
        permissions.add(new Permission(10, "terminal_delete", "Delete", 8L, 2, 2)); // Fixed: parent_id should be 8
        permissions.add(new Permission(11, "terminal_mark", "Mark", 8L, 2, 3));   // Fixed: parent_id should be 8
        permissions.add(new Permission(12, "terminal_confirm", "Confirm", 8L, 2, 4)); // Fixed: parent_id should be 8
        
        // Level 1 - Log info sub-permissions
        permissions.add(new Permission(13, "log_export", "Export", 2L, 1, 1));
        permissions.add(new Permission(14, "log_confirm", "Confirm", 2L, 1, 2));
        
        // Level 1 - Clean terminal sub-permissions
        permissions.add(new Permission(15, "clean_export", "Export", 3L, 1, 1));
        permissions.add(new Permission(16, "clean_start_count", "Start Count", 3L, 1, 2));
        
        // Level 1 - Setting sub-permissions
        permissions.add(new Permission(17, "setting_device", "Device Setting", 4L, 1, 1));
        
        // Level 2 - Device setting sub-permissions (Note: parent_id should be 17, not 16!)
        permissions.add(new Permission(18, "setting_sound", "Sound Setting", 17L, 2, 1));  // Fixed: parent_id should be 17
        permissions.add(new Permission(19, "setting_wifi", "WiFi Connection", 17L, 2, 2));   // Fixed: parent_id should be 17
        permissions.add(new Permission(20, "setting_ip", "IP Config", 17L, 2, 3));       // Fixed: parent_id should be 17
        permissions.add(new Permission(21, "setting_count", "Count Times", 17L, 2, 4));   // Fixed: parent_id should be 17
        
        // Level 1 - Role management sub-permissions
        permissions.add(new Permission(22, "role_add", "Add", 5L, 1, 1));
        permissions.add(new Permission(23, "role_edit", "Edit", 5L, 1, 2));
        permissions.add(new Permission(24, "role_delete", "Delete", 5L, 1, 3));
        
        // Level 1 - User management sub-permissions
        permissions.add(new Permission(25, "user_add", "Add", 6L, 1, 1));
        permissions.add(new Permission(26, "user_edit", "Edit", 6L, 1, 2));
        permissions.add(new Permission(27, "user_delete", "Delete", 6L, 1, 3));
        permissions.add(new Permission(28, "user_reset_password", "Reset Password", 6L, 1, 4));
        permissions.add(new Permission(29, "user_disable", "Enable/Disable", 6L, 1, 5));
        
        return permissions;
    }
    
    private static void validateParentChildRelationships(List<Permission> permissions) {
        System.out.println("1. Parent-Child Relationship Validation:");
        Map<Long, Permission> permissionMap = new HashMap<>();
        for (Permission p : permissions) {
            permissionMap.put(p.permissionId, p);
        }
        
        boolean hasErrors = false;
        for (Permission p : permissions) {
            if (p.parentId != null) {
                Permission parent = permissionMap.get(p.parentId);
                if (parent == null) {
                    System.out.println("   [ERROR] " + p + " parent ID " + p.parentId + " does not exist!");
                    hasErrors = true;
                } else {
                    System.out.println("   [OK] " + p.permissionName + " -> Parent: " + parent.permissionName);
                }
            }
        }
        
        if (!hasErrors) {
            System.out.println("   [OK] All parent-child relationships are correct!");
        }
        System.out.println();
    }
    
    private static void validateLevels(List<Permission> permissions) {
        System.out.println("2. Level Validation:");
        Map<Long, Permission> permissionMap = new HashMap<>();
        for (Permission p : permissions) {
            permissionMap.put(p.permissionId, p);
        }
        
        boolean hasErrors = false;
        for (Permission p : permissions) {
            if (p.parentId == null) {
                // Top-level permissions should be level 0
                if (p.level != 0) {
                    System.out.println("   [ERROR] Top-level permission " + p.permissionName + " should be level 0, but is " + p.level);
                    hasErrors = true;
                }
            } else {
                Permission parent = permissionMap.get(p.parentId);
                if (parent != null) {
                    int expectedLevel = parent.level + 1;
                    if (p.level != expectedLevel) {
                        System.out.println("   [ERROR] " + p.permissionName + " should be level " + expectedLevel + ", but is " + p.level);
                        hasErrors = true;
                    }
                }
            }
        }
        
        if (!hasErrors) {
            System.out.println("   [OK] All levels are correct!");
        }
        System.out.println();
    }
    
    private static void displayTreeStructure(List<Permission> permissions) {
        System.out.println("3. Complete Tree Structure:");
        
        // Display by level and sort order
        Map<Long, List<Permission>> childrenMap = new HashMap<>();
        List<Permission> roots = new ArrayList<>();
        
        for (Permission p : permissions) {
            if (p.parentId == null) {
                roots.add(p);
            } else {
                childrenMap.computeIfAbsent(p.parentId, k -> new ArrayList<>()).add(p);
            }
        }
        
        // Sort root nodes
        roots.sort(Comparator.comparingInt(p -> p.sortOrder));
        
        // Recursively display tree structure
        for (Permission root : roots) {
            displayNode(root, childrenMap, "");
        }
    }
    
    private static void displayNode(Permission node, Map<Long, List<Permission>> childrenMap, String indent) {
        System.out.println(indent + "|-- " + node.permissionName + " (ID:" + node.permissionId + ", Level:" + node.level + ")");
        
        List<Permission> children = childrenMap.get(node.permissionId);
        if (children != null) {
            children.sort(Comparator.comparingInt(p -> p.sortOrder));
            for (Permission child : children) {
                displayNode(child, childrenMap, indent + "    ");
            }
        }
    }
}