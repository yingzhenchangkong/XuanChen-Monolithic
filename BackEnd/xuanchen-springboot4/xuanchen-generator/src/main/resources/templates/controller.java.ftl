package ${package.Controller};

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.excel.FesodSheetExportUtil;
import com.xuanchen.common.excel.FesodSheetListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.fesod.sheet.FesodSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ${package.Entity}.${entity};
import ${package.Service}.${table.serviceName};

/**
* 控制器 --> ${tableComment}
*
* @author ${author}
* @date ${date}
*/
@RestController
@RequestMapping("${urlBase}")
public class ${table.controllerName} {

<#assign pkColumn = "id" pkProperty = "id" pkType = "String"/>
<#list table.fields as tf>
    <#if tf.keyFlag>
        <#assign pkColumn = tf.name pkProperty = tf.propertyName pkType = tf.propertyType/>
    </#if>
</#list>
<#assign serviceField = table.serviceName/>
<#if serviceField?starts_with("I") && serviceField?length gt 1 && serviceField?substring(1,2)?matches("[A-Z]")>
    <#assign serviceField = serviceField?substring(1)?uncap_first/>
</#if>

    @Autowired
    private ${table.serviceName} ${serviceField};

    /**
    * 列表 分页查询
    *
    * @param ${entityVar} 查询条件（按代码生成器字段配置拼装）
    * @param pageNo 页码
    * @param pageSize 每页条数
    * @return 分页结果
    */
    @GetMapping("/list")
    public Result<IPage<${entity}>> list(${entity} ${entityVar},
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<${entity}> queryWrapper = new QueryWrapper<>();
<#list queryFields as f>
        // ${f.comment}
        queryWrapper.${f.queryOp}(${entityVar}.get${f.property?cap_first}() != null,
                "${f.columnName}", ${entityVar}.get${f.property?cap_first}());
</#list>
<#if hasCreateTime>
        queryWrapper.orderByDesc("create_time");
<#else>
        queryWrapper.orderByAsc("${pkColumn}");
</#if>
        Page<${entity}> page = new Page<>(pageNo, pageSize);
        return Result.success(${serviceField}.page(page, queryWrapper));
    }

    /**
    * 添加
    *
    * @param ${entityVar} 实体
    * @return 结果
    */
    @PostMapping("/add")
    public Result<String> add(@RequestBody ${entity} ${entityVar}) {
        ${serviceField}.save(${entityVar});
        // 返回新记录主键：一对一主子表保存完主表后需要立即用它保存子表。
        // 注意 Result.success(字符串) 会命中 success(String msg) 重载把主键当提示语，必须两参形式
        return Result.success(TipConst.ADD_SUCC, String.valueOf(${entityVar}.get${pkProperty?cap_first}()));
    }

    /**
    * 修改
    *
    * @param ${entityVar} 实体
    * @return 结果
    */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody ${entity} ${entityVar}) {
        if (${entityVar}.get${pkProperty?cap_first}() == null<#if pkType == "String"> || ${entityVar}.get${pkProperty?cap_first}().isBlank()</#if>) {
            return Result.badRequest("缺少主键ID！");
        }
        // 影响行数为 0（id 不存在或已被删除）必须返回失败，避免前端误判成功
        if (!${serviceField}.updateById(${entityVar})) {
            return Result.badRequest("记录不存在或已被删除！");
        }
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
    * 通过id删除
    *
    * @param id 主键
    * @return 结果
    */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        ${serviceField}.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
    * 通过id批量删除
    *
    * @param ids 逗号分隔主键
    * @return 结果
    */
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        ${serviceField}.removeByIds(Arrays.asList(ids.split(",")));
        return Result.success(TipConst.DEL_BATCH_SUCC);
    }

    /**
    * 导出excel
    *
    * @param response 响应
    * @throws IOException 写出失败
    */
    @GetMapping(value = "/exportExcel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        FesodSheetExportUtil.export(response, "${tableComment}", ${entity}.class, e -> String.valueOf(e.get${pkProperty?cap_first}()), (lastId, batchSize) -> {
            QueryWrapper<${entity}> queryWrapper = new QueryWrapper<>();
            if (lastId != null) {
                queryWrapper.gt("${pkColumn}", lastId);
            }
            queryWrapper.orderByAsc("${pkColumn}");
            return ${serviceField}.page(FesodSheetExportUtil.cursorPage(batchSize), queryWrapper).getRecords();
        });
    }

    /**
    * 通过excel导入数据
    *
    * @param request 请求
    * @param response 响应
    * @return 导入结果
    * @throws IOException 读取失败
    */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<String> importExcel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        String message = "";
        for (Map.Entry<String, MultipartFile> item : fileMap.entrySet()) {
            MultipartFile multipartFile = item.getValue();
            InputStream inputStream = multipartFile.getInputStream();
            FesodSheetListener<${entity}> listener = new FesodSheetListener<>();
            FesodSheet.read(inputStream, ${entity}.class, listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<${entity}> dataList = listener.getDataList();
            //无有效行时不发批量 INSERT
            if (!dataList.isEmpty()) {
                ${serviceField}.saveBatch(dataList);
            }
            message = "共" + dataList.size() + "条数据,成功：" + dataList.size() + "条数据";
        }
        return Result.success(message);
    }
<#if isSubTable>

    /**
    * 按主表ID查询子表记录（主子表模式，主表编辑弹窗内调用）
    *
    * @param mainId 主表主键
    * @return 子表记录列表
    */
    @GetMapping("/listByMainId")
    public Result<List<${entity}>> listByMainId(@RequestParam(name = "mainId", required = true) String mainId) {
        QueryWrapper<${entity}> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("${subFkColumn}", mainId);
        queryWrapper.orderByAsc("${pkColumn}");
        return Result.success(${serviceField}.list(queryWrapper));
    }
</#if>
}
