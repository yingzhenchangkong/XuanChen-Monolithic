package com.xuanchen.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.datasource.DatasourceConnectionFactory;
import com.xuanchen.common.datasource.DatasourceProvider;
import com.xuanchen.common.datasource.QueryLimits;
import com.xuanchen.common.datasource.QueryRunner;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.SqlGuard;
import com.xuanchen.api.engine.StepDef;
import com.xuanchen.api.engine.TemplateEval;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.entity.ApiData;
import com.xuanchen.api.entity.LogApi;
import com.xuanchen.api.mapper.ApiDataMapper;
import com.xuanchen.api.mapper.LogApiMapper;
import com.xuanchen.api.mapper.ApiMapper;
import com.xuanchen.api.service.IApiInvokeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务实现-->数据接口服务-执行引擎（分发器）
 * 设计要点：
 * - 无脚本引擎，四类声明式步骤（sql/http/map/save），配置即能力；
 * - sql 步骤强制只读 SELECT + 原生 JDBC PreparedStatement（命名参数 :param → ?）；
 * - 路由缓存：Redis api:route:{code} 存已发布接口 JSON，变更时失效；
 * - 每次执行落 log_api（成功/失败、耗时）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Service
public class ApiInvokeServiceImpl implements IApiInvokeService {

    public static final String ROUTE_KEY_PREFIX = "api:route:";
    /** 触发方式：手动调用/定时采集/测试运行 */
    public static final int TRIGGER_MANUAL = 1;
    public static final int TRIGGER_SCHEDULE = 2;
    public static final int TRIGGER_TEST = 3;

    private static final long HTTP_CONNECT_TIMEOUT_MS = 5000;
    private static final long HTTP_READ_TIMEOUT_MS = 15000;

    @Autowired
    private ApiMapper apiMapper;
    @Autowired
    private ApiDataMapper dataMapper;
    @Autowired
    private LogApiMapper logApiMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private DatasourceProvider datasourceProvider;
    @Autowired
    private DatasourceConnectionFactory connectionFactory;
    @Autowired
    private QueryLimits queryLimits;

    private final RestTemplate restTemplate;

