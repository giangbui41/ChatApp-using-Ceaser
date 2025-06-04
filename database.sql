CREATE DATABASE UDPchat2;
GO

USE UDPchat2;
GO

CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
	password VARCHAR(100) NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    port INT NOT NULL CONSTRAINT chk_port CHECK (port BETWEEN 1024 AND 65535),
    is_active BIT NOT NULL DEFAULT 1,
    last_seen DATETIME NOT NULL DEFAULT GETDATE(),
    created_at DATETIME NOT NULL DEFAULT GETDATE()
);

CREATE TABLE chat_logs (
    id INT IDENTITY(1,1) PRIMARY KEY,
    encrypted_text TEXT NOT NULL,
    decrypted_text TEXT NOT NULL,
    key_value INT NOT NULL,
    client_ip VARCHAR(50) NOT NULL,
    message_type VARCHAR(10) NOT NULL DEFAULT 'DIRECT',
    timestamp DATETIME NOT NULL DEFAULT GETDATE()
);

CREATE TABLE broadcast_messages (
    id INT IDENTITY(1,1) PRIMARY KEY,
    base_message_id INT NOT NULL,
    FOREIGN KEY (base_message_id) REFERENCES chat_logs(id)
);

CREATE TABLE broadcast_receivers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    broadcast_id INT NOT NULL,
    receiver_id INT NOT NULL,
    received_time DATETIME,
    FOREIGN KEY (broadcast_id) REFERENCES broadcast_messages(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);

CREATE INDEX idx_users_active ON users(is_active, last_seen);
CREATE INDEX idx_broadcast_receivers ON broadcast_receivers(broadcast_id, receiver_id);
