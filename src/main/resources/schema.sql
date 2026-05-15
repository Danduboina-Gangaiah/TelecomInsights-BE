DROP TABLE IF EXISTS network_metrics;
DROP TABLE IF EXISTS refined_network_metrics;
DROP TABLE IF EXISTS anomaly_alerts;

-- =====================================================
-- BASIC NETWORK METRICS
-- =====================================================

CREATE TABLE network_metrics (

                                 timestamp TIMESTAMP,

                                 region_id VARCHAR(50),

                                 cell_id VARCHAR(50),

                                 avg_latency_ms NUMERIC,

                                 download_speed_mbps NUMERIC,

                                 upload_speed_mbps NUMERIC,

                                 packet_loss_pct NUMERIC,

                                 active_users INTEGER

);

-- =====================================================
-- MAIN TELECOM DATASET
-- =====================================================

CREATE TABLE refined_network_metrics (

                                         id SERIAL PRIMARY KEY,

                                         timestamp TIMESTAMP,

                                         city VARCHAR(100),

                                         signal_strength_dbm NUMERIC,

                                         download_speed_mbps NUMERIC,

                                         upload_speed_mbps NUMERIC,

                                         avg_latency_ms NUMERIC,

                                         jitter_ms NUMERIC,

                                         network_type VARCHAR(50),

                                         device_model VARCHAR(100),

                                         carrier VARCHAR(50),

                                         network_band VARCHAR(50),

                                         battery_level_pct NUMERIC,

                                         temperature_c NUMERIC,

                                         connected_duration_min NUMERIC,

                                         handover_count INTEGER,

                                         data_usage_mb NUMERIC,

                                         video_streaming_quality VARCHAR(50),

                                         vonr_enabled BOOLEAN,

                                         congestion_level VARCHAR(50),

                                         ping_to_google_ms NUMERIC,

                                         dropped_calls INTEGER,

                                         hour_of_day INTEGER,

                                         state VARCHAR(50),

                                         region VARCHAR(50),

                                         environment_type VARCHAR(50),

                                         active_users INTEGER,

                                         packet_loss_pct NUMERIC,

                                         weather_condition VARCHAR(50),

                                         network_utilization_pct NUMERIC,

                                         quality_score NUMERIC,

                                         is_peak_hour BOOLEAN

);

-- =====================================================
-- ANOMALY ALERTS
-- =====================================================

CREATE TABLE anomaly_alerts (

                                id SERIAL PRIMARY KEY,

                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                severity VARCHAR(20),

                                title VARCHAR(100),

                                message TEXT,

<<<<<<< Updated upstream
                                raw_data JSON
                                    B,

                                is_read BOOLEAN DEFAULT FALSE

);
SELECT *
FROM anomaly_alerts
ORDER BY created_at DESC
    LIMIT 30;
SELECT COUNT(*) FROM refined_network_metrics;
SELECT COUNT(*) FROM network_metrics;
SELECT *
FROM refined_network_metrics
ORDER BY timestamp DESC
LIMIT 5;
SELECT COUNT(*) FROM refined_network_metrics;
SELECT id, title, is_read
FROM anomaly_alerts;
SELECT * FROM refined_network_metrics
ORDER BY timestamp DESC
LIMIT 10;
=======
                                raw_data JSONB,

                                is_read BOOLEAN DEFAULT FALSE

);
>>>>>>> Stashed changes
