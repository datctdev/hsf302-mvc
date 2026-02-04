package com.hsf.e_comerce.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewSummaryDTO {
    private double averageRating;      // Điểm trung bình (vd: 4.7)
    private long totalReviews;         // Tổng số đánh giá
    private long count5Star;           // Số lượng 5 sao
    private long count4Star;           // Số lượng 4 sao
    private long count3Star;           // Số lượng 3 sao
    private long count2Star;           // Số lượng 2 sao
    private long count1Star;           // Số lượng 1 sao
    private long countWithImages;      // Số lượng có ảnh
    private long countWithComments;    // Số lượng có bình luận (text)
}