USE bibxdb;

CREATE TABLE article (
  article_id INT unsigned NOT NULL auto_increment,
  type VARCHAR(255),
  source_id INT unsigned,
  title VARCHAR(255),
  year SMALLINT unsigned,
  volume VARCHAR(255),
  issue VARCHAR(255),
  page_begin VARCHAR(255),
  page_end VARCHAR(255),
  pages SMALLINT unsigned,
  number_of_references SMALLINT unsigned,
  times_cited SMALLINT unsigned,
  PRIMARY KEY (article_id)
);


CREATE TABLE author (
  author_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (author_id)
);

CREATE TABLE article_author_relation (
  article_id INT unsigned NOT NULL,
  author_id INT unsigned NOT NULL,
  author_order SMALLINT unsigned NOT NULL,
  PRIMARY KEY (article_id, author_id),
  FOREIGN KEY (article_id) REFERENCES article(article_id),
  FOREIGN KEY (author_id) REFERENCES author(author_id)
);


CREATE TABLE abstract (
	article_id INT unsigned NOT NULL,
	txt text,
	PRIMARY KEY (article_id),
	FOREIGN KEY (article_id) REFERENCES article(article_id) ON DELETE CASCADE
);


CREATE TABLE address (
  address_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(510) NOT NULL UNIQUE,
  PRIMARY KEY (address_id)
);

CREATE TABLE article_address_relation (
  article_id INT unsigned NOT NULL,
  address_id INT unsigned NOT NULL,
  PRIMARY KEY (article_id, address_id),
  FOREIGN KEY (article_id) REFERENCES article(article_id),
  FOREIGN KEY (address_id) REFERENCES address(address_id)
);


CREATE TABLE source (
  source_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (source_id)
);

ALTER TABLE article
ADD FOREIGN KEY (source_id)
REFERENCES source(source_id);


CREATE TABLE source_category (
  source_category_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (source_category_id)
);

CREATE TABLE source_source_category_relation (
  source_id INT unsigned NOT NULL,
  source_category_id INT unsigned NOT NULL,
  PRIMARY KEY (source_id, source_category_id),
  FOREIGN KEY (source_id) REFERENCES source(source_id),
  FOREIGN KEY (source_category_id) REFERENCES source_category(source_category_id)
);


CREATE TABLE source_area (
  source_area_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (source_area_id)
);

CREATE TABLE source_source_area_relation (
  source_id INT unsigned NOT NULL,
  source_area_id INT unsigned NOT NULL,
  PRIMARY KEY (source_id, source_area_id),
  FOREIGN KEY (source_id) REFERENCES source(source_id),
  FOREIGN KEY (source_area_id) REFERENCES source_area(source_area_id)
);


CREATE TABLE keyword_author (
  keyword_author_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (keyword_author_id)
);

CREATE TABLE article_keyword_author_relation (
  article_id INT unsigned NOT NULL,
  keyword_author_id INT unsigned NOT NULL,
  PRIMARY KEY (article_id, keyword_author_id),
  FOREIGN KEY (article_id) REFERENCES article(article_id),
  FOREIGN KEY (keyword_author_id) REFERENCES keyword_author(keyword_author_id)
);


CREATE TABLE keyword_plus (
  keyword_plus_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (keyword_plus_id)
);

CREATE TABLE article_keyword_plus_relation (
  article_id INT unsigned NOT NULL,
  keyword_plus_id INT unsigned NOT NULL,
  PRIMARY KEY (article_id, keyword_plus_id),
  FOREIGN KEY (article_id) REFERENCES article(article_id),
  FOREIGN KEY (keyword_plus_id) REFERENCES keyword_plus(keyword_plus_id)
);


CREATE TABLE cited_reference (
  cited_reference_id INT unsigned NOT NULL auto_increment,
  name VARCHAR(510) NOT NULL UNIQUE,
  PRIMARY KEY (cited_reference_id)
);

CREATE TABLE article_cited_reference_relation (
  article_id INT unsigned NOT NULL,
  cited_reference_id INT unsigned NOT NULL,
  PRIMARY KEY (article_id, cited_reference_id),
  FOREIGN KEY (article_id) REFERENCES article(article_id),
  FOREIGN KEY (cited_reference_id) REFERENCES cited_reference(cited_reference_id)
);
