package com.allforone.starvestop.domain.product.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockDecreaseRequest {
    private Long productId;
    private Integer quantity;
}
