package com.allforone.starvestop.domain.product.repository;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.order.dto.OrderProductQuantityDto;
import com.allforone.starvestop.domain.product.dto.request.StockDecreaseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductBulkUpdateRepository {
    private final JdbcTemplate jdbcTemplate;

    public void decreaseStockWithCaseWhen(List<StockDecreaseRequest> request) {
        if (request.isEmpty()) return;

        StringBuilder sql = new StringBuilder();
        StringBuilder setClause = new StringBuilder("UPDATE products SET stock = CASE id ");
        StringBuilder whereCondition = new StringBuilder(" AND stock >= CASE id ");
        StringBuilder inClause = new StringBuilder("WHERE id IN (");

        //동적 쿼리 문자열 조립
        for (int i = 0; i < request.size(); i++) {
            StockDecreaseRequest req = request.get(i);

            // SET stock = CASE id WHEN 3 THEN stock - N ...
            setClause.append("WHEN ").append(req.getProductId())
                    .append(" THEN stock - ").append(req.getQuantity()).append(" ");

            // AND stock >= CASE id WHEN 3 THEN N ...
            whereCondition.append("WHEN ").append(req.getProductId())
                    .append(" THEN ").append(req.getQuantity()).append(" ");

            // WHERE id IN (3, 8, 4)
            inClause.append(req.getProductId());
            if (i < request.size() - 1) {
                inClause.append(", ");
            }
        }

        setClause.append("ELSE stock END ");
        whereCondition.append("END ");
        inClause.append(") ");

        //최종 쿼리 합치기
        sql.append(setClause).append(inClause).append(whereCondition);

        //단일 쿼리 실행
        int updatedCount = jdbcTemplate.update(sql.toString());

        //요청한 상품 종류의 수와 실제로 업데이트된 레코드 수가 다르면 롤백
        if (updatedCount != request.size()) {
            throw new CustomException(ErrorCode.PRODUCT_LIST_ITEM_NOT_ENOUGH_STOCK);
        }
    }

    public void decreaseStockWithBatchUpdate(List<StockDecreaseRequest> requests) {
        if (requests.isEmpty()) return;

        requests.sort(Comparator.comparing(StockDecreaseRequest::getProductId));

        // 2. 재고가 차감할 수량(quantity)보다 크거나 같을 때만 업데이트하는 쿼리
        String sql = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";

        // 3. Batch Update 실행
        int[] affectedRows = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                StockDecreaseRequest request = requests.get(i);
                ps.setInt(1, request.getQuantity());    // 1번째 ?: 뺄 수량
                ps.setLong(2, request.getProductId());  // 2번째 ?: 타겟 상품 ID
                ps.setInt(3, request.getQuantity());    // 3번째 ?: 남은 재고가 뺄 수량보다 많은지 검사
            }

            @Override
            public int getBatchSize() {
                return requests.size();
            }
        });

        // 4. 검증 로직: 조건(stock >= ?)을 만족하지 못해 업데이트가 안 된 행(0)이 있는지 확인
        for (int rowCount : affectedRows) {
            if (rowCount == 0) {
                // 재고 부족한 상품이 있을 시 @Transactional이 이 장바구니 결제 전체를 Rollback 하도록 만듦
                throw new CustomException(ErrorCode.PRODUCT_LIST_ITEM_NOT_ENOUGH_STOCK);
            }
        }
    }

    public void increaseStockWithBatchUpdate(List<OrderProductQuantityDto> requests) {
        if (requests.isEmpty()) return;

        requests.sort(Comparator.comparing(OrderProductQuantityDto::getId));

        // 2. 재고가 차감할 수량(quantity)보다 크거나 같을 때만 업데이트하는 쿼리
        String sql = "UPDATE products SET stock = stock + ? WHERE id = ?";

        // 3. Batch Update 실행
        int[] affectedRows = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                OrderProductQuantityDto request = requests.get(i);
                ps.setInt(1, request.getQuantity());    // 1번째 ?: 뺄 수량
                ps.setLong(2, request.getId());  // 2번째 ?: 타겟 상품 ID
            }

            @Override
            public int getBatchSize() {
                return requests.size();
            }
        });
    }
}
