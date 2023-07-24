package com.bmw.seckill.model.http;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/13 18:32
 */
@Data
public class SeckillReq implements Serializable {

    @NotNull(message="产品id 不能为空")
    private Long productId;

    private Long userId;
}
