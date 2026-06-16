-- ============================================================
-- V9 — Seed Data (Tỉnh thành + data dev)
-- ============================================================

-- ----------------------------------------------------------------
-- Seed provinces (63 tỉnh thành — rút gọn các tỉnh lớn)
-- ----------------------------------------------------------------
INSERT INTO provinces (id, name, slug, code) VALUES
    (1,  'An Giang',                'an-giang',              '89'),
    (2,  'Bà Rịa - Vũng Tàu',      'ba-ria-vung-tau',       '77'),
    (3,  'Bắc Giang',               'bac-giang',             '24'),
    (4,  'Bắc Kạn',                 'bac-kan',               '06'),
    (5,  'Bạc Liêu',                'bac-lieu',              '95'),
    (6,  'Bắc Ninh',                'bac-ninh',              '27'),
    (7,  'Bến Tre',                 'ben-tre',               '83'),
    (8,  'Bình Định',               'binh-dinh',             '52'),
    (9,  'Bình Dương',              'binh-duong',            '74'),
    (10, 'Bình Phước',              'binh-phuoc',            '70'),
    (11, 'Bình Thuận',              'binh-thuan',            '60'),
    (12, 'Cà Mau',                  'ca-mau',                '96'),
    (13, 'Cần Thơ',                 'can-tho',               '92'),
    (14, 'Cao Bằng',                'cao-bang',              '04'),
    (15, 'Đà Nẵng',                 'da-nang',               '48'),
    (16, 'Đắk Lắk',                 'dak-lak',               '66'),
    (17, 'Đắk Nông',                'dak-nong',              '67'),
    (18, 'Điện Biên',               'dien-bien',             '11'),
    (19, 'Đồng Nai',                'dong-nai',              '75'),
    (20, 'Đồng Tháp',               'dong-thap',             '87'),
    (21, 'Gia Lai',                 'gia-lai',               '64'),
    (22, 'Hà Giang',                'ha-giang',              '02'),
    (23, 'Hà Nam',                  'ha-nam',                '35'),
    (24, 'Hà Nội',                  'ha-noi',                '01'),
    (25, 'Hà Tĩnh',                 'ha-tinh',               '42'),
    (26, 'Hải Dương',               'hai-duong',             '30'),
    (27, 'Hải Phòng',               'hai-phong',             '31'),
    (28, 'Hậu Giang',               'hau-giang',             '93'),
    (29, 'Hòa Bình',                'hoa-binh',              '17'),
    (30, 'Hưng Yên',                'hung-yen',              '33'),
    (31, 'Khánh Hòa',               'khanh-hoa',             '56'),
    (32, 'Kiên Giang',              'kien-giang',            '91'),
    (33, 'Kon Tum',                 'kon-tum',               '62'),
    (34, 'Lai Châu',                'lai-chau',              '12'),
    (35, 'Lâm Đồng',                'lam-dong',              '68'),
    (36, 'Lạng Sơn',                'lang-son',              '20'),
    (37, 'Lào Cai',                 'lao-cai',               '10'),
    (38, 'Long An',                 'long-an',               '80'),
    (39, 'Nam Định',                'nam-dinh',              '36'),
    (40, 'Nghệ An',                 'nghe-an',               '40'),
    (41, 'Ninh Bình',               'ninh-binh',             '37'),
    (42, 'Ninh Thuận',              'ninh-thuan',            '58'),
    (43, 'Phú Thọ',                 'phu-tho',               '25'),
    (44, 'Phú Yên',                 'phu-yen',               '54'),
    (45, 'Quảng Bình',              'quang-binh',            '44'),
    (46, 'Quảng Nam',               'quang-nam',             '49'),
    (47, 'Quảng Ngãi',              'quang-ngai',            '51'),
    (48, 'Quảng Ninh',              'quang-ninh',            '22'),
    (49, 'Quảng Trị',               'quang-tri',             '45'),
    (50, 'Sóc Trăng',               'soc-trang',             '94'),
    (51, 'Sơn La',                  'son-la',                '14'),
    (52, 'Tây Ninh',                'tay-ninh',              '72'),
    (53, 'Thái Bình',               'thai-binh',             '34'),
    (54, 'Thái Nguyên',             'thai-nguyen',           '19'),
    (55, 'Thanh Hóa',               'thanh-hoa',             '38'),
    (56, 'Thừa Thiên Huế',          'thua-thien-hue',        '46'),
    (57, 'Tiền Giang',              'tien-giang',            '82'),
    (58, 'TP. Hồ Chí Minh',         'ho-chi-minh',           '79'),
    (59, 'Trà Vinh',                'tra-vinh',              '84'),
    (60, 'Tuyên Quang',             'tuyen-quang',           '08'),
    (61, 'Vĩnh Long',               'vinh-long',             '86'),
    (62, 'Vĩnh Phúc',               'vinh-phuc',             '26'),
    (63, 'Yên Bái',                 'yen-bai',               '15');

