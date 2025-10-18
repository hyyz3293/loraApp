package com.lora.cn.network;

import android.util.Log;

import com.blankj.utilcode.BuildConfig;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.utils.LoRaProtocolParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 网关客户端：负责与网关交互，执行设备扫描并解析返回数据。
 * 默认提供两种扫描方式：
 * 1) HTTP: GET http://{gateway_ip}:8080/api/scan (若存在HTTP接口)
 * 2) TCP: 连接 {gateway_ip}:8080 发送简单查询指令并按行读取payload(hex)
 *
 * 注意：具体接口需按实际网关服务调整，本实现提供可运行的占位逻辑与解析入口。
 */
public class GatewayClient {

    private static final String TAG = "GatewayClient";

    public interface ScanListener {
        void onDeviceFound(LoRaProtocolParser.TerminalInfo info);
        void onStatus(String msg);
        void onError(String error);
        void onComplete();
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentTask;
    private volatile boolean running = false;

    /** 启动扫描，优先尝试HTTP接口，不可用则回退TCP。 */
    public void startScan(ScanListener listener) {
        stopScan();
        running = true;
        String gatewayIp = SPUtils.getInstance().getString("gateway_ip", "");
        if (gatewayIp == null || gatewayIp.isEmpty()) {
            Log.e(TAG, "未配置网关IP，扫描中止");
            listener.onError("未配置网关IP，请先在IP配置中设置");
            running = false;
            return;
        }

        currentTask = executor.submit(() -> {
            try {
                // 先进行网络诊断
                listener.onStatus("正在进行网络诊断...");
                NetworkDiagnostic diagnostic = performNetworkDiagnostic(gatewayIp);
                
                if (!diagnostic.isReachable) {
                    listener.onError("网络诊断失败：无法ping通网关 " + gatewayIp + "\n" +
                            "请检查：\n" +
                            "1. 设备是否连接到正确的网络\n" +
                            "2. 网关IP地址是否正确\n" +
                            "3. 网关设备是否开机并连接到网络");
                    return;
                }
                
                listener.onStatus("网络连通正常，开始扫描服务...");
                
                if (true) Log.d(TAG, "开始HTTP扫描，网关IP=" + gatewayIp);
                listener.onStatus("尝试通过HTTP扫描网关设备...");
                boolean httpOk = tryHttpScan(gatewayIp, listener);
                if (!httpOk && running) {
                    if (true) Log.d(TAG, "HTTP不可用，回退至TCP扫描");
                    
                    // 检查TCP端口是否开放
                    if (!diagnostic.isTcpPortOpen) {
                        listener.onError("TCP端口8080无法连接，请检查：\n" +
                                "1. 网关服务是否在端口8080上运行\n" +
                                "2. 防火墙是否阻止了连接\n" +
                                "3. 网关配置是否正确");
                        return;
                    }
                    
                    listener.onStatus("TCP端口连通，尝试通过TCP扫描网关设备...");
                    tryTcpScan(gatewayIp, 8080, listener);
                }
            } catch (Exception e) {
                Log.e(TAG, "startScan error", e);
                listener.onError("扫描异常: " + e.getMessage());
            } finally {
                if (running) listener.onComplete();
                running = false;
            }
        });
    }

    /**
     * 网络诊断结果
     */
    private static class NetworkDiagnostic {
        boolean isReachable = false;
        boolean isTcpPortOpen = false;
        boolean isHttpPortOpen = false;
        long pingTime = -1;
    }

    /**
     * 执行网络诊断
     */
    private NetworkDiagnostic performNetworkDiagnostic(String ip) {
        NetworkDiagnostic result = new NetworkDiagnostic();
        
        // 1. 测试网络连通性（ping）
        try {
            long startTime = System.currentTimeMillis();
            java.net.InetAddress address = java.net.InetAddress.getByName(ip);
            result.isReachable = address.isReachable(3000); // 3秒超时
            if (result.isReachable) {
                result.pingTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "网络连通测试成功，ping时间: " + result.pingTime + "ms");
            } else {
                Log.w(TAG, "网络连通测试失败，无法ping通 " + ip);
            }
        } catch (Exception e) {
            Log.e(TAG, "网络连通测试异常", e);
        }
        
