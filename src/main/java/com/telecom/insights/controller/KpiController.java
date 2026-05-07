package com.telecom.insights.controller;

import com.telecom.insights.repository.DashboardRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
public class KpiController {

    private final DashboardRepository dashboardRepository;

    public KpiController(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    // =====================================================
    // KPI CARDS
    // =====================================================

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getKPIs() {

        return ResponseEntity.ok(
                dashboardRepository.getAggregateKPIs()
        );
    }

    // =====================================================
    // 24 HOUR UTILIZATION TREND
    // =====================================================

    @GetMapping("/hourly-trend")
    public ResponseEntity<List<Map<String, Object>>> getHourlyTrend() {

        return ResponseEntity.ok(
                dashboardRepository.getHourlyUtilizationTrend()
        );
    }

    // =====================================================
    // NETWORK BAND ANALYSIS
    // =====================================================

    @GetMapping("/band-performance")
    public ResponseEntity<List<Map<String, Object>>> getBandPerformance() {

        return ResponseEntity.ok(
                dashboardRepository.getBandPerformance()
        );
    }

    // =====================================================
    // WORST STATES
    // =====================================================

    @GetMapping("/worst-states")
    public ResponseEntity<List<Map<String, Object>>> getWorstStates() {

        return ResponseEntity.ok(
                dashboardRepository.getWorstStates()
        );
    }

    // =====================================================
    // CARRIER SCORES
    // =====================================================

    @GetMapping("/carrier-scores")
    public ResponseEntity<List<Map<String, Object>>> getCarrierScores() {

        return ResponseEntity.ok(
                dashboardRepository.getCarrierScores()
        );
    }

    // =====================================================
    // CARRIER PERFORMANCE
    // =====================================================

    @GetMapping("/carrier-performance")
    public ResponseEntity<List<Map<String, Object>>> getCarrierPerformance() {

        return ResponseEntity.ok(
                dashboardRepository.getCarrierPerformance()
        );
    }
}