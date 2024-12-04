package com.fuint.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.fuint.common.service.StoreService;
import com.fuint.framework.annoation.OperationServiceLog;
import com.fuint.framework.exception.BusinessCheckException;
import com.fuint.framework.pagination.PaginationRequest;
import com.fuint.framework.pagination.PaginationResponse;
import com.fuint.repository.model.MtBanner;
import com.fuint.common.dto.BannerDto;
import com.fuint.common.service.BannerService;
import com.fuint.common.service.SettingService;
import com.fuint.common.enums.StatusEnum;
import com.fuint.repository.mapper.MtBannerMapper;

import com.fuint.repository.model.MtStore;
import com.github.pagehelper.PageHelper;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.pagehelper.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * 焦点图服务接口
 *
 * Created by FSQ
 * CopyRight https://www.fuint.cn
 */
@Service
@AllArgsConstructor
public class BannerServiceImpl extends ServiceImpl<MtBannerMapper, MtBanner> implements BannerService {

    private static final Logger logger = LoggerFactory.getLogger(BannerServiceImpl.class);

    private MtBannerMapper mtBannerMapper;

    /**
     * 系统设置服务接口
     * */
    private SettingService settingService;

    /**
     * 店铺接口
     */
    private StoreService storeService;

    /**
     * 分页查询焦点图列表
     *
     * @param paginationRequest
     * @return
     */
    @Override
    public PaginationResponse<MtBanner> queryBannerListByPagination(PaginationRequest paginationRequest) {
        Page<MtBanner> pageHelper = PageHelper.startPage(paginationRequest.getCurrentPage(), paginationRequest.getPageSize());
        LambdaQueryWrapper<MtBanner> lambdaQueryWrapper = Wrappers.lambdaQuery();
        lambdaQueryWrapper.ne(MtBanner::getStatus, StatusEnum.DISABLE.getKey());

        String title = paginationRequest.getSearchParams().get("title") == null ? "" : paginationRequest.getSearchParams().get("title").toString();
        if (StringUtils.isNotBlank(title)) {
            lambdaQueryWrapper.like(MtBanner::getTitle, title);
        }
        String status = paginationRequest.getSearchParams().get("status") == null ? "" : paginationRequest.getSearchParams().get("status").toString();
        if (StringUtils.isNotBlank(status)) {
            lambdaQueryWrapper.eq(MtBanner::getStatus, status);
        }
        String merchantId = paginationRequest.getSearchParams().get("merchantId") == null ? "" : paginationRequest.getSearchParams().get("merchantId").toString();
        if (StringUtils.isNotBlank(merchantId)) {
            lambdaQueryWrapper.eq(MtBanner::getMerchantId, merchantId);
        }
        String storeId = paginationRequest.getSearchParams().get("storeId") == null ? "" : paginationRequest.getSearchParams().get("storeId").toString();
        if (StringUtils.isNotBlank(storeId)) {
            lambdaQueryWrapper.eq(MtBanner::getStoreId, storeId);
        }

        lambdaQueryWrapper.orderByAsc(MtBanner::getSort);
        List<MtBanner> dataList = mtBannerMapper.selectList(lambdaQueryWrapper);

        PageRequest pageRequest = PageRequest.of(paginationRequest.getCurrentPage(), paginationRequest.getPageSize());
        PageImpl pageImpl = new PageImpl(dataList, pageRequest, pageHelper.getTotal());
        PaginationResponse<MtBanner> paginationResponse = new PaginationResponse(pageImpl, MtBanner.class);
        paginationResponse.setTotalPages(pageHelper.getPages());
        paginationResponse.setTotalElements(pageHelper.getTotal());
        paginationResponse.setContent(dataList);

        return paginationResponse;
    }

    /**
     * 添加焦点图
     *
     * @param bannerDto 焦点图信息
     * @return
     */
    @Override
    @OperationServiceLog(description = "新增焦点图")
    public MtBanner addBanner(BannerDto bannerDto) throws BusinessCheckException {
        MtBanner mtBanner = new MtBanner();
        BeanUtils.copyProperties(bannerDto, mtBanner);
        Integer storeId = bannerDto.getStoreId() == null ? 0 : bannerDto.getStoreId();
        if (bannerDto.getMerchantId() == null || bannerDto.getMerchantId() <= 0) {
            MtStore mtStore = storeService.queryStoreById(storeId);
            if (mtStore != null) {
                mtBanner.setMerchantId(mtStore.getMerchantId());
            }
        }
        if (mtBanner.getMerchantId() == null || mtBanner.getMerchantId() < 1) {
            throw new BusinessCheckException("平台方帐号无法执行该操作，请使用商户帐号操作");
        }
        mtBanner.setStoreId(storeId);
        mtBanner.setStatus(StatusEnum.ENABLED.getKey());
        mtBanner.setUpdateTime(new Date());
        mtBanner.setCreateTime(new Date());
        mtBanner.setPosition(bannerDto.getPosition());
        Integer id = mtBannerMapper.insert(mtBanner);
        if (id > 0) {
            return mtBanner;
        } else {
            logger.error("新增焦点图失败.");
            throw new BusinessCheckException("新增焦点图失败");
        }
    }

