DROP SCHEMA IF EXISTS balaam CASCADE;

CREATE SCHEMA balaam;

CREATE OR REPLACE FUNCTION balaam.update_last_modified_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TABLE balaam.users(
  id                    SERIAL PRIMARY KEY NOT NULL UNIQUE,
  username              VARCHAR(71) NOT NULL UNIQUE,
  github_access_token   VARCHAR(100) DEFAULT NULL,
  last_modified_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT Now(),
  inserted_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT Now()
)
WITH (OIDS=FALSE);
CREATE TRIGGER update_users_last_modified_at BEFORE
  UPDATE ON balaam.users FOR EACH ROW EXECUTE PROCEDURE balaam.update_last_modified_at_column();

CREATE TABLE balaam.github_branches(
  id                SERIAL PRIMARY KEY NOT NULL UNIQUE,
  user_id           INTEGER NOT NULL UNIQUE REFERENCES balaam.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  owner             VARCHAR(100) NOT NULL,
  repo              VARCHAR(100) NOT NULL,
  branch            VARCHAR(248) NOT NULL,
  last_modified_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT Now(),
  inserted_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT Now()
)
WITH (OIDS=FALSE);
CREATE TRIGGER update_github_branches_last_modified_at BEFORE
  UPDATE ON balaam.github_branches FOR EACH ROW EXECUTE PROCEDURE balaam.update_last_modified_at_column();
