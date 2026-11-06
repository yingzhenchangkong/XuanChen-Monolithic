package com.xuanchen.visualization.visscreen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.visualization.visscreen.entity.VisDataset;
import com.xuanchen.visualization.visscreen.entity.VisScreen;
import com.xuanchen.visualization.visscreen.mapper.VisDatasetMapper;
import com.xuanchen.visualization.visscreen.mapper.VisScreenMapper;
import com.xuanchen.visualization.visscreen.service.IVisScreenService;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service接口实现类-->数据大屏
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Service
public class VisScreenServiceImpl extends ServiceImpl<VisScreenMapper, VisScreen> implements IVisScreenService {

    @Autowired
    private VisScreenMapper visScreenMapper;
    @Autowired
    private VisDatasetMapper visDatasetMapper;

    @Override
    public IPage<VisScreen> listScreen(IPage<VisScreen> page, VisScreen screen) {
        return visScreenMapper.listScreen(page, screen);
    }

    @Override
    public boolean ifExistsCode(VisScreen screen) {
        QueryWrapper<VisScreen> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("screen_code", screen.getScreenCode());
        if (StringUtil.isNotEmpty(screen.getId())) {
            queryWrapper.ne("id", screen.getId());
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String copyScreen(String id, String newCode, String newName, String userName) {
        VisScreen source = this.getById(id);
        if (source == null) {
            throw new BusinessException(400, "源大屏不存在或已被删除！");
        }
        VisScreen copy = new VisScreen();
        copy.setScreenCode(newCode);
        copy.setScreenName(newName);
        copy.setConfigJson(source.getConfigJson());
        copy.setThumbnail(source.getThumbnail());
        copy.setStatus(0);
        copy.setRemark(source.getRemark());
        copy.setCreateBy(userName);
        copy.setCreateTime(LocalDateTime.now());
        this.save(copy);

        // 数据集随大屏复制：组件按 datasetKey 引用，编码保持不变即可直接复用
        QueryWrapper<VisDataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("screen_id", source.getId());
        for (VisDataset ds : visDatasetMapper.selectList(queryWrapper)) {
            VisDataset item = new VisDataset();
            item.setScreenId(copy.getId());
            item.setDatasetKey(ds.getDatasetKey());
            item.setDatasetName(ds.getDatasetName());
            item.setSqlText(ds.getSqlText());
            item.setRemark(ds.getRemark());
            item.setCreateBy(userName);
            item.setCreateTime(LocalDateTime.now());
            visDatasetMapper.insert(item);
        }
        return copy.getId();
    }
}
