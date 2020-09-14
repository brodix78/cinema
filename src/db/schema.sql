CREATE TABLE halls (
   id INT,
   placeId SERIAL PRIMARY KEY,
   row VARCHAR(10),
   place VARCHAR(10)
);
CREATE TABLE customers (
   id SERIAL PRIMARY KEY,
   name TEXT,
   phone TEXT
);
CREATE TABLE sessions (
   id SERIAL PRIMARY KEY,
   hallId INT,
   time BIGINT,
   movie TEXT,
   price NUMERIC
);
CREATE TABLE reservations (
   sessionId INT NOT NULL,
   placeId INT NOT NULL,
   customerId INT NOT NULL,
   price NUMERIC,
   time BIGINT,
   CONSTRAINT sessions_id_fk FOREIGN KEY (sessionId) REFERENCES sessions (id),
   CONSTRAINT halls_placeId_fk FOREIGN KEY (placeId) REFERENCES halls (placeId),
   CONSTRAINT customers_id_fk FOREIGN KEY (customerId) REFERENCES customers (id),
   CONSTRAINT sessions_place_key PRIMARY KEY (sessionId, placeId)
)