    public ApiInvokeServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) HTTP_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout((int) HTTP_READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public Api getPublishedByCode(String apiCode) {
        //先查 Redis 路由缓存
        String cacheKey = ROUTE_KEY_PREFIX + apiCode;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof String json && !json.isBlank()) {
            return JsonUtils.fromJson(json, Api.class);
        }
        //回源（@TableLogic 自动过滤已删）
        Api api = apiMapper.selectOne(new QueryWrapper<Api>().eq("api_code", apiCode));
        if (api == null || api.getStatus() == null || api.getStatus() != 1) {
            return null;
        }
        redisUtil.set(cacheKey, JsonUtils.toJson(api));
        return api;
    }

    /**
     * 路由缓存失效（配置变更/停用/删除时调用）
     */
    @Override
    public void evictRouteCache(String apiCode) {
        redisUtil.del(ROUTE_KEY_PREFIX + apiCode);
    }

    @Override
    public Map<String, Object> execute(Api api, Map<String, Object> params, int triggerType, boolean collect) {
        long start = System.currentTimeMillis();
        boolean success = true;
        String error = null;
        Object result = null;
        try {
            List<StepDef> steps = IApiInvokeService.parseSteps(api.getSteps());
            log.info("[API-EXEC] code={} steps={}", api.getApiCode(), steps.size());
            Map<String, Object> context = new HashMap<>();
            context.put("params", params == null ? Map.of() : params);
            for (StepDef step : steps) {
                long stepStart = System.currentTimeMillis();
                Object out = execStep(step, context, api);
                long stepCost = System.currentTimeMillis() - stepStart;
                context.put(step.getName(), out);
                // save / map 步骤属于副作用（落库/字段重组），不覆盖最终返回值；
                // sql / http 步骤是生产数据的源头，后者会覆盖前者
                if (!"save".equals(step.getType())) {
                    result = out;
                }
                log.info("[API-EXEC] step={}({}) cost={}ms outType={} outPreview={}",
                        step.getName(), step.getType(), stepCost,
                        out == null ? "null" : out.getClass().getSimpleName(),
                        preview(out));
            }
            log.info("[API-EXEC] code={} finalResultType={}", api.getApiCode(),
                    result == null ? "null" : result.getClass().getSimpleName());
            if (collect && result != null) {
                saveCollect(api.getApiCode(), result);
            }
        } catch (Exception e) {
            success = false;
            error = truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            log.error("[API-EXEC] 接口执行失败 code={}", api.getApiCode(), e);
        }
        long cost = System.currentTimeMillis() - start;
        writeLog(api.getApiCode(), triggerType, success, cost, error);
        if (!success) {
            throw new IllegalStateException(error);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("data", result);
        out.put("costMs", cost);
        return out;
    }

    // ---------------- 步骤执行 ----------------

    private Object execStep(StepDef step, Map<String, Object> context, Api api) {
        Map<String, Object> config = step.getConfig();
        return switch (step.getType()) {
            case "sql" -> execSql(step, config, context);
            case "http" -> execHttp(step, config, context);
            case "map" -> execMap(step, config, context);
            case "save" -> execSave(step, config, context, api);
            default -> throw new IllegalArgumentException("不支持的步骤类型：" + step.getType());
        };
    }

    /**
     * sql 步骤：仅允许 SELECT，参数走 PreparedStatement 绑定，取数统一委托 QueryRunner。
     * 支持 :param 与 #{param} 两种占位符；数据源：config.datasource 为 null/"main" → 主库，
     * 其他值 → sys_datasource 表的外部数据源 id。
     */
    private Object execSql(StepDef step, Map<String, Object> config, Map<String, Object> context) {
        String sql = String.valueOf(config.get("sql")).trim();
        Object[] extracted = SqlGuard.extractParams(sql);
        String finalSql = SqlGuard.validateSelect((String) extracted[0], queryLimits.getMaxRows());
        String[] paramNames = (String[]) extracted[1];

        // 解析命名参数映射（用户配置的 params Map，value 支持 {{xxx}} 模板）
        Map<String, Object> namedParams = new HashMap<>();
        Object rawParams = config.get("params");
        if (rawParams instanceof Map<?, ?> params) {
            for (Map.Entry<?, ?> e : params.entrySet()) {
                Object val = coerceScalar(TemplateEval.evalDeep(e.getValue(), context));
                namedParams.put(String.valueOf(e.getKey()), val);
            }
        }
        List<Object> orderedParams = new ArrayList<>();
        for (String name : paramNames) {
            if (!namedParams.containsKey(name)) {
                throw new IllegalArgumentException("sql 步骤 [" + step.getName() + "] 缺少命名参数 :" + name);
            }
            orderedParams.add(namedParams.get(name));
        }

        Object dsField = config.get("datasource");
        String dsId = dsField == null ? null : String.valueOf(dsField);
        boolean isMain = dsId == null || dsId.isBlank() || "main".equalsIgnoreCase(dsId);

        log.info("[API-EXEC] sql step={} datasource={} finalSql={} params={}",
                step.getName(), isMain ? "main" : dsId, finalSql, orderedParams);

        try (Connection conn = resolveConnection(dsId, isMain)) {
            QueryRunner.QueryResult result = QueryRunner.run(conn, finalSql, orderedParams,
                    queryLimits.getMaxRows(), queryLimits.getTimeoutSeconds());
            log.info("[API-EXEC] sql rows={}", result.rows().size());
            return result.rows();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("sql 步骤 [" + step.getName() + "] 执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按数据源配置获取 JDBC 连接。
     */
    private Connection resolveConnection(String dsId, boolean isMain) throws Exception {
        if (isMain) {
            return dataSource.getConnection();
        }
        DatasourceInfo ds = datasourceProvider.get(dsId);
        log.info("[API-EXEC] sql step using external datasource={}({})", ds.getDsName(), ds.getJdbcUrl());
        return connectionFactory.getConnection(ds.getId(), ds);
    }

    /** 日志预览：Map/List 取首元素摘要 */
    private String preview(Object out) {
        if (out == null) return "null";
        if (out instanceof List<?> list) {
            if (list.isEmpty()) return "[]";
            String first = JsonUtils.toJson(list.get(0));
            return "[" + list.size() + "] first=" + (first.length() > 200 ? first.substring(0, 200) + "..." : first);
        }
        String s = JsonUtils.toJson(out);
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    /**
     * 标量纠偏：URL/表单入参恒为字符串，而 LIMIT/数值比较列需要数字——
     * 纯整数字符串转 Long，纯小数转 Double，其余保持原样
     */
    private static Object coerceScalar(Object v) {
        if (v instanceof String s) {
            String t = s.trim();
            if (t.matches("-?\\d{1,18}")) {
                try {
                    return Long.valueOf(t);
                } catch (NumberFormatException ignore) {
                    //超出 Long 范围则按字符串处理
                }
            }
            if (t.matches("-?\\d+\\.\\d+")) {
                try {
                    return Double.valueOf(t);
                } catch (NumberFormatException ignore) {
                    //忽略
                }
            }
        }
        return v;
    }

    /**
     * http 步骤：从第三方 API 拉取数据（GET query / POST JSON）
     */
    private Object execHttp(StepDef step, Map<String, Object> config, Map<String, Object> context) {
        String url = String.valueOf(TemplateEval.evalDeep(config.get("url"), context));
        String method = String.valueOf(config.get("method")).trim().toUpperCase();
        HttpHeaders headers = new HttpHeaders();
        Object rawHeaders = config.get("headers");
        if (rawHeaders instanceof Map<?, ?> headerMap) {
            for (Map.Entry<?, ?> e : headerMap.entrySet()) {
                headers.set(String.valueOf(e.getKey()), String.valueOf(TemplateEval.evalDeep(e.getValue(), context)));
            }
        }
        Object resultBody;
        if ("POST".equals(method)) {
            Object body = TemplateEval.evalDeep(config.get("body"), context);
            if (headers.getContentType() == null) {
                headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
            }
            resultBody = exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers));
        } else if ("GET".equals(method)) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            Object rawParams = config.get("params");
            if (rawParams instanceof Map<?, ?> paramMap) {
                for (Map.Entry<?, ?> e : paramMap.entrySet()) {
                    builder.queryParam(String.valueOf(e.getKey()), String.valueOf(TemplateEval.evalDeep(e.getValue(), context)));
                }
            }
            URI uri = builder.build().encode().toUri();
            resultBody = exchange(uri.toString(), HttpMethod.GET, new HttpEntity<>(headers));
        } else {
            throw new IllegalArgumentException("http 步骤 [" + step.getName() + "] 仅支持 GET/POST！");
        }
        return parseResponseBody(step.getName(), (String) resultBody);
    }

    private String exchange(String url, HttpMethod method, HttpEntity<?> entity) {
        ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, new ParameterizedTypeReference<>() {
        });
        return resp.getBody() == null ? "" : resp.getBody();
    }

    /**
     * 响应体解析：JSON 则转 Map/List，否则原样字符串
     */
    private Object parseResponseBody(String stepName, String body) {
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return Map.of();
        }
        char first = trimmed.charAt(0);
        try {
            if (first == '{' || first == '[') {
                if (first == '{') {
                    return JsonUtils.fromJson(trimmed, new TypeReference<Map<String, Object>>() {
                    });
                }
                return JsonUtils.fromJson(trimmed, new TypeReference<List<Object>>() {
                });
            }
        } catch (Exception e) {
            log.warn("http 步骤 [{}] 响应非合法 JSON，按字符串返回", stepName);
        }
        return trimmed;
    }

    /**
     * map 步骤：字段映射/重组（源为数组则逐行映射，对象则单对象映射）
     */
    @SuppressWarnings("unchecked")
    private Object execMap(StepDef step, Map<String, Object> config, Map<String, Object> context) {
        String sourceName = String.valueOf(config.get("source"));
        Object source = TemplateEval.getByPath(context, sourceName);
        boolean keepSource = Boolean.TRUE.equals(config.get("keepSource"))
                || "true".equals(String.valueOf(config.get("keepSource")));
        List<Map<String, Object>> fields = extractFields(config.get("fields"));

        if (source instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object item : list) {
                out.add(mapOne(item, keepSource, fields, step.getName()));
            }
            return out;
        }
        return mapOne(source, keepSource, fields, step.getName());
    }

    private Object mapOne(Object item, boolean keepSource, List<Map<String, Object>> fields, String stepName) {
        if (!(item instanceof Map)) {
            return item;
        }
        Map<String, Object> out = new HashMap<>();
        if (keepSource) {
            out.putAll((Map<String, Object>) item);
        }
        for (Map<String, Object> field : fields) {
            String from = String.valueOf(field.get("from"));
            String to = String.valueOf(field.get("to"));
            out.put(to, TemplateEval.getByPath(item, from));
        }
        return out;
    }

    private List<Map<String, Object>> extractFields(Object raw) {
        List<Map<String, Object>> fields = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> f = new HashMap<>();
                    m.forEach((k, v) -> f.put(String.valueOf(k), v));
                    fields.add(f);
                }
            }
        }
        return fields;
    }

    /**
     * save 步骤：将源结果 JSON 逐条落 api_data（数据采集归档）
     */
    private Object execSave(StepDef step, Map<String, Object> config, Map<String, Object> context, Api api) {
        String sourceName = String.valueOf(config.get("source"));
        Object source = TemplateEval.getByPath(context, sourceName);
        int saved = saveCollect(api.getApiCode(), source);
        Map<String, Object> out = new HashMap<>();
        out.put("saved", saved);
        return out;
    }

    /**
     * 采集落库：数组逐条存，非空对象整条存；空 Map / null 跳过不存
     */
    private int saveCollect(String apiCode, Object result) {
        List<Object> records = new ArrayList<>();
        if (result instanceof List<?> list) {
            records.addAll(list);
        } else if (result instanceof Map<?, ?> map && !map.isEmpty()) {
            records.add(result);
        } else if (result != null && !(result instanceof Map<?, ?>)) {
            records.add(result);
        }
        if (records.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Object record : records) {
            ApiData row = new ApiData();
            row.setApiCode(apiCode);
            row.setData(JsonUtils.toJson(record));
            row.setCreateTime(now);
            dataMapper.insert(row);
        }
        return records.size();
    }

    private void writeLog(String apiCode, int triggerType, boolean success, long costMs, String error) {
        try {
            LogApi row = new LogApi();
            row.setApiCode(apiCode);
            row.setTriggerType(triggerType);
            row.setSuccess(success ? 1 : 0);
            row.setCostMs((int) costMs);
            row.setErrorMsg(error);
            row.setCreateTime(LocalDateTime.now());
            logApiMapper.insert(row);
        } catch (Exception e) {
            log.warn("写接口调用日志失败 code={}", apiCode, e);
        }
    }

    private String truncate(String s) {
        return s.length() > 480 ? s.substring(0, 480) : s;
    }

}
