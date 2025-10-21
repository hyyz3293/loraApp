package com.lora.cn.network;

import android.util.Log;

import com.blankj.utilcode.util.SPUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.RequestBody;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * 基于第三方库的网关数据交互客户端：
 * - 使用 OkHttp 进行 HTTP/HTTPS 请求
 * - 使用 Jsoup 解析 HTML 页面（可从 Packets 页面提取字段）
 * - 使用 Gson 解析 JSON 接口（若网关提供 REST API）
 */
public class GatewayPacketsClient {

    private static final String TAG = "GatewayPacketsClient";
    private static final java.util.regex.Pattern HEX_PATTERN = java.util.regex.Pattern.compile("([0-9a-fA-F]{32,})");

    public static class PacketRecord {
        public String time;
        public String deviceId;
        public String devAddr;
        public String gateway;
        public Double freq;
        public String dr;
        public Integer fcnt;
        public Integer fport;
        public Integer rssi;
        public Double snr;
        public String payloadHex;
        public String mic;
        public String rawLine; // 解析不全时保留原始行片段
    }

    public interface PacketsListener {
        void onStatus(String msg);
        //void onMsg(String msg);
        void onPackets(List<PacketRecord> records);
        void onError(String error);
        void onComplete();
    }

    private final OkHttpClient client;

    public GatewayPacketsClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    /**
     * 尝试获取 Packets 数据：优先尝试 JSON 接口，其次解析 HTML 页面。
     * @param ip 网关IP，如 192.168.1.1
     */
    public void fetchPackets(String ip, PacketsListener listener) {
        try {
            String configuredPath = SPUtils.getInstance().getString("packets_api_path", "/api/networkserver/packets");
            String cookie = SPUtils.getInstance().getString("gateway_cookie", "");
            String token = SPUtils.getInstance().getString("gateway_auth_token", "");
            String username = SPUtils.getInstance().getString("gateway_username", "");
            String password = SPUtils.getInstance().getString("gateway_password", "");
            String basicAuth = (!username.isEmpty() && !password.isEmpty())
                    ? ("Basic " + android.util.Base64.encodeToString((username + ":" + password).getBytes(), android.util.Base64.NO_WRAP))
                    : "";

            String[] ports = new String[]{"80", "443", "8080"};
            boolean[] httpsFlags = new boolean[]{false, true, false};
            String[] candidates = new String[]{
                    configuredPath,
                    "/api/networkserver/packets",
                    "/networkserver/packets",
                    "/api/packets",
                    "/packets",
                    "/"
            };

            // 先尝试 JSON 接口
            for (int p = 0; p < ports.length; p++) {
                for (String path : candidates) {
                    HttpUrl url = buildUrl(ip, ports[p], httpsFlags[p], path);
                    if (url == null) continue;
                    Request.Builder rb = new Request.Builder().url(url);
                    rb.header("User-Agent", "Mozilla/5.0 (Android; Mobile) LoRaApp");
                    rb.header("Accept", "application/json, text/html;q=0.9, */*;q=0.8");
                    if (!cookie.isEmpty()) rb.header("Cookie", cookie);
                    if (!token.isEmpty()) {
                        rb.header("Authorization", token);
                    } else if (!basicAuth.isEmpty()) {
                        rb.header("Authorization", basicAuth);
                    }

                    listener.onStatus("请求：" + url);
                    OkHttpClient chosenClient = url.isHttps() ? buildUnsafeHttpsClient() : client;
                    try (Response resp = chosenClient.newCall(rb.build()).execute()) {
                        int code = resp.code();
                        String ct = resp.header("Content-Type", "");
                        ResponseBody body = resp.body();
                        if (body == null) continue;
                        String text = body.string();

                        // 判断是否JSON
                        boolean looksJson = (ct != null && ct.toLowerCase().contains("json"))
                                || (!text.isEmpty() && (text.trim().startsWith("{") || text.trim().startsWith("[")));
                        if (code == 401 || code == 403) {
                            listener.onError("网关接口返回" + code + "，需要登录或权限不足。请在设置-登录网关后重试。");
                            return;
                        }
                        if (code >= 200 && code < 300 && looksJson) {
                            listener.onStatus("检测到JSON接口，开始解析...");
                            List<PacketRecord> records = parseJson(text);
                            if (!records.isEmpty()) {
                                listener.onPackets(records);
                                listener.onComplete();
                                return;
                            } else {
                                listener.onStatus("JSON解析未得到有效记录，尝试其他路径...");
                            }
                        } else if (code >= 200 && code < 300 && (ct != null && ct.toLowerCase().contains("html"))) {
                            // 解析 HTML（可能为前端路由页，但有时含静态表）
                            listener.onStatus("收到HTML内容，尝试解析页面表格...");
                            List<PacketRecord> records = parseHtml(text);
                            if (!records.isEmpty()) {
                                listener.onPackets(records);
                                listener.onComplete();
                                return;
                            } else {
                                listener.onStatus("HTML页面未提取到有效数据，继续尝试其它端口/路径...");
                            }
                        } else {
                            listener.onStatus("响应码=" + code + ", Content-Type=" + ct + "，继续尝试...");
                        }
                    } catch (IOException e) {
                        listener.onStatus("请求失败：" + e.getMessage());
                    }
                }
            }

            listener.onError("未能从网关获取Packets数据，请在浏览器F12中确认具体API路径，并配置到SPUtils键 'packets_api_path'");
        } catch (Exception e) {
            Log.e(TAG, "fetchPackets error", e);
            listener.onError("获取异常：" + e.getMessage());
        } finally {
            listener.onComplete();
        }
    }

