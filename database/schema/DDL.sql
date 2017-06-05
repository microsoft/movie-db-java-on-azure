DROP DATABASE IF EXISTS moviedb;
CREATE DATABASE moviedb;

USE moviedb;

CREATE TABLE `movies` (
  `id` BIGINT(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT NOT NULL,
  `rating` FLOAT DEFAULT NULL,
  `image_uri` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`)
);