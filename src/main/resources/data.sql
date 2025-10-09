-- Insert sample users with BCrypt encoded passwords
-- Password for all users: 'password123'
INSERT INTO users (username, password, email, enabled) VALUES
('user1', '$2a$10$UdVZv0YFDD8O49vuhiUplOJ7PESG3mrIO8kLQC9S4s16DXUmPR.R2', 'user1@example.com', TRUE),
('user2', '$2a$10$UdVZv0YFDD8O49vuhiUplOJ7PESG3mrIO8kLQC9S4s16DXUmPR.R2', 'user2@example.com', TRUE),
('user3', '$2a$10$UdVZv0YFDD8O49vuhiUplOJ7PESG3mrIO8kLQC9S4s16DXUmPR.R2', 'user3@example.com', TRUE);

-- Insert sample jackpots with different configurations
-- Fixed contribution (5%) and Fixed reward chance (10%)
INSERT INTO jackpots (
    jackpot_id, initial_pool_value, current_pool_value,
    contribution_type, fixed_contribution_percentage,
    reward_type, fixed_reward_chance
) VALUES (
    'jackpot-fixed-fixed', 1000.00, 1000.00,
    'FIXED', 5.00,
    'FIXED', 100.00
);

-- Fixed contribution (3%) and Variable reward chance
INSERT INTO jackpots (
    jackpot_id, initial_pool_value, current_pool_value,
    contribution_type, fixed_contribution_percentage,
    reward_type, variable_reward_base_chance, variable_reward_increment, variable_reward_threshold
) VALUES (
    'jackpot-fixed-variable', 2000.00, 2000.00,
    'FIXED', 3.00,
    'VARIABLE', 5.00, 0.5, 10000.00
);

-- Variable contribution and Fixed reward chance (15%)
INSERT INTO jackpots (
    jackpot_id, initial_pool_value, current_pool_value,
    contribution_type, variable_contribution_base_percentage, variable_contribution_decay_rate,
    reward_type, fixed_reward_chance
) VALUES (
    'jackpot-variable-fixed', 1500.00, 1500.00,
    'VARIABLE', 10.00, 0.1,
    'FIXED', 15.00
);

-- Variable contribution and Variable reward chance
INSERT INTO jackpots (
    jackpot_id, initial_pool_value, current_pool_value,
    contribution_type, variable_contribution_base_percentage, variable_contribution_decay_rate,
    reward_type, variable_reward_base_chance, variable_reward_increment, variable_reward_threshold
) VALUES (
    'jackpot-variable-variable', 3000.00, 3000.00,
    'VARIABLE', 8.00, 0.05,
    'VARIABLE', 2.00, 1.0, 5000.00
);