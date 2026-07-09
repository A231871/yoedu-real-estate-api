-- ============================================================
-- V9 — Seed Data (Tỉnh thành + data dev)
-- ============================================================

-- ----------------------------------------------------------------
-- Dev seed: 1 admin user (password = "Admin@123" bcrypt)
-- ----------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, user_role, status, email_verified) VALUES
    (
        uuidv7(),
        'admin@nhatrovn.dev',
        '$2a$12$RnZwxT7sKqP1QbGKzY3.5.KYm8sZ9aW1V7bN2xM6cJ4dE8fH0iL3q',
        'Admin NhaTroVN',
        '0901234567',
        'ADMIN',
        'ACTIVE',
        TRUE
    );
