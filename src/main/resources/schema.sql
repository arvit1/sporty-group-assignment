-- Create users table for JWT authentication
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create jackpots table
CREATE TABLE IF NOT EXISTS jackpots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jackpot_id VARCHAR(50) NOT NULL UNIQUE,
    initial_pool_value DECIMAL(19,2) NOT NULL,
    current_pool_value DECIMAL(19,2) NOT NULL,
    contribution_type VARCHAR(20) NOT NULL,
    fixed_contribution_percentage DECIMAL(5,2),
    variable_contribution_base_percentage DECIMAL(5,2),
    variable_contribution_decay_rate DECIMAL(5,2),
    reward_type VARCHAR(20) NOT NULL,
    fixed_reward_chance DECIMAL(5,2),
    variable_reward_base_chance DECIMAL(5,2),
    variable_reward_increment DECIMAL(5,2),
    variable_reward_threshold DECIMAL(19,2),
    version BIGINT DEFAULT 0
);

-- Create contributions table
CREATE TABLE IF NOT EXISTS contributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bet_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    jackpot_id VARCHAR(50) NOT NULL,
    stake_amount DECIMAL(19,2) NOT NULL,
    contribution_amount DECIMAL(19,2) NOT NULL,
    current_jackpot_amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

-- Create rewards table
CREATE TABLE IF NOT EXISTS rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bet_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    jackpot_id VARCHAR(50) NOT NULL,
    jackpot_reward_amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_contributions_bet_id ON contributions(bet_id);
CREATE INDEX IF NOT EXISTS idx_contributions_user_id ON contributions(user_id);
CREATE INDEX IF NOT EXISTS idx_contributions_jackpot_id ON contributions(jackpot_id);
CREATE INDEX IF NOT EXISTS idx_rewards_bet_id ON rewards(bet_id);
CREATE INDEX IF NOT EXISTS idx_rewards_user_id ON rewards(user_id);
CREATE INDEX IF NOT EXISTS idx_rewards_jackpot_id ON rewards(jackpot_id);
CREATE INDEX IF NOT EXISTS idx_jackpots_jackpot_id ON jackpots(jackpot_id);