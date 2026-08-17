package com.merchantflow.stocktake;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {
  List<Stocktake> findAllByOrderByIdDesc();
}
