-- ============================================================
-- SEED: Users (Owners + Agent)
-- ============================================================

INSERT INTO users (id, email, password_hash, full_name, phone, user_role, status, email_verified, created_at)
VALUES
  (uuidv7(), 'owner1@example.com', '$2a$12$dummyhash1234567890', 'Nguyễn Văn A', '0987654321', 'REGULAR_USER', 'ACTIVE', TRUE, now()),
  (uuidv7(), 'owner2@example.com', '$2a$12$dummyhash1234567890', 'Trần Thị B', '0912345678', 'REGULAR_USER', 'ACTIVE', TRUE, now()),
  (uuidv7(), 'owner3@example.com', '$2a$12$dummyhash1234567890', 'Lê Văn C', '0978123456', 'REGULAR_USER', 'ACTIVE', TRUE, now()),
  (uuidv7(), 'agent1@example.com', '$2a$12$dummyhash1234567890', 'Phạm Thị D', '0933456789', 'AGENT', 'ACTIVE', TRUE, now())
ON CONFLICT DO NOTHING;

-- Get the inserted IDs for references
DO $$
DECLARE
  owner1 UUID; owner2 UUID; owner3 UUID; agent1 UUID;
BEGIN
  SELECT id INTO owner1 FROM users WHERE email = 'owner1@example.com';
  SELECT id INTO owner2 FROM users WHERE email = 'owner2@example.com';
  SELECT id INTO owner3 FROM users WHERE email = 'owner3@example.com';
  SELECT id INTO agent1 FROM users WHERE email = 'agent1@example.com';

  -- ============================================================
  -- SEED: 10 Listings
  -- ============================================================

  INSERT INTO listings (
    id, owner_id, agent_id, property_type_id, address, ward_code,
    listing_type, title, slug, description, area, bedrooms, bathrooms, floors, status, amount_vnd
  ) VALUES
    -- 1. Apartment for rent in Hanoi
    (uuidv7(), owner1, agent1, 2, 'Số 12 Nguyễn Chí Thanh', '00004',
     'FOR_RENT', 'Căn hộ studio view hồ Tây, full nội thất', 'can-ho-studio-nguyen-chi-thanh',
     'Căn hộ studio hiện đại, đầy đủ tiện nghi, gần hồ Tây. Phù hợp cho người độc thân hoặc cặp đôi.',
     45.5, 1, 1, 1, 'APPROVED', 5000000),

    -- 2. House for sale
    (uuidv7(), owner2, NULL, 3, 'Ngõ 45 Tô Ngọc Vân', '00103',
     'FOR_SALE', 'Nhà nguyên căn 3 tầng ngõ thông, ô tô vào nhà', 'nha-nguyen-can-to-ngoc-van',
     'Nhà xây kiên cố, 3 tầng, 4 phòng ngủ, sân trước rộng.',
     120.0, 4, 3, 3, 'APPROVED', 5400000000),

    -- 3. Villa for sale
    (uuidv7(), owner1, agent1, 4, 'Khu biệt thự Ciputra', '00103',
     'FOR_SALE', 'Biệt thự villa Ciputra view sông', 'biet-thu-ciputra',
     'Biệt thự cao cấp, hồ bơi riêng, an ninh 24/7.',
     350.0, 5, 4, 2, 'APPROVED', 15750000000),

    -- 4. Room for rent
    (uuidv7(), owner3, NULL, 1, 'Phòng trọ 192 Thái Hà', '00235',
     'FOR_RENT', 'Phòng trọ sạch sẽ, gần ĐH Quốc Gia', 'phong-tro-thai-ha',
     'Phòng rộng, có WC riêng, bếp riêng, wifi miễn phí.',
     25.0, 1, 1, 1, 'APPROVED', 5000000),

    -- 5. Office for rent
    (uuidv7(), owner2, agent1, 5, 'Tầng 5, Tòa nhà Mipec', '00166',
     'FOR_RENT', 'Văn phòng cho thuê 80m2 Cầu Giấy', 'van-phong-mipec',
     'Văn phòng full nội thất, thang máy, chỗ đỗ xe.',
     80.0, NULL, NULL, 1, 'APPROVED', 12000000),

    -- 6. Land for sale
    (uuidv7(), owner3, NULL, 7, 'Đất nền Thạch Thất', '09955',
     'FOR_SALE', 'Đất nền 120m2 Thạch Thất, sổ đỏ', 'dat-nen-thach-that',
     'Đất vuông vắn, dân cư đông đúc, pháp lý đầy đủ.',
     120.0, NULL, NULL, NULL, 'APPROVED', 5400000000),

    -- 7. Apartment in HCMC
    (uuidv7(), owner1, agent1, 2, 'Chung cư The Sun', '26743',
     'FOR_RENT', 'Căn hộ 2PN Quận 1 view sông', 'can-ho-quan-1',
     'Căn hộ cao cấp, nội thất sang trọng, gần Bitexco.',
     85.0, 2, 2, 1, 'APPROVED', 12000000),

    -- 8. Shop house
    (uuidv7(), owner2, NULL, 6, 'Mặt bằng kinh doanh Nguyễn Huệ', '26740',
     'FOR_RENT', 'Mặt bằng kinh doanh 50m2 Nguyễn Huệ', 'mat-bang-nguyen-hue',
     'Vị trí đắc địa, kinh doanh cafe, quán ăn.',
     50.0, NULL, 1, 2, 'APPROVED', 12000000),

    -- 9. Family house for sale
    (uuidv7(), owner3, agent1, 3, 'Nhà mặt phố Hoàng Mai', '00331',
     'FOR_SALE', 'Nhà mặt phố 5 tầng Hoàng Mai', 'nha-mat-pho-hoang-mai',
     'Nhà kinh doanh + ở, mặt tiền 6m, 5 tầng.',
     180.0, 5, 4, 5, 'APPROVED', 8100000000),

    -- 10. Studio in Binh Thanh
    (uuidv7(), owner1, NULL, 2, 'Căn studio Binh Thạnh', '26929',
     'FOR_RENT', 'Studio full nội thất Binh Thạnh', 'studio-binh-thanh',
     'Căn hộ nhỏ xinh, gần trung tâm, an ninh tốt.',
     35.0, 1, 1, 1, 'APPROVED', 5000000)
  ON CONFLICT DO NOTHING;

  -- ============================================================
  -- SEED: Listing Amenities (some examples)
  -- ============================================================

  INSERT INTO listing_amenities (listing_id, amenity_id)
  SELECT l.id, a.id
  FROM listings l
  CROSS JOIN (SELECT id FROM amenities WHERE slug IN ('dieu-hoa','wifi','cho-xe-may','wc-rieng','ban-cong') LIMIT 4) a
  WHERE l.id IS NOT NULL
  ON CONFLICT DO NOTHING;

  -- ============================================================
  -- SEED: Sample Media
  -- ============================================================

  INSERT INTO listing_media (id, listing_id, media_type, url, caption, sort_order, is_primary)
  SELECT uuidv7(), l.id, 'IMAGE',
         'https://example.com/images/listing-' || l.id || '-1.jpg',
         'Hình chính', 0, TRUE
  FROM listings l
  WHERE NOT EXISTS (SELECT 1 FROM listing_media lm WHERE lm.listing_id = l.id AND lm.is_primary = TRUE)
  ON CONFLICT DO NOTHING;

END $$;
