CREATE TABLE IF NOT EXISTS short_urls (
    short_code  VARCHAR(10) PRIMARY KEY,
    long_url    VARCHAR(2048) NOT NULL,
    click_count INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL
);