CREATE TABLE IF NOT EXISTS dining_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    campus VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    cuisine VARCHAR(255),
    average_price DECIMAL(8,2),
    distance_meters INT,
    rating DECIMAL(2,1),
    description VARCHAR(1000),
    signature_dish VARCHAR(255),
    image_emoji VARCHAR(255),
    visual_tone VARCHAR(255),
    taste_tags VARCHAR(255),
    meal_periods VARCHAR(255),
    dietary_tags VARCHAR(255),
    allergens VARCHAR(255),
    business_hours VARCHAR(255),
    data_version VARCHAR(255),
    data_updated_at DATE,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    INDEX idx_dining_enabled_rating (enabled, rating),
    INDEX idx_dining_price (average_price),
    INDEX idx_dining_distance (distance_meters)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS business_hours (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dining_item_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    closed BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT fk_business_item FOREIGN KEY (dining_item_id) REFERENCES dining_item(id) ON DELETE CASCADE,
    UNIQUE KEY uk_business_slot (dining_item_id, day_of_week, open_time, close_time),
    INDEX idx_business_day (day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tag_type VARCHAR(32) NOT NULL,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_tag_type_code (tag_type, tag_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS dining_item_tag (
    dining_item_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (dining_item_id, tag_id),
    CONSTRAINT fk_item_tag_item FOREIGN KEY (dining_item_id) REFERENCES dining_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    INDEX idx_item_tag_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
