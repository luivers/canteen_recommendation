package com.school.canteen.service;

import com.school.canteen.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 订单状态变更SSE推送服务 */
@Service
public class OrderEventService {
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }
    
    public void publishOrderUpdate(Order order) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("order-update")
                    .data(new OrderUpdatePayload(order)));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        });
    }
    
    public static class OrderUpdatePayload {
        public Long id;
        public String orderNumber;
        public String status;
        public String paymentMethod;
        public String transactionId;
        public String paymentTime;
        
        public OrderUpdatePayload(Order o) {
            this.id = o.getId();
            this.orderNumber = o.getOrderNumber();
            this.status = o.getStatus() != null ? o.getStatus().name() : null;
            if (o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
                com.school.canteen.entity.OrderItem item = o.getOrderItems().get(0);
                this.paymentMethod = item.getPaymentMethod();
                this.transactionId = item.getPaymentTransactionId();
                this.paymentTime = item.getPaymentTime() != null ? item.getPaymentTime().toString() : null;
            }
        }
    }
}
