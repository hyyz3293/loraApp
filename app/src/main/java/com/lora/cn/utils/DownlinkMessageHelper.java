package com.lora.cn.utils;

import android.content.Context;
import android.util.Log;
import com.lora.cn.network.MqttPacketsClient;

/**
 * 下行报文调用辅助类
 * 根据下行报文规则编写调用函数
 */
public class DownlinkMessageHelper {
    
    private static final String TAG = "DownlinkMessageHelper";
    
    // MQTT客户端实例
    private MqttPacketsClient mqttClient;
    
    // 下行主题配置
    private static final String DOWNLINK_TOPIC_BASE = "/milesight/downlink";
    private static final int DEFAULT_FPORT = 85;
    
    public DownlinkMessageHelper(MqttPacketsClient mqttClient) {
        this.mqttClient = mqttClient;
    }
    
    /**
     * 发送下行8001报文到指定设备
     * @param deviceIdHex 设备ID (16位HEX字符串)
     * @param ackResult 应答结果 (0:成功, 1:失败)
     * @param queryOp 查询指令 (0:无操作, 1:查询状态, 2:查询配置等)
     * @param departmentId 科室ID
     * @param cartId 台车ID
     * @param registerResult 登记结果 (0:未登记, 1:已登记)
     * @param clearMask 清除码 (4字节掩码)
     * @param reportIntervalMin 定时上报间隔(分钟)
     * @param confirmed 是否需要确认
     */
    public void sendDownlink8001(String deviceIdHex, 
                                int ackResult,
                                int queryOp,
                                int departmentId,
                                int cartId,
                                int registerResult,
                                int clearMask,
                                int reportIntervalMin,
                                boolean confirmed) {
        try {
            if (mqttClient == null) {
                Log.e(TAG, "MQTT客户端未初始化");
                return;
            }
            
            // 生成流水号 (简单递增)
            byte seq = (byte) (System.currentTimeMillis() & 0xFF);
            
            // 获取当前UTC时间
            long currentTimeUtc = System.currentTimeMillis();
            
            // 构建下行8001帧
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001(
                deviceIdHex,
                seq,
                currentTimeUtc,
                ackResult,
                queryOp,
                departmentId,
                cartId,
                registerResult,
                clearMask,
                reportIntervalMin
            );
            
            // 转换为HEX字符串
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            
            // 通过MQTT发送下行报文 (使用设备主题方式)
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                confirmed
            );
            
            Log.i(TAG, "发送下行8001报文成功: deviceId=" + deviceIdHex + 
                      ", ackResult=" + ackResult + 
                      ", queryOp=" + queryOp + 
                      ", departmentId=" + departmentId + 
                      ", cartId=" + cartId + 
                      ", registerResult=" + registerResult + 
                      ", clearMask=" + clearMask + 
                      ", reportInterval=" + reportIntervalMin + "分钟" +
                      ", payloadHex=" + payloadHex);
                      
        } catch (Exception e) {
            Log.e(TAG, "发送下行8001报文失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送标准应答下行报文
     * 用于对设备上行数据进行应答确认
     * @param deviceIdHex 设备ID
     * @param ackResult 应答结果 (0:成功, 1:失败)
     */
    public void sendAckDownlink(String deviceIdHex, int ackResult) {
        sendDownlink8001(
            deviceIdHex,
            ackResult,      // 应答结果
            0,              // 无查询操作
            0,              // 默认科室ID
            0,              // 默认台车ID
            0,              // 默认登记结果
            0,              // 无清除操作
            30,             // 默认30分钟上报间隔
            true            // 需要确认
        );
    }
    
    /**
     * 发送查询状态下行报文
     * 用于主动查询设备状态
     * @param deviceIdHex 设备ID
     */
    public void sendQueryStatusDownlink(String deviceIdHex) {
        sendDownlink8001(
            deviceIdHex,
            0,              // 成功应答
            1,              // 查询状态操作
            0,              // 默认科室ID
            0,              // 默认台车ID
            0,              // 默认登记结果
            0,              // 无清除操作
            30,             // 默认30分钟上报间隔
            true            // 需要确认
        );
    }
    
    /**
     * 发送设备配置下行报文
     * 用于配置设备参数
     * @param deviceIdHex 设备ID
     * @param departmentId 科室ID
     * @param cartId 台车ID
     * @param reportIntervalMin 上报间隔(分钟)
     */
    public void sendConfigDownlink(String deviceIdHex, 
                                  int departmentId, 
                                  int cartId, 
                                  int reportIntervalMin) {
        sendDownlink8001(
            deviceIdHex,
            0,              // 成功应答
            2,              // 配置操作
            departmentId,   // 指定科室ID
            cartId,         // 指定台车ID
            1,              // 已登记
            0,              // 无清除操作
            reportIntervalMin, // 指定上报间隔
            true            // 需要确认
        );
    }
    
    /**
     * 发送清除数据下行报文
     * 用于清除设备特定数据
     * @param deviceIdHex 设备ID
     * @param clearMask 清除掩码 (bit0:清除事件, bit1:清除状态, bit2:清除配置等)
     */
    public void sendClearDataDownlink(String deviceIdHex, int clearMask) {
        sendDownlink8001(
            deviceIdHex,
            0,              // 成功应答
            3,              // 清除操作
            0,              // 默认科室ID
            0,              // 默认台车ID
            0,              // 默认登记结果
            clearMask,      // 指定清除掩码
            30,             // 默认30分钟上报间隔
            true            // 需要确认
        );
    }
}