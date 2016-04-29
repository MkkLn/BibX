get_last_insert_id = \
SELECT LAST_INSERT_ID() AS a;


insert_article = \
INSERT INTO article \
(title, year, volume, issue, page_begin, page_end, pages, number_of_references, times_cited, type) \
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

insert_abstract = \
INSERT INTO abstract \
(article_id, txt) VALUES (?, ?);


insert_author = \
INSERT INTO author (name) \
SELECT ? FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM author \
    WHERE name=? \
) LIMIT 1;

get_author_id = \
SELECT author_id FROM author WHERE name = ?;

insert_article_author_relation = \
INSERT INTO article_author_relation (article_id, author_id, author_order) \
VALUES (?, ?, ?);


insert_address = \
INSERT INTO address (name) \
SELECT ? FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM address \
    WHERE name=? \
) LIMIT 1;

get_address_id = \
SELECT address_id FROM address WHERE name = ?;

insert_article_address_relation = \
INSERT INTO article_address_relation (article_id, address_id) \
VALUES (?, ?);


insert_source = \
INSERT INTO source (name) \
SELECT UPPER(?) FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM source \
    WHERE name=UPPER(?) \
) LIMIT 1;

get_source_id = \
SELECT source_id FROM source WHERE name = UPPER(?);

update_article_source = \
UPDATE article SET source_id = ? WHERE article_id = ?;


insert_source_category = \
INSERT INTO source_category (name) \
SELECT UPPER(?) FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM source_category \
    WHERE name=UPPER(?) \
) LIMIT 1;

get_source_category_id = \
SELECT source_category_id FROM source_category WHERE name = UPPER(?);

insert_source_source_category_relation = \
INSERT INTO source_source_category_relation (source_id, source_category_id) \
VALUES (?, ?);


insert_source_area = \
INSERT INTO source_area (name) \
SELECT UPPER(?) FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM source_area \
    WHERE name=UPPER(?) \
) LIMIT 1;

get_source_area_id = \
SELECT source_area_id FROM source_area WHERE name = UPPER(?);

insert_source_source_area_relation = \
INSERT INTO source_source_area_relation (source_id, source_area_id) \
VALUES (?, ?);


insert_keyword_author = \
INSERT INTO keyword_author (name) \
SELECT UPPER(?) FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM keyword_author \
    WHERE name=UPPER(?) \
) LIMIT 1;

get_keyword_author_id = \
SELECT keyword_author_id FROM keyword_author WHERE name = UPPER(?);

insert_article_keyword_author_relation = \
INSERT INTO article_keyword_author_relation (article_id, keyword_author_id) \
VALUES (?, ?);


insert_keyword_plus = \
INSERT INTO keyword_plus (name) \
SELECT UPPER(?) FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM keyword_plus \
    WHERE name=UPPER(?) \
) LIMIT 1;

get_keyword_plus_id = \
SELECT keyword_plus_id FROM keyword_plus WHERE name = UPPER(?);

insert_article_keyword_plus_relation = \
INSERT INTO article_keyword_plus_relation (article_id, keyword_plus_id) \
VALUES (?, ?);


insert_cited_reference = \
INSERT INTO cited_reference (name) \
SELECT UPPER(?) FROM (select 1) AS a \
WHERE NOT EXISTS( \
    SELECT name FROM cited_reference \
    WHERE name=UPPER(?) \
) LIMIT 1;

get_cited_reference_id = \
SELECT cited_reference_id FROM cited_reference WHERE name = UPPER(?);

insert_article_cited_reference_relation = \
INSERT INTO article_cited_reference_relation (article_id, cited_reference_id) \
VALUES (?, ?);
