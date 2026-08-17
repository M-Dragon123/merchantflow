package com.merchantflow.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** 统一分页结果：page 从 1 开始。 */
public record PageResult<T>(List<T> items, long total, int page, int size) {
  public static <T> PageResult<T> of(Page<T> source) {
    return new PageResult<>(source.getContent(), source.getTotalElements(), source.getNumber() + 1, source.getSize());
  }
}