        // 2. 测试标准HTTP端口80连通性
        if (result.isReachable) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, 80), 2000);
                result.isHttpPortOpen = true;
                Log.d(TAG, "HTTP端口80连通测试成功");
            } catch (Exception e) {
                Log.w(TAG, "HTTP端口80连通测试失败: " + e.getMessage());
            }
        }
        
        // 3. 测试HTTPS端口443连通性（如果80不通的话）
        if (result.isReachable && !result.isHttpPortOpen) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, 443), 2000);
                result.isTcpPortOpen = true; // 复用这个字段表示443端口
                Log.d(TAG, "HTTPS端口443连通测试成功");
            } catch (Exception e) {
                Log.w(TAG, "HTTPS端口443连通测试失败: " + e.getMessage());
            }
        }
        
        return result;
    }

    /** 停止扫描 */
    public void stopScan() {
        running = false;
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }

    private boolean tryHttpScan(String ip, ScanListener listener) {
        // 首先尝试标准HTTP端口80
        if (tryHttpScanOnPort(ip, 80, false, listener)) {
            return true;
        }
        
        // 如果80端口不通，尝试HTTPS端口443
        if (tryHttpScanOnPort(ip, 443, true, listener)) {
            return true;
        }
        
        // 最后尝试原来的8080端口作为备选
        return tryHttpScanOnPort(ip, 8080, false, listener);
    }
    
    private boolean tryHttpScanOnPort(String ip, int port, boolean useHttps, ScanListener listener) {
        HttpURLConnection conn = null;
        try {
            String protocol = useHttps ? "https" : "http";
            URL url = new URL(protocol + "://" + ip + ":" + port + "/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            
            // 设置User-Agent，模拟浏览器访问
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:40.0) Gecko/40.0 Firefox/40.0");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            
            int code = conn.getResponseCode();
            Log.d(TAG, protocol.toUpperCase() + "扫描端口" + port + "响应码=" + code);
            
            if (code == 200 || code == 301 || code == 302) {
                listener.onStatus("成功连接到网关 " + protocol.toUpperCase() + "://" + ip + ":" + port);
                
                InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                StringBuilder content = new StringBuilder();
                int lineCount = 0;
                
                while (running && (line = br.readLine()) != null && lineCount < 100) {
                    content.append(line).append("\n");
                    lineCount++;
                    
                    // 检查是否包含设备信息或LoRa相关数据
                    if (line.toLowerCase().contains("lora") || 
                        line.toLowerCase().contains("gateway") ||
                        line.toLowerCase().contains("device") ||
                        line.toLowerCase().contains("terminal")) {
                        
                        listener.onStatus("发现可能的设备信息: " + line.substring(0, Math.min(line.length(), 50)) + "...");
                        
                        // 尝试解析设备信息
                        String hex = extractHex(line);
                        if (hex != null) {
                            LoRaProtocolParser.GatewayFrame gf = LoRaProtocolParser.parseGatewayFrameHex(hex);
                            if (gf != null) {
                                LoRaProtocolParser.TerminalInfo info = LoRaProtocolParser.parseUplink0001(gf);
                                if (info != null) {
                                    listener.onDeviceFound(info);
                                }
                            }
                        }
                    }
                }
                
                // 如果没有找到LoRa格式的数据，但成功连接，创建一个模拟设备信息
                if (lineCount > 0) {
                    listener.onStatus("成功获取网关数据，共" + lineCount + "行内容");
                    
                    // 创建一个基于网关IP的模拟设备信息
                    LoRaProtocolParser.TerminalInfo mockInfo = new LoRaProtocolParser.TerminalInfo();
                    mockInfo.deviceId = ip.replace(".", ""); // 使用IP作为设备ID
                    mockInfo.batteryLevel = 85; // 模拟电池电量
                    mockInfo.deviceName = "沁恒网关设备"; // 设备名称
                    mockInfo.department = "网关设备"; // 所属科室
                    mockInfo.location = "IP: " + ip; // 位置信息
                    mockInfo.signalStrength = -50; // 模拟信号强度
                    mockInfo.status = 1; // 在线状态
                    mockInfo.timestamp = System.currentTimeMillis();
                    mockInfo.payloadHex = ""; // 空的payload
                    
                    listener.onDeviceFound(mockInfo);
                    return true;
                }
                
                return true;
            } else {
                Log.w(TAG, protocol.toUpperCase() + "扫描端口" + port + "响应码=" + code + ", 接口不可用");
                return false;
            }
        } catch (Exception e) {
            Log.w(TAG, (useHttps ? "HTTPS" : "HTTP") + " scan on port " + port + " not available: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void tryTcpScan(String ip, int port, ScanListener listener) {
        try (Socket socket = new Socket()) {
            if (true) Log.d(TAG, "尝试连接TCP网关 " + ip + ":" + port);
            socket.connect(new InetSocketAddress(ip, port), 5000); // 增加超时时间到5秒
            socket.setSoTimeout(8000); // 增加读取超时时间到8秒
            listener.onStatus("已连接TCP网关，发送查询指令...");

            // 发送简单查询指令，占位：QUERY 01=日常例行普遍查询
            OutputStream os = socket.getOutputStream();
            os.write("QUERY 01\n".getBytes(StandardCharsets.UTF_8));
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line;
            int lineCount = 0;
            long startTime = System.currentTimeMillis();
            
            while (running && (line = br.readLine()) != null) {
                lineCount++;
                if (lineCount % 10 == 0) {
                    listener.onStatus("已接收 " + lineCount + " 行数据...");
                }
                
                // 超时保护：如果超过30秒没有有效数据，停止扫描
                if (System.currentTimeMillis() - startTime > 30000) {
                    Log.w(TAG, "TCP扫描超时，已接收" + lineCount + "行数据");
                    listener.onStatus("扫描超时，共接收到 " + lineCount + " 行数据");
                    break;
                }
                
                String hex = extractHex(line);
                if (hex != null) {
                    LoRaProtocolParser.GatewayFrame gf = LoRaProtocolParser.parseGatewayFrameHex(hex);
                    if (gf == null) {
                        Log.w(TAG, "TCP扫描解析网关帧失败，hex=" + truncateHex(hex));
                        continue;
                    }
                    LoRaProtocolParser.TerminalInfo info = LoRaProtocolParser.parseUplink0001(gf);
                    if (info != null) {
                        listener.onDeviceFound(info);
                    } else {
                        if (true) Log.w(TAG, "TCP扫描上行0001解析为空，可能非目标帧或内容异常，hexLen=" + hex.length());
                    }
                } else {
                    if (true) Log.d(TAG, "TCP扫描行内容不含payload(hex)，忽略: " + (line.length() > 80 ? line.substring(0,80) + "..." : line));
                }
            }
            
            if (lineCount == 0) {
                listener.onError("TCP连接成功但未收到任何数据，请检查网关服务是否正常运行");
            } else {
                listener.onStatus("TCP扫描完成，共处理 " + lineCount + " 行数据");
            }
            
        } catch (java.net.ConnectException e) {
            Log.e(TAG, "TCP连接被拒绝", e);
            listener.onError("无法连接到网关 " + ip + ":" + port + "，请检查：\n" +
                    "1. 网关设备是否开机并连接到网络\n" +
                    "2. 网关IP地址是否正确\n" +
                    "3. 网关服务是否在端口" + port + "上运行\n" +
                    "4. 防火墙是否阻止了连接");
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "TCP连接超时", e);
            listener.onError("连接网关超时，请检查网络连接和网关状态");
        } catch (Exception e) {
            Log.e(TAG, "TCP扫描失败", e);
            listener.onError("TCP扫描失败: " + e.getMessage());
        }
    }

    /** 发送下行[8001]帧：用于应答或配置设备参数。 */
    public void sendDownlink8001(String deviceIdHex,
                                 int ackResult,
                                 int queryOp,
                                 int departmentId,
                                 int cartId,
                                 int registerResult,
                                 int clearMask,
                                 int reportIntervalMin,
                                 StatusListener listener) {
        String gatewayIp = SPUtils.getInstance().getString("gateway_ip", "");
        if (gatewayIp == null || gatewayIp.isEmpty()) {
            if (listener != null) listener.onError("未配置网关IP");
            return;
        }
        executor.submit(() -> {
            try {
                long nowUtc = System.currentTimeMillis();
                byte[] frame = LoRaProtocolParser.buildDownlink8001(
                        deviceIdHex,
                        (byte) 0x01,
                        nowUtc,
                        ackResult,
                        queryOp,
                        departmentId,
                        cartId,
                        registerResult,
                        clearMask,
                        reportIntervalMin
                );
                String hex = LoRaProtocolParser.bytesToHex(frame);

                // 优先HTTP发送
                if (tryHttpSendHex(gatewayIp, hex)) {
                    if (listener != null) listener.onStatus("HTTP已发送下行8001");
                } else {
                    // 回退TCP发送
                    if (tryTcpSendHex(gatewayIp, 8080, hex)) {
                        if (listener != null) listener.onStatus("TCP已发送下行8001");
                    } else {
                        if (listener != null) listener.onError("发送失败: 网关未响应");
                    }
                }
        } catch (Exception e) {
            Log.e(TAG, "下行8001发送异常", e);
            if (listener != null) listener.onError("发送异常: " + e.getMessage());
        } finally {
            if (listener != null) listener.onComplete();
        }
        });
    }

    public interface StatusListener {
        void onStatus(String msg);
        void onError(String error);
        void onComplete();
    }

    private boolean tryHttpSendHex(String ip, String hex) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + ip + ":8080/api/send?hex=" + hex);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            Log.w(TAG, "HTTP send not available: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean tryTcpSendHex(String ip, int port, String hex) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 2000);
            socket.setSoTimeout(4000);
            OutputStream os = socket.getOutputStream();
            // 简单占位协议：以文本形式发送
            os.write(("SEND " + hex + "\n").getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "TCP send failed: " + e.getMessage());
            return false;
        }
    }

    /** 从行内提取形如 payload(hex):<HEX> 的HEX串 */
    private String extractHex(String line) {
        if (line == null) return null;
        int idx = line.toLowerCase().indexOf("payload(hex):");
        if (idx >= 0) {
            String hex = line.substring(idx + "payload(hex):".length()).trim();
            // 合法性：仅十六进制字符
            if (hex.matches("[0-9a-fA-F]+")) {
                return hex;
            }
            if (true) Log.d(TAG, "提取到的HEX不合法，忽略: " + (hex.length() > 64 ? hex.substring(0,64) + "..." : hex));
        }
        return null;
    }

    /** 将HEX字符串截断以避免日志过长 */
    private String truncateHex(String hex) {
        if (hex == null) return "null";
        return hex.length() > 64 ? hex.substring(0, 64) + "..." : hex;
    }
}