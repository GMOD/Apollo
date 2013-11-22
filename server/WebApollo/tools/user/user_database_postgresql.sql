DROP TABLE IF EXISTS users;

CREATE TABLE users(
	user_id SERIAL,
	username VARCHAR,
	password VARCHAR,
	PRIMARY KEY (user_id)
);


DROP TABLE IF EXISTS tracks;

CREATE TABLE tracks(
	track_id SERIAL,
	track_name VARCHAR,
	PRIMARY KEY (track_id)
);


DROP TABLE IF EXISTS permissions;

CREATE TABLE permissions(
	track_id INT,
	user_id INT,
	permission INT,
	PRIMARY KEY(track_id, user_id),
	FOREIGN KEY(track_id) REFERENCES tracks(track_id),
	FOREIGN KEY(user_id) REFERENCES users(user_id)
);
