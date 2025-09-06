package com.lora.cn.database;

import android.content.Context;
import com.lora.cn.database.dao.CategoryDao;
import com.lora.cn.database.dao.DepartmentDao;
import com.lora.cn.database.dao.GroupDao;
import com.lora.cn.database.dao.PositionDao;
import com.lora.cn.database.dao.RoleDao;
import com.lora.cn.database.dao.PermissionDao;
import com.lora.cn.database.dao.RolePermissionDao;
import com.lora.cn.database.dao.UserDao;
import com.lora.cn.database.entity.Category;
import com.lora.cn.database.entity.Department;
import com.lora.cn.database.entity.Group;
import com.lora.cn.database.entity.Position;
import com.lora.cn.database.entity.Role;
import com.lora.cn.database.entity.Permission;
import com.lora.cn.database.entity.RolePermission;
import com.lora.cn.database.entity.User;

import java.util.Date;
import java.util.List;

/**
 * 数据库管理器
 * 提供统一的数据库操作接口
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private DatabaseHelper dbHelper;
    private GroupDao groupDao;
    private CategoryDao categoryDao;
    private DepartmentDao departmentDao;
    private PositionDao positionDao;
    private RoleDao roleDao;
    private PermissionDao permissionDao;
    private RolePermissionDao rolePermissionDao;
    private UserDao userDao;
    
    private DatabaseManager(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
        groupDao = new GroupDao(dbHelper);
        categoryDao = new CategoryDao(dbHelper);
        departmentDao = new DepartmentDao(dbHelper);
        positionDao = new PositionDao(dbHelper);
        roleDao = new RoleDao(dbHelper);
        permissionDao = new PermissionDao(dbHelper);
        rolePermissionDao = new RolePermissionDao(dbHelper);
        userDao = new UserDao(dbHelper);
    }
    
    /**
     * 获取数据库管理器的单例实例
     */
    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }
    
    // ==================== 分组相关操作 ====================
    
    /**
     * 添加分组
     */
    public long addGroup(String groupName, String groupDescription) {
        if (groupDao.isGroupNameExists(groupName)) {
            throw new IllegalArgumentException("分组名称已存在: " + groupName);
        }
        
        Group group = new Group(groupName, groupDescription);
        return groupDao.insertGroup(group);
    }
    
    /**
     * 更新分组
     */
    public boolean updateGroup(long groupId, String groupName, String groupDescription) {
        if (groupDao.isGroupNameExists(groupName, groupId)) {
            throw new IllegalArgumentException("分组名称已存在: " + groupName);
        }
        
        Group group = groupDao.getGroupById(groupId);
        if (group == null) {
            return false;
        }
        
        group.setGroupName(groupName);
        group.setGroupDescription(groupDescription);
        
        return groupDao.updateGroup(group) > 0;
    }
    
    /**
     * 删除分组（会级联删除相关分类）
     */
    public boolean deleteGroup(long groupId) {
        return groupDao.deleteGroup(groupId) > 0;
    }
    
    /**
     * 根据ID获取分组
     */
    public Group getGroupById(long groupId) {
        return groupDao.getGroupById(groupId);
    }
    
    /**
     * 根据名称获取分组
     */
    public Group getGroupByName(String groupName) {
        return groupDao.getGroupByName(groupName);
    }
    
    /**
     * 获取所有分组
     */
    public List<Group> getAllGroups() {
        return groupDao.getAllGroups();
    }
    
    /**
     * 获取分组及其分类列表
     */
    public List<Group> getGroupsWithCategories() {
        List<Group> groups = groupDao.getAllGroups();
        for (Group group : groups) {
            List<Category> categories = categoryDao.getCategoriesByGroupId(group.getGroupId());
            group.setCategories(categories);
        }
        return groups;
    }
    
    /**
     * 检查分组名称是否存在
     */
    public boolean isGroupNameExists(String groupName) {
        return groupDao.isGroupNameExists(groupName);
    }
    
    /**
     * 获取分组总数
     */
    public int getGroupCount() {
        return groupDao.getGroupCount();
    }
    
    // ==================== 分类相关操作 ====================
    
    /**
     * 添加分类
     */
    public long addCategory(String categoryName, String categoryDescription, long groupId) {
        // 检查分组是否存在
        Group group = groupDao.getGroupById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("分组不存在，ID: " + groupId);
        }
        
        // 检查分类名称在该分组中是否已存在
        if (categoryDao.isCategoryNameExistsInGroup(categoryName, groupId)) {
            throw new IllegalArgumentException("分类名称在该分组中已存在: " + categoryName);
        }
        
        Category category = new Category(categoryName, categoryDescription, groupId);
        return categoryDao.insertCategory(category);
    }
    
    /**
     * 更新分类
     */
    public boolean updateCategory(long categoryId, String categoryName, String categoryDescription, long groupId) {
        // 检查分组是否存在
        Group group = groupDao.getGroupById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("分组不存在，ID: " + groupId);
        }
        
        // 检查分类名称在该分组中是否已存在（排除当前分类）
        if (categoryDao.isCategoryNameExistsInGroup(categoryName, groupId, categoryId)) {
            throw new IllegalArgumentException("分类名称在该分组中已存在: " + categoryName);
        }
        
        Category category = categoryDao.getCategoryById(categoryId);
        if (category == null) {
            return false;
        }
        
        category.setCategoryName(categoryName);
        category.setCategoryDescription(categoryDescription);
        category.setGroupId(groupId);
        
        return categoryDao.updateCategory(category) > 0;
    }
    
    /**
     * 删除分类
     */
    public boolean deleteCategory(long categoryId) {
        return categoryDao.deleteCategory(categoryId) > 0;
    }
    
    /**
     * 根据ID获取分类
     */
    public Category getCategoryById(long categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }
    
    /**
     * 根据分组ID获取分类列表
     */
    public List<Category> getCategoriesByGroupId(long groupId) {
        return categoryDao.getCategoriesByGroupId(groupId);
    }
    
    /**
     * 获取所有分类
     */
    public List<Category> getAllCategories() {
        return categoryDao.getAllCategories();
    }
    
    /**
     * 获取分类及其关联的分组信息
     */
    public List<Category> getCategoriesWithGroup() {
        return categoryDao.getCategoriesWithGroup();
    }
    
    /**
     * 检查分类名称在指定分组中是否存在
     */
    public boolean isCategoryNameExistsInGroup(String categoryName, long groupId) {
        return categoryDao.isCategoryNameExistsInGroup(categoryName, groupId);
    }
    
    /**
     * 获取指定分组的分类总数
     */
    public int getCategoryCountByGroupId(long groupId) {
        return categoryDao.getCategoryCountByGroupId(groupId);
    }
    
    /**
     * 获取分类总数
     */
    public int getCategoryCount() {
        return categoryDao.getCategoryCount();
    }
    
    // ==================== 科室相关操作 ====================
    
    /**
     * 添加科室
     */
    public long addDepartment(String departmentName, int sortOrder, int status) {
        if (departmentDao.isDepartmentNameExists(departmentName)) {
            throw new IllegalArgumentException("科室名称已存在: " + departmentName);
        }
        
        Department department = new Department();
        department.setDepartmentName(departmentName);
        department.setSortOrder(sortOrder);
        department.setStatus(status);
        
        return departmentDao.insertDepartment(department);
    }
    
    /**
     * 插入科室对象
     */
    public long insertDepartment(Department department) {
        if (departmentDao.isDepartmentNameExists(department.getDepartmentName())) {
            throw new IllegalArgumentException("科室名称已存在: " + department.getDepartmentName());
        }
        
        return departmentDao.insertDepartment(department);
    }
    
    /**
     * 更新科室
     */
    public int updateDepartment(Department department) {
        return departmentDao.updateDepartment(department);
    }
    
    /**
     * 删除科室
     */
    public int deleteDepartment(long departmentId) {
        return departmentDao.deleteDepartment(departmentId);
    }
    
    /**
     * 根据ID获取科室
     */
    public Department getDepartmentById(long departmentId) {
        return departmentDao.getDepartmentById(departmentId);
    }
    
    /**
     * 根据名称获取科室
     */
    public Department getDepartmentByName(String departmentName) {
        return departmentDao.getDepartmentByName(departmentName);
    }
    
    /**
     * 获取所有科室
     */
    public List<Department> getAllDepartments() {
        return departmentDao.getAllDepartments();
    }
    
    /**
     * 检查科室名称是否存在
     */
    public boolean isDepartmentNameExists(String departmentName) {
        return departmentDao.isDepartmentNameExists(departmentName);
    }
    
    /**
     * 获取科室总数
     */
    public int getDepartmentCount() {
        return departmentDao.getDepartmentCount();
    }
    
    // ==================== 职位相关操作 ====================
    
    /**
     * 添加职位
     */
    public long addPosition(String positionName, int sortOrder, int status) {
        if (positionDao.isPositionNameExists(positionName)) {
            throw new IllegalArgumentException("职位名称已存在: " + positionName);
        }
        
        Position position = new Position();
        position.setPositionName(positionName);
        position.setSortOrder(sortOrder);
        position.setStatus(status);
        
        return positionDao.insertPosition(position);
    }
    
    /**
     * 插入职位对象
     */
    public long insertPosition(Position position) {
        if (positionDao.isPositionNameExists(position.getPositionName())) {
            throw new IllegalArgumentException("职位名称已存在: " + position.getPositionName());
        }
        
        return positionDao.insertPosition(position);
    }
    
    /**
     * 更新职位
     */
    public int updatePosition(Position position) {
        return positionDao.updatePosition(position);
    }
    
    /**
     * 删除职位
     */
    public int deletePosition(long positionId) {
        return positionDao.deletePosition(positionId);
    }
    
    /**
     * 根据ID获取职位
     */
    public Position getPositionById(long positionId) {
        return positionDao.getPositionById(positionId);
    }
    
    /**
     * 根据名称获取职位
     */
    public Position getPositionByName(String positionName) {
        return positionDao.getPositionByName(positionName);
    }
    
    /**
     * 获取所有职位
     */
    public List<Position> getAllPositions() {
        return positionDao.getAllPositions();
    }
    
    /**
     * 检查职位名称是否存在
     */
    public boolean isPositionNameExists(String positionName) {
        return positionDao.isPositionNameExists(positionName);
    }
    
    /**
     * 获取职位总数
     */
    public int getPositionCount() {
        return positionDao.getPositionCount();
    }
    
    // ==================== 统计相关操作 ====================
    
    /**
     * 获取数据库统计信息
     */
    public DatabaseStats getDatabaseStats() {
        int groupCount = getGroupCount();
        int categoryCount = getCategoryCount();
        int departmentCount = getDepartmentCount();
        int positionCount = getPositionCount();
        int roleCount = getRoleCount();
        int permissionCount = getPermissionCount();
        
        return new DatabaseStats(groupCount, categoryCount, departmentCount, positionCount, roleCount, permissionCount);
    }
    
    // ==================== 角色相关操作 ====================
    
    /**
     * 添加角色
     */
    public long addRole(String roleName, String description) {
        if (roleDao.isRoleNameExists(roleName, 0)) {
            throw new IllegalArgumentException("角色名称已存在: " + roleName);
        }
        
        Role role = new Role();
        role.setRoleName(roleName);
        role.setDescription(description);
        role.setStatus(1); // 默认启用
        String currentTime = String.valueOf(System.currentTimeMillis());
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        
        return roleDao.insertRole(role);
    }
    
    /**
     * 插入角色
     */
    public long insertRole(Role role) {
        return roleDao.insertRole(role);
    }
    
    /**
     * 更新角色
     */
    public boolean updateRole(int roleId, String roleName, String description) {
        if (roleDao.isRoleNameExists(roleName, roleId)) {
            throw new IllegalArgumentException("角色名称已存在: " + roleName);
        }
        
        Role role = roleDao.getRoleById(roleId);
        if (role == null) {
            return false;
        }
        
        role.setRoleName(roleName);
        role.setDescription(description);
        role.setUpdateTime(new Date());
        
        return roleDao.updateRole(role) > 0;
    }
    
    /**
     * 更新角色
     */
    public boolean updateRole(Role role) {
        return roleDao.updateRole(role) > 0;
    }
    
    /**
     * 删除角色（会级联删除相关权限关联）
     */
    public boolean deleteRole(int roleId) {
        // 先删除角色权限关联
        rolePermissionDao.deleteRolePermissionsByRoleId(roleId);
        // 再删除角色
        return roleDao.deleteRole(roleId) > 0;
    }
    
    /**
     * 根据ID获取角色
     */
    public Role getRoleById(int roleId) {
        return roleDao.getRoleById(roleId);
    }
    
    /**
     * 根据名称获取角色
     */
    public Role getRoleByName(String roleName) {
        return roleDao.getRoleByName(roleName);
    }
    
    /**
     * 获取所有角色
     */
    public List<Role> getAllRoles() {
        return roleDao.getAllRoles();
    }
    
    /**
     * 获取启用的角色
     */
    public List<Role> getActiveRoles() {
        return roleDao.getActiveRoles();
    }
    
    /**
     * 检查角色名称是否存在
     */
    public boolean isRoleNameExists(String roleName) {
        return roleDao.isRoleNameExists(roleName, 0);
    }
    
    /**
     * 获取角色总数
     */
    public int getRoleCount() {
        return roleDao.getRoleCount();
    }
    
    // ==================== 权限相关操作 ====================
    
    /**
     * 获取所有权限
     */
    public List<Permission> getAllPermissions() {
        return permissionDao.getAllPermissions();
    }
    
    /**
     * 根据分类获取权限
     */
    public List<Permission> getPermissionsByCategory(String category) {
        return permissionDao.getPermissionsByCategory(category);
    }
    
    /**
     * 获取启用的权限
     */
    public List<Permission> getActivePermissions() {
        return permissionDao.getActivePermissions();
    }
    
    /**
     * 根据角色ID获取权限
     */
    public List<Permission> getPermissionsByRoleId(int roleId) {
        return permissionDao.getPermissionsByRoleId(roleId);
    }
    
    /**
     * 获取所有权限分类
     */
    public List<String> getAllPermissionCategories() {
        return permissionDao.getAllCategories();
    }
    
    /**
     * 获取权限总数
     */
    public int getPermissionCount() {
        return permissionDao.getAllPermissions().size();
    }
    
    // ==================== 角色权限关联操作 ====================
    
    /**
     * 设置角色权限
     */
    public boolean setRolePermissions(int roleId, List<Integer> permissionIds) {
        return rolePermissionDao.setRolePermissions(roleId, permissionIds);
    }
    
    /**
     * 检查角色是否拥有某个权限
     */
    public boolean hasPermission(int roleId, String permissionCode) {
        return rolePermissionDao.hasPermissionByCode(roleId, permissionCode);
    }
    
    /**
     * 获取角色的权限ID列表
     */
    public List<Integer> getPermissionIdsByRoleId(int roleId) {
        return rolePermissionDao.getPermissionIdsByRoleId(roleId);
    }
    
    // ==================== 树形权限管理操作 ====================
    
    /**
     * 获取根权限列表（顶级权限）
     */
    public List<Permission> getRootPermissions() {
        return permissionDao.getRootPermissions();
    }
    
    /**
     * 根据父权限ID获取子权限列表
     */
    public List<Permission> getChildPermissions(int parentId) {
        return permissionDao.getChildPermissions(parentId);
    }
    
    /**
     * 获取指定层级的权限列表
     */
    public List<Permission> getPermissionsByLevel(int level) {
        return permissionDao.getPermissionsByLevel(level);
    }
    
    /**
     * 获取完整的权限树结构
     */
    public List<Permission> getPermissionTree() {
        return permissionDao.getPermissionTree();
    }
    
    /**
     * 检查权限是否有子权限
     */
    public boolean hasChildPermissions(int permissionId) {
        return permissionDao.hasChildPermissions(permissionId);
    }
    
    /**
     * 获取权限的所有祖先权限ID列表
     */
    public List<Integer> getAncestorPermissionIds(int permissionId) {
        return permissionDao.getAncestorPermissionIds(permissionId);
    }
    
    /**
     * 获取权限的所有后代权限ID列表
     */
    public List<Integer> getDescendantPermissionIds(int permissionId) {
        return permissionDao.getDescendantPermissionIds(permissionId);
    }
    
    /**
     * 根据权限ID获取权限信息
     */
    public Permission getPermissionById(int permissionId) {
        return permissionDao.getPermissionById(permissionId);
    }
    
    /**
     * 根据权限代码获取权限信息
     */
    public Permission getPermissionByCode(String permissionCode) {
        return permissionDao.getPermissionByCode(permissionCode);
    }
    
    // ==================== 用户管理相关方法 ====================
    
    /**
     * 添加用户
     */
    public long addUser(User user) {
        if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            return -1;
        }
        if (user.getUserAccount() == null || user.getUserAccount().trim().isEmpty()) {
            return -1;
        }
        if (user.getUserPassword() == null || user.getUserPassword().trim().isEmpty()) {
            return -1;
        }
        
        return userDao.insertUser(user);
    }
    
    /**
     * 更新用户信息
     */
    public boolean updateUser(User user) {
        if (user.getUserId() <= 0) {
            return false;
        }
        return userDao.updateUser(user) > 0;
    }
    
    /**
     * 更新用户密码
     */
    public boolean updateUserPassword(long userId, String newPassword) {
        if (userId <= 0 || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }
        return userDao.updateUserPassword((int) userId, newPassword) > 0;
    }
    
    /**
     * 删除用户
     */
    public boolean deleteUser(long userId) {
        return userDao.deleteUser((int) userId) > 0;
    }
    
    /**
     * 根据用户ID获取用户信息
     */
    public User getUserById(long userId) {
        return userDao.getUserById((int) userId);
    }
    
    /**
     * 根据用户账号获取用户信息
     */
    public User getUserByAccount(String userAccount) {
        return userDao.getUserByAccount(userAccount);
    }
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }
    
    /**
     * 获取启用状态的用户
     */
    public List<User> getActiveUsers() {
        return userDao.getActiveUsers();
    }
    
    /**
     * 根据角色ID获取用户列表
     */
    public List<User> getUsersByRoleId(int roleId) {
        return userDao.getUsersByRoleId(roleId);
    }
    
    /**
     * 检查用户账号是否已存在
     */
    public boolean isUserAccountExists(String userAccount) {
        return userDao.isUserAccountExists(userAccount, 0);
    }
    
    /**
     * 检查用户账号是否已存在（排除指定用户ID）
     */
    public boolean isUserAccountExists(String userAccount, int excludeUserId) {
        return userDao.isUserAccountExists(userAccount, excludeUserId);
    }
    
    /**
     * 用户认证
     */
    public User authenticateUser(String userAccount, String password) {
        return userDao.authenticateUser(userAccount, password);
    }
    
    /**
     * 获取用户总数
     */
    public int getUserCount() {
        return getAllUsers().size();
    }
    
    /**
     * 数据库统计信息类
     */
    public static class DatabaseStats {
        private int groupCount;
        private int categoryCount;
        private int departmentCount;
        private int positionCount;
        
        private int roleCount;
        private int permissionCount;
        
        public DatabaseStats(int groupCount, int categoryCount, int departmentCount, int positionCount) {
            this.groupCount = groupCount;
            this.categoryCount = categoryCount;
            this.departmentCount = departmentCount;
            this.positionCount = positionCount;
        }
        
        public DatabaseStats(int groupCount, int categoryCount, int departmentCount, int positionCount, int roleCount, int permissionCount) {
            this.groupCount = groupCount;
            this.categoryCount = categoryCount;
            this.departmentCount = departmentCount;
            this.positionCount = positionCount;
            this.roleCount = roleCount;
            this.permissionCount = permissionCount;
        }
        
        public int getGroupCount() {
            return groupCount;
        }
        
        public int getCategoryCount() {
            return categoryCount;
        }
        
        public int getDepartmentCount() {
            return departmentCount;
        }
        
        public int getPositionCount() {
            return positionCount;
        }
        
        public int getRoleCount() {
            return roleCount;
        }
        
        public int getPermissionCount() {
            return permissionCount;
        }
        
        @Override
        public String toString() {
            return "DatabaseStats{" +
                    "groupCount=" + groupCount +
                    ", categoryCount=" + categoryCount +
                    ", departmentCount=" + departmentCount +
                    ", positionCount=" + positionCount +
                    ", roleCount=" + roleCount +
                    ", permissionCount=" + permissionCount +
                    '}';
        }
    }
}