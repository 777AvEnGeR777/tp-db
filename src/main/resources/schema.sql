CREATE TABLE IF NOT EXISTS users (
  about    TEXT,
  email    VARCHAR(50) NOT NULL UNIQUE,
  fullname VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) NOT NULL PRIMARY KEY
);

CREATE UNIQUE INDEX IF NOT EXISTS lower_nicknames ON users ((lower(nickname)));
CREATE UNIQUE INDEX IF NOT EXISTS lower_email ON users ((lower(email)));

CREATE TABLE IF NOT EXISTS forum (
  posts   BIGINT DEFAULT 0,
  slug    TEXT         NOT NULL UNIQUE,
  threads INT    DEFAULT 0,
  title   VARCHAR(255) NOT NULL,
  "user"  VARCHAR(50)  NOT NULL,
  FOREIGN KEY ("user") REFERENCES users (nickname)
);

CREATE UNIQUE INDEX IF NOT EXISTS lower_slug_index ON forum((lower(slug)));

CREATE TABLE IF NOT EXISTS thread (
  id      SERIAL       NOT NULL PRIMARY KEY,
  author  VARCHAR(50)  NOT NULL,
  created TIMESTAMPTZ  NOT NULL,
  forum   TEXT         NOT NULL,
  message TEXT         NOT NULL,
  slug    TEXT UNIQUE,
  title   VARCHAR(255) NOT NULL,
  votes   INT DEFAULT 0,
  FOREIGN KEY (author) REFERENCES users (nickname),
  FOREIGN KEY (forum) REFERENCES forum (slug)
);

CREATE UNIQUE INDEX IF NOT EXISTS lower_thread_slug ON thread((lower(slug)));

CREATE TABLE IF NOT EXISTS post (
  id        BIGSERIAL   NOT NULL PRIMARY KEY,
  author    VARCHAR(50) NOT NULL,
  created   TIMESTAMPTZ NOT NULL,
  forum     TEXT        NOT NULL,
  is_edited BOOLEAN     NOT NULL DEFAULT FALSE,
  message   TEXT        NOT NULL,
  parent    BIGINT      NOT NULL DEFAULT 0,
  thread    INT         NOT NULL,
  path      BIGINT[]    NOT NULL,
  FOREIGN KEY (author) REFERENCES users (nickname),
  FOREIGN KEY (forum) REFERENCES forum (slug),
  FOREIGN KEY (thread) REFERENCES thread (id)
);

CREATE TABLE IF NOT EXISTS vote (
  nickname VARCHAR(50) NOT NULL,
  voice    SMALLINT    NOT NULL,
  thread   INT         NOT NULL,
  PRIMARY KEY (nickname, thread),
  FOREIGN KEY (nickname) REFERENCES users (nickname),
  FOREIGN KEY (thread) REFERENCES thread (id)
);

