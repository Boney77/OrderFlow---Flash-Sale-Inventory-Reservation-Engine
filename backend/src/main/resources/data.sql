-- Seed data for flash sale product
INSERT INTO products (id, name, total_stock, available_stock, created_at)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'Flash Sale Item', 100, 100, NOW())
ON CONFLICT (id) DO NOTHING;
