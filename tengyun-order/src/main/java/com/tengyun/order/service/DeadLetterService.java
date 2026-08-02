package com.tengyun.order.service;

import com.tengyun.order.entity.OrderDeadLetterLog;

import java.util.List;

public interface DeadLetterService {
    List<OrderDeadLetterLog> latest(int limit);
    String requeue(Long id);
}
