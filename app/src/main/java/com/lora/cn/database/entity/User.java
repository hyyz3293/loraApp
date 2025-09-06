package com.lora.cn.database.entity;

import java.util.Date;

/**
 * 用户实体类
 */
public class User {
    private long userId;
    private String userName;        // 用户姓名
    private String userAccount;     // 用户账号 *必填
    private String userPassword;    // 用户密码 *必填
    private long roleId;           // 用户角色ID *必填
    private int status;            // 状态 *必填 (0-禁用, 1-启用)
    private long positionId;       // 职位ID *必填
    private long departmentId;     // 科室ID *必填
    private String userCode;       // 编号 *必填
    private String gender;         // 性别 *必填 (男/女)
    private String phone;          // 电话号码
    private Date createTime;
    private Date updateTime;
    
    // 关联对象
    private Role role;             // 关联的角色对象
    private Position position;     // 关联的职位对象
    private Department department; // 关联的科室对象
    
    public User() {
    }
    
    public User(String userName, String userAccount, String userPassword, 
                long roleId, int status, long positionId, long departmentId, 
                String userCode, String gender) {
        this.userName = userName;
        this.userAccount = userAccount;
        this.userPassword = userPassword;
        this.roleId = roleId;
        this.status = status;
        this.positionId = positionId;
        this.departmentId = departmentId;
        this.userCode = userCode;
        this.gender = gender;
    }
    
    // Getters and Setters
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserAccount() {
        return userAccount;
    }
    
    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }
    
    public String getUserPassword() {
        return userPassword;
    }
    
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
    
    public long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public long getPositionId() {
        return positionId;
    }
    
    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }
    
    public long getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(long departmentId) {
        this.departmentId = departmentId;
    }
    
    public String getUserCode() {
        return userCode;
    }
    
    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public void setPosition(Position position) {
        this.position = position;
    }
    
    public Department getDepartment() {
        return department;
    }
    
    public void setDepartment(Department department) {
        this.department = department;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", userAccount='" + userAccount + '\'' +
                ", roleId=" + roleId +
                ", status=" + status +
                ", positionId=" + positionId +
                ", departmentId=" + departmentId +
                ", userCode='" + userCode + '\'' +
                ", gender='" + gender + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}