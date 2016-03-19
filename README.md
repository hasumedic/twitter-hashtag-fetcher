# Objective

Little Play! application that fetches Tweets with a certain hashtag configurable through configuration.

# Requirements

- PostgreSQL 9.4+
- Scala 2.11.7+
- Sbt 0.13.8+

# Usage

* Import DB schema (conf/db_schema.sql) into PostgreSQL
* Create a conf/twitter.conf file from conf/twitter.conf.dist containing your Twitter API keys
* Determine the target hashtag in the conf/twitter.conf file
* Auto-generate the DB classes using the SBT Command: *GenerateJOOQ* 
* Run the application to start the Actor that fetches tweets for the hashtag

#Disclaimer

This project is mainly for learning purposes. Please **DO NOT** use this code in Production environments.