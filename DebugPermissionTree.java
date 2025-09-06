import java.util.*;

// Debug tool to analyze permission data and tree conversion
public class DebugPermissionTree {
    
    // Simplified Permission class matching the actual one
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
        
        // All getters and setters
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
    
    // Exact copy of the actual PermissionTreeConverter
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
        System.out.println("=== Permission Tree Debug Tool ===");
        System.out.println("This tool helps debug permission tree conversion issues.\n");
        
        // Test with various scenarios that might cause issues
        
        // Scenario 1: Normal hierarchical data
        System.out.println("1. Testing Normal Hierarchical Data:");
        testScenario("Normal Data", createNormalData());
        
        // Scenario 2: Empty data
        System.out.println("\n2. Testing Empty Data:");
        testScenario("Empty Data", new ArrayList<>());
        
        // Scenario 3: Only root nodes
        System.out.println("\n3. Testing Only Root Nodes:");
        testScenario("Only Roots", createOnlyRootData());
        
        // Scenario 4: Missing parent references
        System.out.println("\n4. Testing Missing Parent References:");
        testScenario("Missing Parents", createMissingParentData());
        
        // Scenario 5: Circular references (should be handled gracefully)
        System.out.println("\n5. Testing Orphaned Children:");
        testScenario("Orphaned Children", createOrphanedChildrenData());
        
        // Scenario 6: Unsorted data
        System.out.println("\n6. Testing Unsorted Data:");
        testScenario("Unsorted Data", createUnsortedData());
        
        System.out.println("\n=== Debug Summary ===");
        System.out.println("If your convertToTree() is not working as expected:");
        System.out.println("1. Check if input data has correct parentId values");
        System.out.println("2. Verify that parent permissions exist in the input list");
        System.out.println("3. Ensure sortOrder values are set correctly");
        System.out.println("4. Check if the input list is not null or empty");
        System.out.println("5. Verify that Permission objects have proper getter/setter methods");
    }
    
    private static void testScenario(String scenarioName, List<Permission> inputData) {
        System.out.println("  Scenario: " + scenarioName);
        System.out.println("  Input: " + inputData.size() + " permissions");
        
        if (!inputData.isEmpty()) {
            System.out.println("  Input details:");
            for (Permission p : inputData) {
                System.out.println(String.format("    ID: %d, Name: %s, ParentID: %s, Level: %d, Sort: %d",
                    p.getPermissionId(), p.getPermissionName(),
                    p.getParentId() == null ? "null" : p.getParentId().toString(),
                    p.getLevel(), p.getSortOrder()));
            }
        }
        
        try {
            List<Permission> result = PermissionTreeConverter.convertToTree(inputData);
            System.out.println("  Result: " + result.size() + " root nodes");
            
            if (!result.isEmpty()) {
                System.out.println("  Tree structure:");
                printTree(result, "    ");
                
                // Count total nodes in tree
                int totalNodes = countTotalNodes(result);
                System.out.println("  Total nodes in tree: " + totalNodes);
                System.out.println("  Conversion success: " + (totalNodes == inputData.size() ? "YES" : "NO - Missing nodes!"));
            }
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printTree(List<Permission> permissions, String indent) {
        for (Permission p : permissions) {
            System.out.println(indent + "├─ " + p.getPermissionName() + " (ID: " + p.getPermissionId() + 
                             ", Children: " + (p.getChildList() != null ? p.getChildList().size() : 0) + ")");
            if (p.getChildList() != null && !p.getChildList().isEmpty()) {
                printTree(p.getChildList(), indent + "  ");
            }
        }
    }
    
    private static int countTotalNodes(List<Permission> permissions) {
        int count = permissions.size();
        for (Permission p : permissions) {
            if (p.getChildList() != null && !p.getChildList().isEmpty()) {
                count += countTotalNodes(p.getChildList());
            }
        }
        return count;
    }
    
    private static List<Permission> createNormalData() {
        return Arrays.asList(
            new Permission(1L, "SYSTEM", "System Management", "SYSTEM", "System management", 1, null, 1, 1),
            new Permission(2L, "USER_MGMT", "User Management", "SYSTEM", "User management", 1, 1L, 2, 1),
            new Permission(3L, "USER_ADD", "Add User", "SYSTEM", "Add user", 1, 2L, 3, 1),
            new Permission(4L, "USER_EDIT", "Edit User", "SYSTEM", "Edit user", 1, 2L, 3, 2)
        );
    }
    
    private static List<Permission> createOnlyRootData() {
        return Arrays.asList(
            new Permission(1L, "SYSTEM", "System Management", "SYSTEM", "System management", 1, null, 1, 1),
            new Permission(2L, "BUSINESS", "Business Management", "BUSINESS", "Business management", 1, null, 1, 2)
        );
    }
    
    private static List<Permission> createMissingParentData() {
        return Arrays.asList(
            new Permission(1L, "SYSTEM", "System Management", "SYSTEM", "System management", 1, null, 1, 1),
            new Permission(3L, "USER_ADD", "Add User", "SYSTEM", "Add user", 1, 2L, 3, 1), // Parent ID 2 doesn't exist
            new Permission(4L, "USER_EDIT", "Edit User", "SYSTEM", "Edit user", 1, 2L, 3, 2)  // Parent ID 2 doesn't exist
        );
    }
    
    private static List<Permission> createOrphanedChildrenData() {
        return Arrays.asList(
            new Permission(3L, "USER_ADD", "Add User", "SYSTEM", "Add user", 1, 1L, 3, 1), // Parent ID 1 doesn't exist
            new Permission(4L, "USER_EDIT", "Edit User", "SYSTEM", "Edit user", 1, 1L, 3, 2)  // Parent ID 1 doesn't exist
        );
    }
    
    private static List<Permission> createUnsortedData() {
        return Arrays.asList(
            new Permission(3L, "USER_ADD", "Add User", "SYSTEM", "Add user", 1, 2L, 3, 2),
            new Permission(1L, "SYSTEM", "System Management", "SYSTEM", "System management", 1, null, 1, 2),
            new Permission(4L, "USER_EDIT", "Edit User", "SYSTEM", "Edit user", 1, 2L, 3, 1),
            new Permission(2L, "USER_MGMT", "User Management", "SYSTEM", "User management", 1, 1L, 2, 1)
        );
    }
}