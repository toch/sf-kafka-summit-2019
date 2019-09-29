DROP TABLE IF EXISTS ghosts, movies, ghosts_in_movies;
CREATE TABLE IF NOT EXISTS ghosts
(
  id SERIAL PRIMARY KEY,
  name TEXT,
  aka TEXT,
  singular_or_multiple TEXT,
  class_based_on_rpg TEXT,
  class_in_materials TEXT
);
CREATE TABLE IF NOT EXISTS movies
(
  id SERIAL PRIMARY KEY,
  name TEXT,
  year INT
);
CREATE TABLE IF NOT EXISTS ghosts_in_movies
(
  ghost_id INT NOT NULL REFERENCES ghosts(id),
  movie_id INT NOT NULL REFERENCES movies(id),
  id SERIAL PRIMARY KEY
);
\copy ghosts FROM 'ghostbusters-ghosts.csv' CSV
\copy movies FROM 'ghostbusters-movies.csv' CSV
\copy ghosts_in_movies (ghost_id, movie_id) FROM 'ghostbusters-ghosts_in_movies.csv' CSV