    private HttpUrl buildUrl(String ip, String port, boolean https, String path) {
        if (ip == null || ip.isEmpty()) return null;
        String scheme = https ? "https" : "http";
        String cleanPath = (path == null) ? "/" : path.trim();
        if (!cleanPath.startsWith("/")) cleanPath = "/" + cleanPath;
        // 移除hash片段，避免'#networkserver/packets'被忽略
        int idxHash = cleanPath.indexOf('#');
        if (idxHash >= 0) cleanPath = cleanPath.substring(0, idxHash);
        return new HttpUrl.Builder()
                .scheme(scheme)
                .host(ip)
                .port(Integer.parseInt(port))
                .encodedPath(cleanPath)
                .build();
    }

    // -------------------- 解析 JSON --------------------
    private List<PacketRecord> parseJson(String text) {
        List<PacketRecord> list = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(text);
            JsonArray arr = null;
            if (root.isJsonArray()) {
                arr = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                // 常见字段：packets/list/data/items
                if (obj.has("packets") && obj.get("packets").isJsonArray()) {
                    arr = obj.get("packets").getAsJsonArray();
                } else if (obj.has("list") && obj.get("list").isJsonArray()) {
                    arr = obj.get("list").getAsJsonArray();
                } else if (obj.has("data")) {
                    JsonElement de = obj.get("data");
                    if (de.isJsonArray()) arr = de.getAsJsonArray();
                    else if (de.isJsonObject() && de.getAsJsonObject().has("items")) {
                        arr = de.getAsJsonObject().get("items").getAsJsonArray();
                    }
                }
            }
            if (arr == null) return list;

            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                PacketRecord r = new PacketRecord();
                r.deviceId = getString(o, "devEUI", "devEui", "deveui", "deviceId", "devEuiHex");
                r.devAddr  = getString(o, "devAddr", "devaddr");
                r.payloadHex = getString(o, "payload", "phyPayload", "data", "macPayload", "frmPayload");
                r.gateway = getString(o, "gateway", "gw", "rxGateway", "rxInfo.gatewayId");
                r.mic = getString(o, "mic");
                r.dr = getString(o, "dr", "dataRate", "datarate");
                r.time = getString(o, "time", "rxTime", "ts", "timestamp");
                r.fcnt = getInt(o, "fcnt", "fCnt", "frameCounter");
                r.fport = getInt(o, "fport", "fPort");
                r.rssi = getInt(o, "rssi");
                r.snr  = getDouble(o, "snr");
                r.freq = getDouble(o, "frequency", "freq");
                r.rawLine = o.toString();
                list.add(r);
            }
        } catch (Exception e) {
            Log.w(TAG, "parseJson失败: " + e.getMessage());
        }
        return list;
    }

    private String getString(JsonObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.get(k).isJsonNull()) return o.get(k).getAsString();
        }
        return null;
    }
    private Integer getInt(JsonObject o, String... keys) {
        for (String k : keys) {
            try {
                if (o.has(k) && !o.get(k).isJsonNull()) return o.get(k).getAsInt();
            } catch (Exception ignored) {}
        }
        return null;
    }
    private Double getDouble(JsonObject o, String... keys) {
        for (String k : keys) {
            try {
                if (o.has(k) && !o.get(k).isJsonNull()) return o.get(k).getAsDouble();
            } catch (Exception ignored) {}
        }
        return null;
    }

    // -------------------- 解析 HTML --------------------
    private List<PacketRecord> parseHtml(String html) {
        List<PacketRecord> list = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(html);
            // 优先尝试表格
            Elements tables = doc.select("table");
            for (Element table : tables) {
                // 读取头部
                List<String> headers = new ArrayList<>();
                Elements ths = table.select("thead th");
                for (Element th : ths) headers.add(th.text().toLowerCase());
                if (headers.isEmpty()) {
                    // 某些页面无thead，直接扫描第一行
                    Elements firstRow = table.select("tr");
                    if (!firstRow.isEmpty()) {
                        Elements tds = firstRow.get(0).select("td, th");
                        for (Element td : tds) headers.add(td.text().toLowerCase());
                    }
                }
                // 逐行解析
                Elements rows = table.select("tbody tr");
                if (rows.isEmpty()) rows = table.select("tr");
                for (Element tr : rows) {
                    Elements cells = tr.select("td, th");
                    if (cells.size() < 4) continue; // 过于短的行忽略
                    PacketRecord r = new PacketRecord();
                    String rowText = tr.text();
                    r.rawLine = rowText;
                    // 尝试按列名匹配
                    for (int i = 0; i < cells.size(); i++) {
                        String val = cells.get(i).text();
                        String key = i < headers.size() ? headers.get(i) : "";
                        if (key.contains("dev")) r.deviceId = val;
                        else if (key.contains("addr")) r.devAddr = val;
                        else if (key.contains("payload") || key.contains("data")) r.payloadHex = val;
                        else if (key.contains("rssi")) try { r.rssi = Integer.parseInt(val); } catch (Exception ignored) {}
                        else if (key.contains("snr")) try { r.snr = Double.parseDouble(val); } catch (Exception ignored) {}
                        else if (key.contains("fport")) try { r.fport = Integer.parseInt(val); } catch (Exception ignored) {}
                        else if (key.contains("fcnt") || key.contains("frame")) try { r.fcnt = Integer.parseInt(val); } catch (Exception ignored) {}
                        else if (key.contains("freq")) try { r.freq = Double.parseDouble(val); } catch (Exception ignored) {}
                        else if (key.contains("dr")) r.dr = val;
                        else if (key.contains("time")) r.time = val;
                        else if (key.contains("gw")) r.gateway = val;
                    }
                    // 如果某些关键字段未命中，再做关键词兜底
                    if (r.deviceId == null && rowText.toLowerCase().contains("deveui")) {
                        r.deviceId = extractAfter(rowText, "DevEUI");
                    }
                    if (r.payloadHex == null && rowText.toLowerCase().contains("payload")) {
                        r.payloadHex = extractAfter(rowText, "Payload");
                    }
                    list.add(r);
                }
            }

            // 若表格未提取到，尝试解析 <pre>/<code> 文本块
            if (list.isEmpty()) {
                for (Element block : doc.select("pre, code")) {
                    String[] lines = block.text().split("\n");
                    for (String line : lines) {
                        String t = line.trim();
                        if (t.isEmpty()) continue;
                        PacketRecord r = new PacketRecord();
                        r.rawLine = t;
                        if (t.toLowerCase().contains("deveui")) r.deviceId = extractAfter(t, "DevEUI");
                        if (t.toLowerCase().contains("devaddr")) r.devAddr = extractAfter(t, "DevAddr");
                        if (t.toLowerCase().contains("payload")) r.payloadHex = extractAfter(t, "Payload");
                        list.add(r);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "parseHtml失败: " + e.getMessage());
        }
        return list;
    }

    private String extractAfter(String text, String key) {
        try {
            int idx = text.toLowerCase().indexOf(key.toLowerCase());
            if (idx < 0) return null;
            String sub = text.substring(idx + key.length());
            sub = sub.replace(":", "").trim();
            // 截取到下一个空格/逗号
            int end = Math.max(0, Math.min(
                    indexOrLen(sub, ' '),
                    Math.min(indexOrLen(sub, ','), indexOrLen(sub, ';'))
            ));
            if (end == 0) return sub;
            return sub.substring(0, end).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private int indexOrLen(String s, char ch) {
        int i = s.indexOf(ch);
        return i >= 0 ? i : s.length();
    }

    // 使用 Basic 认证直接连接数据流
    public void fetchPacketsStreamBasic(String ip, String username, String password, PacketsListener listener) {
        try {
            String credential = Credentials.basic(username, password);
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(ip)
                    .port(8080)
                    .addPathSegments("networkserver/packets")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", credential)
                    .build();

            listener.onStatus("开始连接数据流: " + url);
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onStatus("数据流已连接: " + url);
                        InputStream inputStream = response.body().byteStream();
                        readDataStream(inputStream, listener);
                    } else {
                        listener.onError("数据流响应异常: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    listener.onError("连接数据流失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onError("构建请求失败: " + e.getMessage());
        }
    }

    // 先登录获取 Cookie，再连接数据流
    public void loginAndFetchPacketsStream(String ip, String username, String password, PacketsListener listener) {
        OkHttpClient cookieClient = client.newBuilder()
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                })
                .build();

        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        HttpUrl loginUrl = new HttpUrl.Builder()
                .scheme("http")
                .host(ip)
                .addPathSegment("login")
                .build();

        Request loginRequest = new Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .build();

        listener.onStatus("开始登录: " + loginUrl);
        cookieClient.newCall(loginRequest).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    listener.onStatus("登录成功，开始拉取数据流");

                    HttpUrl dataUrl = new HttpUrl.Builder()
                            .scheme("http")
                            .host(ip)
                            .port(8080)
                            .addPathSegments("networkserver/packets")
                            .build();

                    Request dataRequest = new Request.Builder()
                            .url(dataUrl)
                            .build();

                    cookieClient.newCall(dataRequest).enqueue(new Callback() {
                        @Override
                        public void onResponse(Call call2, Response response2) throws IOException {
                            if (response2.isSuccessful() && response2.body() != null) {
                                listener.onStatus("数据流已连接: " + dataUrl);
                                InputStream inputStream = response2.body().byteStream();
                                readDataStream(inputStream, listener);
                            } else {
                                listener.onError("数据流响应异常: " + response2.code());
                            }
                        }

                        @Override
                        public void onFailure(Call call2, IOException e) {
                            listener.onError("拉取数据流失败: " + e.getMessage());
                        }
                    });

                } else {
                    listener.onError("登录失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                listener.onError("登录请求失败: " + e.getMessage());
            }
        });
    }

    // 读取数据流，提取 HEX 载荷并回调
    private void readDataStream(InputStream inputStream, PacketsListener listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;

                List<PacketRecord> batch = new ArrayList<>();
                Matcher m = HEX_PATTERN.matcher(t);
                while (m.find()) {
                    String hex = m.group(1);
                    PacketRecord r = new PacketRecord();
                    r.payloadHex = hex;
                    r.rawLine = t;
                    batch.add(r);
                }
                if (!batch.isEmpty()) {
                    listener.onPackets(batch);
                }
            }
            listener.onComplete();
        } catch (Exception e) {
            listener.onError("数据流读取失败: " + e.getMessage());
        }
    }

    // 构建信任所有证书的 HTTPS 客户端（仅用于局域网设备自签证书登录）
    private OkHttpClient buildUnsafeHttpsClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            X509TrustManager trustManager = (X509TrustManager) trustAllCerts[0];
            return client.newBuilder()
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            Log.w(TAG, "构建不安全HTTPS客户端失败，回退默认客户端: " + e.getMessage());
            return client;
        }
    }

    // 通过 HTTPS 登录页（https://<ip>/login.html），登录成功后在 HTTP 流端点获取数据
    public void loginHttpsAndFetchPacketsStream(String ip, String username, String password, PacketsListener listener) {
        OkHttpClient httpsCookieClient = buildUnsafeHttpsClient().newBuilder()
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
                    @Override public void saveFromResponse(HttpUrl url, List<Cookie> cookies) { cookieStore.put(url.host(), cookies); }
                    @Override public List<Cookie> loadForRequest(HttpUrl url) { List<Cookie> c = cookieStore.get(url.host()); return c != null ? c : new ArrayList<>(); }
                })
                .build();

        HttpUrl loginPageUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(ip)
                .addPathSegments("login.html")
                .build();

        Request loginPageReq = new Request.Builder().url(loginPageUrl).get().build();
        listener.onStatus("访问登录页: " + loginPageUrl);
        httpsCookieClient.newCall(loginPageReq).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onError("登录页访问失败: " + response.code());
                    return;
                }
                String html = response.body() != null ? response.body().string() : "";
                String action = null;
                try {
                    Document doc = Jsoup.parse(html);
                    Element form = doc.selectFirst("form");
                    if (form != null) {
                        action = form.hasAttr("action") ? form.attr("action") : null;
                    }
                } catch (Exception ignore) {}

                // 构建候选登录提交地址
                List<HttpUrl> candidates = new ArrayList<>();
                HttpUrl base = new HttpUrl.Builder().scheme("https").host(ip).build();
                if (action != null && !action.isEmpty()) {
                    try {
                        HttpUrl candidate = base.newBuilder().addPathSegments(action.replaceFirst("^/", "")).build();
                        candidates.add(candidate);
                    } catch (Exception ignore) {}
                }
                candidates.add(new HttpUrl.Builder().scheme("https").host(ip).addPathSegment("login").build());
                candidates.add(loginPageUrl);

                // 逐个尝试登录（POST 表单）
                boolean loginOk = false;
                for (HttpUrl url : candidates) {
                    try {
                        RequestBody formBody = new FormBody.Builder()
                                .add("username", username)
                                .add("password", password)
                                .build();
                        Request loginReq = new Request.Builder().url(url).post(formBody).build();
                        Response loginResp = httpsCookieClient.newCall(loginReq).execute();
                        int code = loginResp.code();
                        if (code >= 200 && code < 400) {
                            listener.onStatus("登录成功: " + url + " code=" + code);
                            loginOk = true;
                            break;
                        } else {
                            listener.onStatus("登录尝试失败: " + url + " code=" + code);
                        }
                    } catch (Exception e) {
                        listener.onStatus("登录请求异常: " + url + " err=" + e.getMessage());
                    }
                }

                if (!loginOk) {
                    listener.onError("所有候选登录地址均失败");
                    return;
                }

                // 登录成功后，连接 HTTP 数据流（携带 Cookie 与 Basic 作为兜底）
                HttpUrl dataUrl = new HttpUrl.Builder()
                        .scheme("http")
                        .host(ip)
                        .port(8080)
                        .addPathSegments("networkserver/packets")
                        .build();
                String credential = Credentials.basic(username, password);
                Request dataReq = new Request.Builder()
                        .url(dataUrl)
                        .header("Authorization", credential)
                        .build();
                listener.onStatus("开始连接数据流: " + dataUrl);
                httpsCookieClient.newCall(dataReq).enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call2, Response resp2) throws IOException {
                        if (resp2.isSuccessful() && resp2.body() != null) {
                            listener.onStatus("数据流已连接: " + dataUrl);
                            readDataStream(resp2.body().byteStream(), listener);
                        } else {
                            listener.onError("数据流响应异常: " + resp2.code());
                        }
                    }
                    @Override
                    public void onFailure(Call call2, IOException e) { listener.onError("拉取数据流失败: " + e.getMessage()); }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) { listener.onError("登录页请求失败: " + e.getMessage()); }
        });
    }

    // 智能拉流：优先 Basic，失败后走 HTTPS 登录页再拉流
    public void fetchPacketsStreamSmart(String ip, String username, String password, PacketsListener listener) {
        PacketsListener wrapper = new PacketsListener() {
            @Override public void onStatus(String msg) { listener.onStatus(msg); }
            @Override public void onPackets(List<PacketRecord> records) { listener.onPackets(records); }
            @Override public void onComplete() { listener.onComplete(); }
            @Override public void onError(String error) {
                listener.onStatus("Basic 模式失败，切换 HTTPS 登录再拉流: " + error);
                loginHttpsAndFetchPacketsStream(ip, username, password, listener);
            }
        };
        fetchPacketsStreamBasic(ip, username, password, wrapper);
    }
}