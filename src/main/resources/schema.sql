CREATE TABLE network_metrics (
                                 id SERIAL PRIMARY KEY,
                                 region VARCHAR(100),
                                 signal_strength INT,
                                 churn_rate DECIMAL(5,2),
                                 complaints INT,
                                 revenue DECIMAL(10,2)
);