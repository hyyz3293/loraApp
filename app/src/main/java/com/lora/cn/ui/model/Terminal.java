package com.lora.cn.ui.model;

public class Terminal {
    private long id;
    private String terminalId;
    private String terminalName;
    private String name;
    private String department;
    private String location;
    private int statusIconResId;
    private String statusText;
    private String status;
    private int batteryIconResId;
    private String batteryText;
    private boolean isImportant;
    private int signalStrength;
    private long createTime;
    private long updateTime;
    
    // 上行数据相关字段
    private java.util.Date dataTime;           // 数据产生时间
    private Long deviceEvent;        // 设备事件
    private Long deviceStatus;       // 设备状态
    private Integer batteryVoltage;  // 电池电压
    private Integer batteryLevel;    // 电量
    private Integer rssi;            // RSSI
    private Integer departmentNumber; // 科室或护士站编号
    private Integer cartNumber;      // 台车编号
    private Integer deviceCount;     // 放置的设备数量
    private Integer rackNumber;      // 设备所属台车台架编号
    private String functionCode;     // 功能码
    private Integer sequenceNumber;  // 序列号
    private Integer dataLength;      // 数据长度
    private byte[] dataContent;      // 数据内容
    private Byte checksum;           // 校验码

    public Terminal() {
    }

    public Terminal(String name, String department, String location, 
                   int statusIconResId, String statusText, 
                   int batteryIconResId, String batteryText, 
                   boolean isImportant) {
        this.name = name;
        this.department = department;
        this.location = location;
        this.statusIconResId = statusIconResId;
        this.statusText = statusText;
        this.batteryIconResId = batteryIconResId;
        this.batteryText = batteryText;
        this.isImportant = isImportant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getStatusIconResId() {
        return statusIconResId;
    }

    public void setStatusIconResId(int statusIconResId) {
        this.statusIconResId = statusIconResId;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public int getBatteryIconResId() {
        return batteryIconResId;
    }

    public void setBatteryIconResId(int batteryIconResId) {
        this.batteryIconResId = batteryIconResId;
    }

    public String getBatteryText() {
        return batteryText;
    }

    public void setBatteryText(String batteryText) {
        this.batteryText = batteryText;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }

    // 新增字段的getter和setter方法
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    // 上行数据相关字段的getter和setter方法
    public java.util.Date getDataTime() {
        return dataTime;
    }

    public void setDataTime(java.util.Date dataTime) {
        this.dataTime = dataTime;
    }

    public Long getDeviceEvent() {
        return deviceEvent;
    }

    public void setDeviceEvent(Long deviceEvent) {
        this.deviceEvent = deviceEvent;
    }

    public Long getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(Long deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public Integer getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(Integer batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    public Integer getDepartmentNumber() {
        return departmentNumber;
    }

    public void setDepartmentNumber(Integer departmentNumber) {
        this.departmentNumber = departmentNumber;
    }

    public Integer getCartNumber() {
        return cartNumber;
    }

    public void setCartNumber(Integer cartNumber) {
        this.cartNumber = cartNumber;
    }

    public Integer getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(Integer deviceCount) {
        this.deviceCount = deviceCount;
    }

    public Integer getRackNumber() {
        return rackNumber;
    }

    public void setRackNumber(Integer rackNumber) {
        this.rackNumber = rackNumber;
    }

    // 新增字段的getter和setter方法
    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getDataLength() {
        return dataLength;
    }

    public void setDataLength(Integer dataLength) {
        this.dataLength = dataLength;
    }

    public byte[] getDataContent() {
        return dataContent;
    }

    public void setDataContent(byte[] dataContent) {
        this.dataContent = dataContent;
    }

    public Byte getChecksum() {
        return checksum;
    }

    public void setChecksum(Byte checksum) {
        this.checksum = checksum;
    }
}