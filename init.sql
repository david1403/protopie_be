-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    user_name VARCHAR(255) NOT NULL,
    password VARCHAR(500) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Optional: Insert sample data for testing
-- INSERT INTO users (email, user_name, password, role)
-- VALUES ('admin@example.com', 'Admin User', 'encrypted_password_here', 'ADMIN');