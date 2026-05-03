package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.WaitlistOffer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WaitlistOfferMapper extends BaseMapper<WaitlistOffer> {

    @Select("SELECT * FROM waitlist_offer WHERE offer_status = 'PENDING' " +
            "AND expire_at <= NOW() AND deleted = 0")
    List<WaitlistOffer> findExpiredOffers();
}
