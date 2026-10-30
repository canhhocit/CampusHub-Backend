package com.campushub.modules.review.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.notification.service.NotificationService;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.review.dto.ReviewCreateRequest;
import com.campushub.modules.review.dto.ReviewResponse;
import com.campushub.modules.review.dto.UserRatingSummaryResponse;
import com.campushub.modules.review.entity.Review;
import com.campushub.modules.review.repository.ReviewRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReviewResponse createReview(Long orderId, ReviewCreateRequest request, String buyerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED, "Chỉ có thể đánh giá khi đơn hàng đã hoàn tất");
        }

        if (reviewRepository.existsByOrderId(orderId)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = new Review();
        review.setOrder(order);
        review.setBuyer(order.getBuyer());
        review.setSeller(order.getSeller());
        review.setProduct(order.getProduct());
        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : null);

        review = reviewRepository.save(review);
        log.info("User {} reviewed order #{} (rating: {} stars)", buyerUsername, order.getId(), request.getRating());

        // Notify seller about the new review
        notificationService.createNotification(
                order.getSeller(),
                "Bạn nhận được đánh giá mới!",
                String.format("Người mua %s đã đánh giá %d sao cho đơn hàng %s",
                        order.getBuyer().getFullName() != null ? order.getBuyer().getFullName() : order.getBuyer().getUsername(),
                        request.getRating(),
                        order.getOrderNumber()),
                "REVIEW_RECEIVED",
                "ORDER",
                order.getId()
        );

        return mapToResponse(review);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewByOrderId(Long orderId) {
        return reviewRepository.findByOrderId(orderId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsBySeller(Long sellerId, Pageable pageable) {
        return reviewRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserRatingSummaryResponse getSellerRatingSummary(Long sellerId) {
        if (!userRepository.existsById(sellerId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        Double avg = reviewRepository.getAverageRatingBySellerId(sellerId);
        long count = reviewRepository.countBySellerId(sellerId);
        return new UserRatingSummaryResponse(sellerId, avg, count);
    }

    private ReviewResponse mapToResponse(Review r) {
        ReviewResponse res = new ReviewResponse();
        res.setId(r.getId());
        res.setOrderId(r.getOrder().getId());
        res.setOrderNumber(r.getOrder().getOrderNumber());
        res.setBuyerId(r.getBuyer().getId());
        res.setBuyerUsername(r.getBuyer().getUsername());
        res.setBuyerFullName(r.getBuyer().getFullName());
        res.setSellerId(r.getSeller().getId());
        res.setSellerUsername(r.getSeller().getUsername());
        if (r.getProduct() != null) {
            res.setProductId(r.getProduct().getId());
            res.setProductTitle(r.getProduct().getTitle());
        }
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }
}

