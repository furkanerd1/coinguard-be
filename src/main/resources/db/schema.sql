-- ============================================
-- CoinGuard Database Schema
-- PostgreSQL 16+
-- ============================================

-- Drop tables if exists (for fresh start)
DROP TABLE IF EXISTS budgets CASCADE;
DROP TABLE IF EXISTS ai_conversations CASCADE;
DROP TABLE IF EXISTS receipts CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS wallets CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Drop enums if exists
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS transaction_type CASCADE;
DROP TYPE IF EXISTS transaction_status CASCADE;
DROP TYPE IF EXISTS receipt_category CASCADE;
DROP TYPE IF EXISTS processing_status CASCADE;

-- ============================================
-- ENUMS
-- ============================================

CREATE TYPE user_role AS ENUM ('USER', 'ADMIN', 'PREMIUM_USER');

CREATE TYPE transaction_type AS ENUM (
    'TRANSFER',
    'DEPOSIT',
    'WITHDRAWAL',
    'FEE',
    'RECEIPT_EXPENSE'
);

CREATE TYPE transaction_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REVERSED'
);

CREATE TYPE receipt_category AS ENUM (
    'FOOD_BEVERAGE',
    'GROCERY',
    'TRANSPORT',
    'SHOPPING',
    'ENTERTAINMENT',
    'HEALTHCARE',
    'UTILITIES',
    'EDUCATION',
    'TECHNOLOGY',
    'OTHER'
);

CREATE TYPE processing_status AS ENUM (
    'UPLOADED',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'MANUAL_REVIEW'
);

CREATE TYPE notification_type AS ENUM (
    'INFO',
    'SUCCESS',
    'WARNING',
    'ERROR'
);

-- ============================================
-- TABLES
-- ============================================

-- --------------------------------------------
-- USERS TABLE
-- --------------------------------------------
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100),
                       phone_number VARCHAR(15),
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       role user_role NOT NULL DEFAULT 'USER',
                       created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP(6)
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(is_active);

-- Comments
COMMENT ON TABLE users IS 'User accounts with authentication information';
COMMENT ON COLUMN users.password IS 'BCrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role for authorization';

