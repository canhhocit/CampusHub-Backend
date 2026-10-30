-- Migration V20: System settings for withdrawal limit
INSERT INTO system_settings (setting_key, setting_value, description, updated_at) VALUES
('MIN_WITHDRAWAL_AMOUNT', '50000.00', 'Hạn mức rút tiền ví tối thiểu (VNĐ)', CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;
