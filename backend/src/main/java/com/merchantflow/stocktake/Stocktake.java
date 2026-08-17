package com.merchantflow.stocktake;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name = "stocktake") public class Stocktake {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "stocktake_no", nullable = false, unique = true) private String stocktakeNo;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private StocktakeStatus status = StocktakeStatus.DRAFT;
  @Column(name = "operator_name", nullable = false) private String operatorName;
  @Column(nullable = false) private String remark = "";
  @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
  public Long getId() { return id; } public String getStocktakeNo() { return stocktakeNo; } public StocktakeStatus getStatus() { return status; } public String getOperatorName() { return operatorName; } public String getRemark() { return remark; } public LocalDateTime getCreatedAt() { return createdAt; }
  public static Stocktake create(String number, String operator) { Stocktake s = new Stocktake(); s.stocktakeNo = number; s.operatorName = operator; return s; }
  public void markCompleted() { status = StocktakeStatus.COMPLETED; }
  public void markCancelled() { status = StocktakeStatus.CANCELLED; }
}
