-- Test data for integration tests
-- PostgreSQL compatible syntax without ON CONFLICT

-- Insert test user for authentication tests
-- Password hash must be at least 60 characters to match @Size(min = 60) constraint
-- Using a known working BCrypt hash for 'password'
-- Insert with explicit ID 1 if it doesn't exist
INSERT INTO users (id, username, password, email, enabled, created_at)
SELECT 1,
       'testuser',
       '$2a$10$DantiWQsE41KFDfGwscAoO3rdA.hAyVPrUXexKqM4tIMHT0E2z7Sq',
       'test@example.com',
       true,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

-- Insert test jackpot for bet tests
INSERT INTO jackpots (jackpot_id, initial_pool_value, current_pool_value,
                      contribution_type, fixed_contribution_percentage,
                      reward_type, fixed_reward_chance)
SELECT 'jackpot-1',
       1000.00,
       1000.00,
       'FIXED',
       5.00,
       'FIXED',
       100.00 WHERE NOT EXISTS (SELECT 1 FROM jackpots WHERE jackpot_id = 'jackpot-1');

INSERT INTO jackpots (jackpot_id, initial_pool_value, current_pool_value,
                      contribution_type, fixed_contribution_percentage,
                      reward_type, variable_reward_base_chance, variable_reward_increment, variable_reward_threshold)
SELECT 'jackpot-2',
       2000.00,
       2000.00,
       'FIXED',
       3.00,
       'VARIABLE',
       5.00,
       0.5,
       10000.00 WHERE NOT EXISTS (SELECT 1 FROM jackpots WHERE jackpot_id = 'jackpot-2');

INSERT INTO jackpots (jackpot_id, initial_pool_value, current_pool_value,
                      contribution_type, variable_contribution_base_percentage, variable_contribution_decay_rate,
                      reward_type, fixed_reward_chance)
SELECT 'jackpot-3',
       1500.00,
       1500.00,
       'VARIABLE',
       10.00,
       0.1,
       'FIXED',
       15.00 WHERE NOT EXISTS (SELECT 1 FROM jackpots WHERE jackpot_id = 'jackpot-3');

INSERT INTO jackpots (jackpot_id, initial_pool_value, current_pool_value,
                      contribution_type, variable_contribution_base_percentage, variable_contribution_decay_rate,
                      reward_type, variable_reward_base_chance, variable_reward_increment, variable_reward_threshold)
SELECT 'jackpot-4',
       3000.00,
       3000.00,
       'VARIABLE',
       8.00,
       0.05,
       'VARIABLE',
       2.00,
       1.0,
       5000.00 WHERE NOT EXISTS (SELECT 1 FROM jackpots WHERE jackpot_id = 'jackpot-4');

-- Insert test contribution for bet tests
-- Note: Using IDENTITY generation, so we don't specify id column
INSERT INTO contributions (bet_id, user_id, jackpot_id, stake_amount, contribution_amount, current_jackpot_amount, created_at)
SELECT 'bet-1',
       1,
       'jackpot-1',
       50.00,
       5.00,
       1000.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM contributions WHERE bet_id = 'bet-1');

-- Insert contributions for all jackpots to support parametrized tests
INSERT INTO contributions (bet_id, user_id, jackpot_id, stake_amount, contribution_amount, current_jackpot_amount, created_at)
SELECT 'bet-2',
       1,
       'jackpot-2',
       100.00,
       3.00,
       2000.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM contributions WHERE bet_id = 'bet-2' AND jackpot_id = 'jackpot-2');

INSERT INTO contributions (bet_id, user_id, jackpot_id, stake_amount, contribution_amount, current_jackpot_amount, created_at)
SELECT 'bet-3',
       1,
       'jackpot-3',
       200.00,
       20.00,
       1500.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM contributions WHERE bet_id = 'bet-3' AND jackpot_id = 'jackpot-3');

INSERT INTO contributions (bet_id, user_id, jackpot_id, stake_amount, contribution_amount, current_jackpot_amount, created_at)
SELECT 'bet-4',
       1,
       'jackpot-4',
       150.00,
       12.00,
       3000.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM contributions WHERE bet_id = 'bet-4' AND jackpot_id = 'jackpot-4');

-- Insert test reward for jackpot reward tests
-- Note: Using IDENTITY generation, so we don't specify id column
INSERT INTO rewards (bet_id, user_id, jackpot_id, jackpot_reward_amount, created_at)
SELECT 'bet-1',
       1,
       'jackpot-1',
       100.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE bet_id = 'bet-1');

-- Insert additional rewards for parametrized tests
INSERT INTO rewards (bet_id, user_id, jackpot_id, jackpot_reward_amount, created_at)
SELECT 'bet-2',
       1,
       'jackpot-2',
       200.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE bet_id = 'bet-2' AND jackpot_id = 'jackpot-2');

INSERT INTO rewards (bet_id, user_id, jackpot_id, jackpot_reward_amount, created_at)
SELECT 'bet-3',
       1,
       'jackpot-3',
       150.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE bet_id = 'bet-3' AND jackpot_id = 'jackpot-3');

INSERT INTO rewards (bet_id, user_id, jackpot_id, jackpot_reward_amount, created_at)
SELECT 'bet-4',
       1,
       'jackpot-4',
       300.00,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE bet_id = 'bet-4' AND jackpot_id = 'jackpot-4');