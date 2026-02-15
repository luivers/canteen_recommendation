package com.school.canteen.controller;

import com.school.canteen.entity.Order;
import com.school.canteen.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/** 支付控制器 — 处理支付回调和前端主动标记支付成功 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final OrderService orderService;
    
    @PostMapping("/callback")
    public ResponseEntity<?> paymentCallback(@RequestBody Map<String, Object> payload) {
        try {
            String orderNumber = String.valueOf(payload.get("orderNumber"));
            String paymentMethod = String.valueOf(payload.get("paymentMethod"));
            String transactionId = String.valueOf(payload.get("transactionId"));
            LocalDateTime paidAt = LocalDateTime.now();
            Object paidAtObj = payload.get("paidAt");
            if (paidAtObj instanceof String) {
                try {
                    paidAt = java.time.ZonedDateTime.parse((String) paidAtObj)
                            .withZoneSameInstant(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                } catch (Exception ignore) {
                    try {
                        paidAt = LocalDateTime.parse((String) paidAtObj);
                    } catch (Exception ignored) { }
                }
            }
            Order updated = orderService.markOrderPaidByNumber(orderNumber, paymentMethod, transactionId, paidAt);
            
            String updatedPaymentMethod = null;
            String updatedTransactionId = null;
            LocalDateTime updatedPaymentTime = null;
            
            if (updated.getOrderItems() != null && !updated.getOrderItems().isEmpty()) {
                com.school.canteen.entity.OrderItem item = updated.getOrderItems().get(0);
                updatedPaymentMethod = item.getPaymentMethod();
                updatedTransactionId = item.getPaymentTransactionId();
                updatedPaymentTime = item.getPaymentTime();
            }
            
            return ResponseEntity.ok(Map.of(
                "orderId", updated.getId(),
                "orderNumber", updated.getOrderNumber(),
                "status", updated.getStatus().name(),
                "paymentMethod", updatedPaymentMethod != null ? updatedPaymentMethod : "UNKNOWN",
                "transactionId", updatedTransactionId != null ? updatedTransactionId : "",
                "paymentTime", updatedPaymentTime != null ? updatedPaymentTime : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @PostMapping("/orders/{orderId}/success")
    public ResponseEntity<?> markPaid(@PathVariable Long orderId, @RequestBody Map<String, Object> payload) {
        try {
            String paymentMethod = String.valueOf(payload.getOrDefault("paymentMethod", "UNKNOWN"));
            String transactionId = String.valueOf(payload.getOrDefault("transactionId", ""));
            LocalDateTime paidAt = LocalDateTime.now();
            Object paidAtObj = payload.get("paidAt");
            if (paidAtObj instanceof String) {
                try {
                    paidAt = java.time.ZonedDateTime.parse((String) paidAtObj)
                            .withZoneSameInstant(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                } catch (Exception ignore) {
                    try {
                        paidAt = LocalDateTime.parse((String) paidAtObj);
                    } catch (Exception ignored) { }
                }
            }
            Order updated = orderService.markOrderPaid(orderId, paymentMethod, transactionId, paidAt);
            
            String updatedPaymentMethod = null;
            String updatedTransactionId = null;
            LocalDateTime updatedPaymentTime = null;
            
            if (updated.getOrderItems() != null && !updated.getOrderItems().isEmpty()) {
                com.school.canteen.entity.OrderItem item = updated.getOrderItems().get(0);
                updatedPaymentMethod = item.getPaymentMethod();
                updatedTransactionId = item.getPaymentTransactionId();
                updatedPaymentTime = item.getPaymentTime();
            }

            return ResponseEntity.ok(Map.of(
                "orderId", updated.getId(),
                "orderNumber", updated.getOrderNumber(),
                "status", updated.getStatus().name(),
                "paymentMethod", updatedPaymentMethod != null ? updatedPaymentMethod : "UNKNOWN",
                "transactionId", updatedTransactionId != null ? updatedTransactionId : "",
                "paymentTime", updatedPaymentTime != null ? updatedPaymentTime : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