-- --------------------------------------------
-- REFRESH_TOKENS TABLE
-- --------------------------------------------
CREATE TABLE refresh_tokens (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 token VARCHAR(512) NOT NULL UNIQUE,
                                 expiry_date TIMESTAMP(6) NOT NULL,
                                 is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_expiry ON refresh_tokens(expiry_date);
CREATE INDEX idx_refresh_user_revoked ON refresh_tokens(user_id, is_revoked);

-- Comments
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for authentication';
COMMENT ON COLUMN refresh_tokens.token IS 'Unique UUID refresh token';
COMMENT ON COLUMN refresh_tokens.expiry_date IS 'Token expiration timestamp';
COMMENT ON COLUMN refresh_tokens.is_revoked IS 'Whether token has been revoked';

-- --------------------------------------------
-- PASSWORD_RESET_TOKENS TABLE
-- --------------------------------------------
CREATE TABLE password_reset_tokens (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       token VARCHAR(255) NOT NULL UNIQUE,
                                       expiry_date TIMESTAMP(6) NOT NULL,
                                       created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expiry ON password_reset_tokens(expiry_date);

-- Comments
COMMENT ON TABLE password_reset_tokens IS 'Password reset tokens for account recovery';
COMMENT ON COLUMN password_reset_tokens.token IS 'Unique UUID token for password reset';
COMMENT ON COLUMN password_reset_tokens.expiry_date IS 'Token expiration timestamp (15 minutes)';

-- --------------------------------------------
-- WALLETS TABLE
-- --------------------------------------------
CREATE TABLE wallets (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                         balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
                         currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                         version BIGINT NOT NULL DEFAULT 0,
                         is_frozen BOOLEAN NOT NULL DEFAULT FALSE,
                         daily_limit NUMERIC(19,2) DEFAULT 10000.00,
                         daily_spent NUMERIC(19,2) DEFAULT 0.00,
                         last_reset_date TIMESTAMP(6),
                         created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_currency ON wallets(currency);
CREATE INDEX idx_wallets_frozen ON wallets(is_frozen);

-- Constraints
ALTER TABLE wallets ADD CONSTRAINT check_balance_non_negative
    CHECK (balance >= 0);

ALTER TABLE wallets ADD CONSTRAINT check_daily_limit_positive
    CHECK (daily_limit > 0);

ALTER TABLE wallets ADD CONSTRAINT check_daily_spent_non_negative
    CHECK (daily_spent >= 0);

ALTER TABLE wallets ADD CONSTRAINT check_currency_format
    CHECK (currency ~ '^[A-Z]{3}$');

-- Comments
COMMENT ON TABLE wallets IS 'User wallets with balance and transaction limits';
COMMENT ON COLUMN wallets.version IS 'Optimistic locking version for concurrency control';
COMMENT ON COLUMN wallets.is_frozen IS 'Admin can freeze wallet for security reasons';

-- --------------------------------------------
-- TRANSACTIONS TABLE
-- --------------------------------------------
CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              from_wallet_id BIGINT REFERENCES wallets(id) ON DELETE SET NULL,
                              to_wallet_id BIGINT REFERENCES wallets(id) ON DELETE SET NULL,
                              amount NUMERIC(19,2) NOT NULL,
                              fee NUMERIC(19,2) NOT NULL DEFAULT 0.00,
                              currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                              type transaction_type NOT NULL,
                              status transaction_status NOT NULL DEFAULT 'PENDING',
                              description VARCHAR(500),
                              reference_no VARCHAR(36) NOT NULL UNIQUE,
                              failure_reason VARCHAR(255),
                              created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              completed_at TIMESTAMP(6)
);

-- Indexes
CREATE INDEX idx_transactions_from_wallet ON transactions(from_wallet_id);
CREATE INDEX idx_transactions_to_wallet ON transactions(to_wallet_id);
CREATE INDEX idx_transactions_reference_no ON transactions(reference_no);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_from_wallet_date ON transactions(from_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_to_wallet_date ON transactions(to_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_type_status ON transactions(type, status);

-- Constraints
ALTER TABLE transactions ADD CONSTRAINT check_amount_positive
    CHECK (amount > 0);

ALTER TABLE transactions ADD CONSTRAINT check_fee_non_negative
    CHECK (fee >= 0);

ALTER TABLE transactions ADD CONSTRAINT check_wallet_not_both_null
    CHECK (from_wallet_id IS NOT NULL OR to_wallet_id IS NOT NULL);

ALTER TABLE transactions ADD CONSTRAINT check_wallet_not_same
    CHECK (
        from_wallet_id IS NULL OR
        to_wallet_id IS NULL OR
        from_wallet_id != to_wallet_id
    );

ALTER TABLE transactions ADD CONSTRAINT check_currency_format_tx
    CHECK (currency ~ '^[A-Z]{3}$');

-- Comments
COMMENT ON TABLE transactions IS 'All financial transactions between wallets';
COMMENT ON COLUMN transactions.from_wallet_id IS 'Source wallet (NULL for deposits)';
COMMENT ON COLUMN transactions.to_wallet_id IS 'Destination wallet (NULL for withdrawals)';
COMMENT ON COLUMN transactions.reference_no IS 'Unique transaction reference number (UUID)';
COMMENT ON COLUMN transactions.status IS 'Transaction processing status';

-- --------------------------------------------
-- RECEIPTS TABLE
-- --------------------------------------------
CREATE TABLE receipts (
                          id BIGSERIAL PRIMARY KEY,
                          wallet_id BIGINT NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
                          transaction_id BIGINT UNIQUE REFERENCES transactions(id) ON DELETE SET NULL,
                          file_url VARCHAR(500) NOT NULL,
                          file_name VARCHAR(255),
                          file_size BIGINT,
                          merchant_name VARCHAR(255),
                          amount NUMERIC(19,2),
                          currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                          receipt_date DATE,
                          category receipt_category,
                          raw_text TEXT,
                          gemini_confidence DOUBLE PRECISION,
                          status processing_status NOT NULL DEFAULT 'UPLOADED',
                          error_message VARCHAR(500),
                          created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          processed_at TIMESTAMP(6)
);

-- Indexes
CREATE INDEX idx_receipts_wallet_id ON receipts(wallet_id);
CREATE INDEX idx_receipts_transaction_id ON receipts(transaction_id);
CREATE INDEX idx_receipts_status ON receipts(status);
CREATE INDEX idx_receipts_category ON receipts(category);
CREATE INDEX idx_receipts_receipt_date ON receipts(receipt_date DESC);
CREATE INDEX idx_receipts_wallet_status ON receipts(wallet_id, status);
CREATE INDEX idx_receipts_date_category ON receipts(receipt_date DESC, category);

-- Constraints
ALTER TABLE receipts ADD CONSTRAINT check_amount_positive_receipt
    CHECK (amount IS NULL OR amount > 0);

ALTER TABLE receipts ADD CONSTRAINT check_file_size_positive
    CHECK (file_size IS NULL OR file_size > 0);

ALTER TABLE receipts ADD CONSTRAINT check_gemini_confidence_range
    CHECK (gemini_confidence IS NULL OR (gemini_confidence >= 0 AND gemini_confidence <= 1));

ALTER TABLE receipts ADD CONSTRAINT check_currency_format_receipt
    CHECK (currency ~ '^[A-Z]{3}$');

-- Comments
COMMENT ON TABLE receipts IS 'Uploaded receipts for AI OCR processing';
COMMENT ON COLUMN receipts.file_url IS 'MinIO object storage URL';
COMMENT ON COLUMN receipts.raw_text IS 'Full text extracted by Gemini OCR';
COMMENT ON COLUMN receipts.gemini_confidence IS 'AI confidence score (0-1)';
COMMENT ON COLUMN receipts.status IS 'Processing status of receipt';

-- --------------------------------------------
-- BUDGETS TABLE
-- --------------------------------------------
CREATE TABLE budgets (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         category transaction_category NOT NULL,
                         limit_amount NUMERIC(19,2) NOT NULL,
                         spent_amount NUMERIC(19,2) NOT NULL DEFAULT 0.00,
                         period_start DATE NOT NULL,
                         period_end DATE NOT NULL,
                         is_active BOOLEAN NOT NULL DEFAULT TRUE,
                         alert_threshold INTEGER NOT NULL DEFAULT 80,
                         alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
                         version BIGINT NOT NULL DEFAULT 0,
                         created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_budgets_category ON budgets(category);
CREATE INDEX idx_budgets_period ON budgets(period_start, period_end);
CREATE INDEX idx_budgets_user_active ON budgets(user_id, is_active, period_end DESC);
CREATE INDEX idx_budgets_active_period ON budgets(is_active, period_end DESC);

-- Constraints
ALTER TABLE budgets ADD CONSTRAINT check_limit_amount_positive
    CHECK (limit_amount > 0);

ALTER TABLE budgets ADD CONSTRAINT check_spent_amount_non_negative
    CHECK (spent_amount >= 0);

ALTER TABLE budgets ADD CONSTRAINT check_alert_threshold_range
    CHECK (alert_threshold >= 0 AND alert_threshold <= 100);

ALTER TABLE budgets ADD CONSTRAINT check_period_valid
    CHECK (period_end > period_start);

-- Comments
COMMENT ON TABLE budgets IS 'User budget limits per category';
COMMENT ON COLUMN budgets.alert_threshold IS 'Alert when usage exceeds this percentage (0-100)';
COMMENT ON COLUMN budgets.version IS 'Optimistic locking for concurrent updates';

-- --------------------------------------------
-- AI_CONVERSATIONS TABLE
-- --------------------------------------------
CREATE TABLE ai_conversations (
                                  id BIGSERIAL PRIMARY KEY,
                                  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  user_message TEXT NOT NULL,
                                  ai_response TEXT NOT NULL,
                                  context_used TEXT,
                                  token_count INTEGER,
                                  type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
                                  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_ai_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_created_at ON ai_conversations(created_at DESC);
CREATE INDEX idx_ai_conversations_type ON ai_conversations(type);
CREATE INDEX idx_ai_conversations_user_date ON ai_conversations(user_id, created_at DESC);

-- Constraints
ALTER TABLE ai_conversations ADD CONSTRAINT check_token_count_positive
    CHECK (token_count IS NULL OR token_count > 0);

-- Comments
COMMENT ON TABLE ai_conversations IS 'AI chatbot conversation history';
COMMENT ON COLUMN ai_conversations.context_used IS 'JSON data used for context (transactions, budgets, etc.)';
COMMENT ON COLUMN ai_conversations.token_count IS 'API token usage for cost tracking';

-- ============================================
-- FUNCTIONS & TRIGGERS
-- ============================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refresh_tokens_updated_at BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_wallets_updated_at BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_receipts_updated_at BEFORE UPDATE ON receipts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_budgets_updated_at BEFORE UPDATE ON budgets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Note: notifications table doesn't have updated_at column, so no trigger needed

-- ============================================
-- SAMPLE DATA (for testing)
-- ============================================

-- Insert test users
INSERT INTO users (username, email, password, full_name, phone_number, role) VALUES
                                                                                 ('admin', 'admin@coinguard.com', '$2a$10$dummyHashedPassword123456789', 'Admin User', '+905551234567', 'ADMIN'),
                                                                                 ('furkan', 'furkan@example.com', '$2a$10$dummyHashedPassword123456789', 'Furkan Erdoğan', '+905559876543', 'USER'),
                                                                                 ('test', 'test@example.com', '$2a$10$dummyHashedPassword123456789', 'Test User', '+905551111111', 'USER');

-- Insert wallets for users
INSERT INTO wallets (user_id, balance, currency) VALUES
                                                     (1, 50000.00, 'TRY'),  -- Admin wallet
                                                     (2, 10000.00, 'TRY'),  -- Furkan's wallet
                                                     (3, 5000.00, 'TRY');   -- Test wallet

-- Insert sample transaction
INSERT INTO transactions (
    from_wallet_id,
    to_wallet_id,
    amount,
    fee,
    currency,
    type,
    status,
    reference_no,
    description
) VALUES (
             2,
             3,
             500.00,
             5.00,
             'TRY',
             'TRANSFER',
             'COMPLETED',
             gen_random_uuid()::text,
             'Test transfer from Furkan to Test User'
         );

-- ============================================
-- VIEWS (for reporting)
-- ============================================

-- User wallet summary
CREATE OR REPLACE VIEW v_user_wallet_summary AS
SELECT
    u.id AS user_id,
    u.username,
    u.email,
    u.full_name,
    w.balance,
    w.currency,
    w.daily_limit,
    w.daily_spent,
    w.is_frozen,
    w.last_reset_date
FROM users u
         LEFT JOIN wallets w ON u.id = w.user_id;

-- Transaction history with user info
CREATE OR REPLACE VIEW v_transaction_history AS
SELECT
    t.id,
    t.reference_no,
    uf.username AS from_user,
    ut.username AS to_user,
    t.amount,
    t.fee,
    t.currency,
    t.type,
    t.status,
    t.description,
    t.created_at,
    t.completed_at
FROM transactions t
         LEFT JOIN wallets wf ON t.from_wallet_id = wf.id
         LEFT JOIN users uf ON wf.user_id = uf.id
         LEFT JOIN wallets wt ON t.to_wallet_id = wt.id
         LEFT JOIN users ut ON wt.user_id = ut.id
ORDER BY t.created_at DESC;

-- Receipt processing statistics
CREATE OR REPLACE VIEW v_receipt_stats AS
SELECT
    u.username,
    r.status,
    COUNT(*) AS count,
    AVG(r.gemini_confidence) AS avg_confidence,
    SUM(r.amount) AS total_amount
FROM receipts r
    JOIN wallets w ON r.wallet_id = w.id
    JOIN users u ON w.user_id = u.id
GROUP BY u.username, r.status;

-- ============================================
-- PERMISSIONS (Optional - for production)
-- ============================================

-- Create read-only role
-- CREATE ROLE coinguard_readonly;
-- GRANT CONNECT ON DATABASE coinguard TO coinguard_readonly;
-- GRANT USAGE ON SCHEMA public TO coinguard_readonly;
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO coinguard_readonly;

-- ============================================
-- MAINTENANCE QUERIES
-- ============================================

-- Analyze tables for query optimization
-- ANALYZE users;
-- ANALYZE wallets;
-- ANALYZE transactions;
-- ANALYZE receipts;
-- ANALYZE budgets;

-- Vacuum tables (for production maintenance)
-- VACUUM ANALYZE users;
-- VACUUM ANALYZE wallets;
-- VACUUM ANALYZE transactions;
-- VACUUM ANALYZE receipts;

COMMENT ON DATABASE coinguard IS 'CoinGuard - AI-Powered Banking Core Database';