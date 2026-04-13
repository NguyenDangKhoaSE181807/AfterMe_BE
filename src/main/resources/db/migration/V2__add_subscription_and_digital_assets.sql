CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    max_reminders INT NOT NULL,
    max_trusted_contacts INT NOT NULL,
    max_digital_assets INT NOT NULL,
    features TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

ALTER TABLE users
    ADD COLUMN current_plan_id BIGINT NULL,
    ADD COLUMN plan_expires_at TIMESTAMP NULL,
    ADD CONSTRAINT fk_users_current_plan FOREIGN KEY (current_plan_id) REFERENCES plans (id);

CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_user_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES plans (id)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_ref VARCHAR(255),
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_transactions_subscription FOREIGN KEY (subscription_id) REFERENCES user_subscriptions (id)
);

CREATE TABLE subscription_histories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    from_plan_id BIGINT NULL,
    to_plan_id BIGINT NULL,
    changed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_subscription_histories_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_subscription_histories_from_plan FOREIGN KEY (from_plan_id) REFERENCES plans (id),
    CONSTRAINT fk_subscription_histories_to_plan FOREIGN KEY (to_plan_id) REFERENCES plans (id)
);

CREATE TABLE family_members (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_family_members_subscription FOREIGN KEY (subscription_id) REFERENCES user_subscriptions (id),
    CONSTRAINT fk_family_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE digital_assets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    encrypted_secret TEXT NOT NULL,
    encryption_iv VARCHAR(255) NOT NULL,
    encryption_algo VARCHAR(100) NOT NULL,
    access_instructions TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_digital_assets_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE asset_shares (
    id BIGSERIAL PRIMARY KEY,
    digital_asset_id BIGINT NOT NULL,
    trusted_contact_id BIGINT NOT NULL,
    unlock_condition VARCHAR(30) NOT NULL,
    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_asset_shares_digital_asset FOREIGN KEY (digital_asset_id) REFERENCES digital_assets (id),
    CONSTRAINT fk_asset_shares_trusted_contact FOREIGN KEY (trusted_contact_id) REFERENCES trusted_contacts (id)
);

CREATE INDEX idx_users_current_plan_id ON users (current_plan_id);
CREATE INDEX idx_user_subscriptions_user_id ON user_subscriptions (user_id);
CREATE INDEX idx_user_subscriptions_plan_id ON user_subscriptions (plan_id);
CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_subscription_id ON transactions (subscription_id);
CREATE INDEX idx_subscription_histories_user_id ON subscription_histories (user_id);
CREATE INDEX idx_family_members_subscription_id ON family_members (subscription_id);
CREATE INDEX idx_family_members_user_id ON family_members (user_id);
CREATE INDEX idx_digital_assets_user_id ON digital_assets (user_id);
CREATE INDEX idx_asset_shares_digital_asset_id ON asset_shares (digital_asset_id);
CREATE INDEX idx_asset_shares_trusted_contact_id ON asset_shares (trusted_contact_id);
