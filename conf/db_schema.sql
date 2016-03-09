CREATE USER "user" NOSUPERUSER INHERIT CREATEROLE;

CREATE DATABASE hashtag_aggregator WITH OWNER = "user" ENCODING 'UTF8';

GRANT ALL PRIVILEGES ON DATABASE hashtag_aggregator to "user";

\connect hashtag_aggregator user

CREATE TABLE twitter_user (
  id bigserial primary key,
  twitter_username varchar NOT NULL,
  created_on timestamp with time zone NOT NULL
);

CREATE TABLE tweets (
  id bigserial primary key,
  user_id bigint NOT NULL,
  tweet_id varchar NOT NULL,
  text varchar NOT NULL,
  created_on timestamp with time zone NOT NULL
);