    /**
     * 根据ID获取Banner信息
     *
     * @param id BannerID
     * @return
     */
    @Override
    public MtBanner queryBannerById(Integer id) {
        return mtBannerMapper.selectById(id);
    }

    /**
     * 根据ID删除Banner图
     *
     * @param id BannerID
     * @param operator 操作人
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationServiceLog(description = "删除Banner图")
    public void deleteBanner(Integer id, String operator) {
        MtBanner mtBanner = queryBannerById(id);
        if (null == mtBanner) {
            return;
        }
        mtBanner.setStatus(StatusEnum.DISABLE.getKey());
        mtBanner.setUpdateTime(new Date());
        mtBannerMapper.updateById(mtBanner);
    }

    /**
     * 修改Banner图
     *
     * @param bannerDto
     * @throws BusinessCheckException
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationServiceLog(description = "更新焦点图")
    public MtBanner updateBanner(BannerDto bannerDto) throws BusinessCheckException {
        MtBanner mtBanner = queryBannerById(bannerDto.getId());
        if (mtBanner == null) {
            throw new BusinessCheckException("该Banner状态异常");
        }
        if (bannerDto.getMerchantId() == null || bannerDto.getMerchantId() < 1) {
            throw new BusinessCheckException("平台方帐号无法执行该操作，请使用商户帐号操作");
        }
        mtBanner.setId(bannerDto.getId());
        if (bannerDto.getImage() != null) {
            mtBanner.setImage(bannerDto.getImage());
        }
        if (bannerDto.getTitle() != null) {
            mtBanner.setTitle(bannerDto.getTitle());
        }
        if (bannerDto.getStoreId() != null) {
            mtBanner.setStoreId(bannerDto.getStoreId());
        }
        if (bannerDto.getDescription() != null) {
            mtBanner.setDescription(bannerDto.getDescription());
        }
        if (bannerDto.getOperator() != null) {
            mtBanner.setOperator(bannerDto.getOperator());
        }
        if (bannerDto.getStatus() != null) {
            mtBanner.setStatus(bannerDto.getStatus());
        }
        if (bannerDto.getUrl() != null) {
            mtBanner.setUrl(bannerDto.getUrl());
        }
        if (bannerDto.getSort() != null) {
            mtBanner.setSort(bannerDto.getSort());
        }
        if (bannerDto.getPosition() != null) {
            mtBanner.setPosition(bannerDto.getPosition());
        }
        mtBanner.setUpdateTime(new Date());
        mtBannerMapper.updateById(mtBanner);
        return mtBanner;
    }

    /**
     * 根据条件搜索焦点图
     *
     * @param params 查询参数
     * @throws BusinessCheckException
     * @return
     * */
    @Override
    public List<MtBanner> queryBannerListByParams(Map<String, Object> params) {
        String status =  params.get("status") == null ? StatusEnum.ENABLED.getKey(): params.get("status").toString();
        String storeId =  params.get("storeId") == null ? "" : params.get("storeId").toString();
        String merchantId =  params.get("merchantId") == null ? "" : params.get("merchantId").toString();
        String title = params.get("title") == null ? "" : params.get("title").toString();
        String position = params.get("position") == null ? "" : params.get("position").toString();

        LambdaQueryWrapper<MtBanner> lambdaQueryWrapper = Wrappers.lambdaQuery();
        lambdaQueryWrapper.ne(MtBanner::getStatus, StatusEnum.DISABLE.getKey());
        if (StringUtils.isNotBlank(title)) {
            lambdaQueryWrapper.like(MtBanner::getTitle, title);
        }
        if (StringUtils.isNotBlank(status)) {
            lambdaQueryWrapper.eq(MtBanner::getStatus, status);
        }
        if (StringUtils.isNotBlank(merchantId)) {
            lambdaQueryWrapper.eq(MtBanner::getMerchantId, merchantId);
        }
        if (StringUtils.isNotBlank(position)) {
            lambdaQueryWrapper.eq(MtBanner::getPosition, position);
        }
        if (StringUtils.isNotBlank(storeId)) {
            lambdaQueryWrapper.and(wq -> wq
                              .eq(MtBanner::getStoreId, 0)
                              .or()
                              .eq(MtBanner::getStoreId, storeId));
        }

        lambdaQueryWrapper.orderByAsc(MtBanner::getSort);
        List<MtBanner> dataList = mtBannerMapper.selectList(lambdaQueryWrapper);
        String baseImage = settingService.getUploadBasePath();

        if (dataList.size() > 0) {
            for (MtBanner banner : dataList) {
                 banner.setImage(baseImage + banner.getImage());
            }
        }

        return dataList;
    }
}