-- ----------------------------------------------------------------
-- Seed districts: TP.HCM (province_id = 58)
-- ----------------------------------------------------------------
INSERT INTO districts (province_id, name, slug, code) VALUES
    (58, 'Quận 1',         'quan-1',         '760'),
    (58, 'Quận 2',         'quan-2',         '769'),
    (58, 'Quận 3',         'quan-3',         '761'),
    (58, 'Quận 4',         'quan-4',         '762'),
    (58, 'Quận 5',         'quan-5',         '763'),
    (58, 'Quận 6',         'quan-6',         '764'),
    (58, 'Quận 7',         'quan-7',         '765'),
    (58, 'Quận 8',         'quan-8',         '766'),
    (58, 'Quận 9',         'quan-9',         '767'),
    (58, 'Quận 10',        'quan-10',        '768'),
    (58, 'Quận 11',        'quan-11',        '770'),
    (58, 'Quận 12',        'quan-12',        '771'),
    (58, 'Bình Chánh',     'binh-chanh',     '785'),
    (58, 'Bình Tân',       'binh-tan',       '776'),
    (58, 'Bình Thạnh',     'binh-thanh',     '765'),
    (58, 'Gò Vấp',         'go-vap',         '772'),
    (58, 'Hóc Môn',        'hoc-mon',        '783'),
    (58, 'Nhà Bè',         'nha-be',         '787'),
    (58, 'Phú Nhuận',      'phu-nhuan',      '774'),
    (58, 'Tân Bình',       'tan-binh',       '775'),
    (58, 'Tân Phú',        'tan-phu',        '777'),
    (58, 'Thủ Đức',        'thu-duc',        '769'),
    (58, 'Củ Chi',         'cu-chi',         '783');

-- ----------------------------------------------------------------
-- Seed wards: Quận 1 TP.HCM
-- ----------------------------------------------------------------
WITH d1 AS (
    SELECT id FROM districts WHERE province_id = 58 AND slug = 'quan-1' LIMIT 1
)
INSERT INTO wards (district_id, name, slug) VALUES
    ((SELECT id FROM d1), 'Phường Bến Nghé',    'ben-nghe'),
    ((SELECT id FROM d1), 'Phường Bến Thành',   'ben-thanh'),
    ((SELECT id FROM d1), 'Phường Cô Giang',    'co-giang'),
    ((SELECT id FROM d1), 'Phường Cầu Kho',     'cau-kho'),
    ((SELECT id FROM d1), 'Phường Cầu Ông Lãnh','cau-ong-lanh'),
    ((SELECT id FROM d1), 'Phường Đa Kao',      'da-kao'),
    ((SELECT id FROM d1), 'Phường Nguyễn Cư Trinh','nguyen-cu-trinh'),
    ((SELECT id FROM d1), 'Phường Nguyễn Thái Bình','nguyen-thai-binh'),
    ((SELECT id FROM d1), 'Phường Phạm Ngũ Lão','pham-ngu-lao'),
    ((SELECT id FROM d1), 'Phường Tân Định',    'tan-dinh');

-- ----------------------------------------------------------------
-- Dev seed: 1 admin user (password = "Admin@123" bcrypt)
-- ----------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, email_verified) VALUES
    (
        gen_random_uuid(),
        'admin@nhatrovn.dev',
        '$2a$12$RnZwxT7sKqP1QbGKzY3.5.KYm8sZ9aW1V7bN2xM6cJ4dE8fH0iL3q',
        'Admin NhaTroVN',
        '0901234567',
        'ADMIN',
        'ACTIVE',
        TRUE
    );
