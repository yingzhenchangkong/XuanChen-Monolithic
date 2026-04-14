package com.xuanchen.common.fastexcel;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener-->FastExcel
 *
 * @author XuanChen
 * @date 2025-04-07
 */
public class FastExcelListener<T> extends AnalysisEventListener<T> {
    //用于存储读取到的Excel数据对象列表
    private List<T> dataList = new ArrayList<T>();

    @Override
    public void invoke(T t, AnalysisContext analysisContext) {
        //每读取一行数据，就将其添加到dataList中
        dataList.add(t);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 当所有数据读取完成后，可以在这里进行一些后续操作，如打印读取到的数据数量
        //System.out.println("读取完成，共读取了 " + dataList.size() + " 条数据");
    }

    //提供一个方法用于获取存储数据的列表
    public List<T> getDataList() {
        return dataList;
    }
}
