article_count = \
SELECT COUNT(*) FROM article;


author_count = \
SELECT COUNT(*) FROM author;

article_author_relation_count = \
SELECT COUNT(*) FROM article_author_relation;


address_count = \
SELECT COUNT(*) FROM address;

article_address_relation_count = \
SELECT COUNT(*) FROM article_address_relation;


source_count = \
SELECT COUNT(*) FROM source;

source_category_count = \
SELECT COUNT(*) FROM source_category;

source_source_category_relation_count = \
SELECT COUNT(*) FROM source_source_category_relation;

source_source_category_relation_count_weighted = \
SELECT COUNT(*) \
FROM article a \
	 INNER JOIN source b \
     ON a.source_id = b.source_id \
	 INNER JOIN source_source_category_relation c \
     ON b.source_id = c.source_id;


source_area_count = \
SELECT COUNT(*) FROM source_area;

source_source_area_relation_count = \
SELECT COUNT(*) FROM source_source_area_relation;

source_source_area_relation_count_weighted = \
SELECT COUNT(*) \
FROM article a \
	 INNER JOIN source b \
     ON a.source_id = b.source_id \
	 INNER JOIN source_source_area_relation c \
     ON b.source_id = c.source_id;


keyword_author_count = \
SELECT COUNT(*) FROM keyword_author;

article_keyword_author_relation_count = \
SELECT COUNT(*) FROM article_keyword_author_relation;


keyword_plus_count = \
SELECT COUNT(*) FROM keyword_plus;

article_keyword_plus_relation_count = \
SELECT COUNT(*) FROM article_keyword_plus_relation;


cited_reference_count = \
SELECT COUNT(*) FROM cited_reference;

article_cited_reference_relation_count = \
SELECT COUNT(*) FROM article_cited_reference_relation;

pages_count = \
SELECT SUM(pages) FROM article;

times_cited_count = \
SELECT SUM(times_cited) FROM article;


articles_by_year = \
SELECT year, COUNT(year) as count FROM article GROUP BY year ORDER BY year ASC \
LIMIT 40;

articles_by_type = \
SELECT type, COUNT(type) as count FROM article GROUP BY type ORDER BY count DESC;

most_cited_articles = \
SELECT GROUP_CONCAT(SUBSTRING_INDEX(c.name, ',', 1) ORDER BY b.author_order SEPARATOR ', ') AS authors, \
       a.year, \
       a.title, \
       d.name as source, \
	   a.times_cited \
FROM article a \
     INNER JOIN article_author_relation b \
     ON a.article_id = b.article_id \
     INNER JOIN author c \
     ON c.author_id = b.author_id \
     INNER JOIN source d \
     ON a.source_id = d.source_id \
GROUP BY a.article_id \
ORDER BY times_cited DESC \
LIMIT 10;


articles_by_author_count = \
SELECT c AS authors, \
       COUNT(c) AS count \
FROM (SELECT COUNT(author_id) AS c, \
             article_id \
      FROM article_author_relation \
      GROUP BY article_id \
      ORDER BY c ASC \
) AS t \
GROUP BY t.c \
ORDER BY t.c \
LIMIT 40;

authors_by_author_order = \
SELECT author_order, COUNT(author_id) AS count \
FROM article_author_relation \
GROUP BY author_order \
LIMIT 40;

most_productive_authors = \
SELECT a.name AS name, \
       COUNT(b.author_id) AS count \
FROM author a \
     INNER JOIN article_author_relation b \
	 ON a.author_id = b.author_id \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 40;

most_productive_authors_order = \
SELECT a.name AS name, \
       COUNT(a.name) AS count \
FROM author a \
     INNER JOIN article_author_relation b \
     ON a.author_id = b.author_id \
WHERE b.author_order = ? \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 10;


most_mentioned_addresses = \
SELECT a.name AS name, \
       COUNT(b.address_id) AS count \
FROM address a \
     INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 40;

most_mentioned_countries = \
SELECT IF( \
         STRCMP( \
           TRIM(TRAILING '.' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', -1)), \
           'USA' \
		 ), \
		 TRIM(TRAILING '.' FROM SUBSTRING_INDEX(a.name, ', ', -1)), \
         'USA' \
	   ) AS country_name, \
       COUNT(b.address_id) AS count \
FROM address a \
     INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
GROUP BY country_name \
ORDER BY count DESC, country_name \
LIMIT 40;

most_mentioned_countries_states = \
SELECT IF( \
         STRCMP( \
           TRIM(TRAILING '.' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', -1)), \
           'USA' \
		 ), \
		 TRIM(TRAILING '.' FROM SUBSTRING_INDEX(a.name, ', ', -1)), \
         CONCAT( \
           SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', 1), \
           ' ', \
		   TRIM(TRAILING '.' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', -1)) \
		 ) \
	   ) AS country_name, \
       COUNT(b.address_id) AS count \
FROM address a \
     INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
GROUP BY country_name \
ORDER BY count DESC, country_name \
LIMIT 40;

most_mentioned_countries_states_zips = \
SELECT TRIM(TRAILING '.' FROM SUBSTRING_INDEX(a.name, ', ', -1)) AS country_name, \
       COUNT(b.address_id) AS count \
FROM address a \
     INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
GROUP BY country_name \
ORDER BY count DESC, country_name \
LIMIT 40;

most_mentioned_institutions = \
SELECT IF( \
         STRCMP(LEFT(a.name, 1), '['), \
		 SUBSTRING_INDEX(a.name, ', ', 1), \
         SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, '] ', -1), ', ', 1) \
 	   ) AS institution_name, \
       COUNT(b.address_id) AS count \
FROM address a \
     INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
GROUP BY institution_name \
ORDER BY count DESC, institution_name \
LIMIT 40;


most_utilized_sources = \
SELECT b.name AS name, \
       COUNT(a.source_id) AS count \
FROM article a \
	 INNER JOIN source b \
	 ON a.source_id = b.source_id \
GROUP BY a.source_id \
ORDER BY count DESC, name \
LIMIT 40;

most_utilized_source_categories = \
SELECT a.name AS name, \
       COUNT(b.source_category_id) AS count \
FROM source_category a \
     INNER JOIN source_source_category_relation b \
	 ON a.source_category_id = b.source_category_id \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 40;

most_utilized_source_categories_weighted = \
SELECT d.name AS name, \
       COUNT(d.name) AS count \
FROM article a \
	 INNER JOIN source b \
     ON a.source_id = b.source_id \
	 INNER JOIN source_source_category_relation c \
     ON b.source_id = c.source_id \
	 INNER JOIN source_category d \
	 ON c.source_category_id = d.source_category_id \
GROUP BY d.name \
ORDER BY count DESC, name \
LIMIT 40;


most_utilized_source_areas = \
SELECT a.name AS name, \
       COUNT(b.source_area_id) AS count \
FROM source_area a \
     INNER JOIN source_source_area_relation b \
	 ON a.source_area_id = b.source_area_id \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 40;

most_utilized_source_areas_weighted = \
SELECT d.name AS name, \
       COUNT(d.name) AS count \
FROM article a \
	 INNER JOIN source b \
     ON a.source_id = b.source_id \
	 INNER JOIN source_source_area_relation c \
     ON b.source_id = c.source_id \
	 INNER JOIN source_area d \
	 ON c.source_area_id = d.source_area_id \
GROUP BY d.name \
ORDER BY count DESC, name \
LIMIT 40;


most_mentioned_keywords_author = \
SELECT a.name AS name, \
       COUNT(b.keyword_author_id) AS count \
FROM keyword_author a \
     INNER JOIN article_keyword_author_relation b \
	 ON a.keyword_author_id = b.keyword_author_id \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 40;

most_mentioned_keywords_plus = \
SELECT a.name AS name, \
       COUNT(b.keyword_plus_id) AS count \
FROM keyword_plus a \
     INNER JOIN article_keyword_plus_relation b \
	 ON a.keyword_plus_id = b.keyword_plus_id \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 40;


most_cited_references = \
SELECT a.name AS name, \
       COUNT(b.cited_reference_id) AS count \
FROM cited_reference a \
     INNER JOIN article_cited_reference_relation b \
	 ON a.cited_reference_id = b.cited_reference_id \
GROUP BY a.name \
ORDER BY count DESC, name \
LIMIT 40;

most_cited_years = \
SELECT CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', 2), ', ', -1) AS UNSIGNED) AS year, \
       COUNT(b.cited_reference_id) AS count \
FROM cited_reference a \
     INNER JOIN article_cited_reference_relation b \
	 ON a.cited_reference_id = b.cited_reference_id \
GROUP BY year \
HAVING (year > 0 AND year <= YEAR(CURDATE())) \
ORDER BY year DESC \
LIMIT 40;

most_cited_authors = \
SELECT SUBSTRING_INDEX(a.name, ', ', 1) AS author_name, \
       COUNT(b.cited_reference_id) AS count \
FROM cited_reference a \
     INNER JOIN article_cited_reference_relation b \
	 ON a.cited_reference_id = b.cited_reference_id \
GROUP BY author_name \
ORDER BY count DESC, author_name \
LIMIT 40;


all_authors = \
SELECT name FROM author ORDER BY name;

all_addresses = \
SELECT name FROM address WHERE name!='NONE';

all_sources = \
SELECT name FROM source ORDER BY name;

all_source_categories = \
SELECT name FROM source_category ORDER BY name;

all_source_areas = \
SELECT name FROM source_area ORDER BY name;

all_keywords_author = \
SELECT name FROM keyword_author ORDER BY name;

all_keywords_plus = \
SELECT name FROM keyword_plus ORDER BY name;


# network relations:

source_categories_by_source = \
SELECT a.name AS source_name, \
       c.name AS source_category_name \
FROM source a \
	 INNER JOIN source_source_category_relation b \
	 ON a.source_id = b.source_id \
	 INNER JOIN source_category c \
	 ON b.source_category_id = c.source_category_id \
ORDER BY source_name, source_category_name ASC;

source_categories_by_source_weighted = \
SELECT b.name AS source_name, \
       d.name AS source_category_name \
FROM article a \
	 INNER JOIN source b \
     ON a.source_id = b.source_id \
	 INNER JOIN source_source_category_relation c \
     ON b.source_id = c.source_id \
	 INNER JOIN source_category d \
	 ON c.source_category_id = d.source_category_id \
ORDER BY a.article_id, source_name, source_category_name ASC;


source_areas_by_source = \
SELECT a.name AS source_name, \
       c.name AS source_area_name \
FROM source a \
	 INNER JOIN source_source_area_relation b \
	 ON a.source_id = b.source_id \
	 INNER JOIN source_area c \
	 ON b.source_area_id = c.source_area_id \
ORDER BY source_name, source_area_name ASC;

source_areas_by_source_weighted = \
SELECT b.name AS source_name, \
       d.name AS source_area_name \
FROM article a \
	 INNER JOIN source b \
     ON a.source_id = b.source_id \
	 INNER JOIN source_source_area_relation c \
     ON b.source_id = c.source_id \
	 INNER JOIN source_area d \
	 ON c.source_area_id = d.source_area_id \
ORDER BY a.article_id, source_name, source_area_name ASC;


coauthorships = \
SELECT c.article_id AS article_id, \
       a.name AS author_name \
FROM author a \
	 INNER JOIN article_author_relation b \
	 ON a.author_id = b.author_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, author_name ASC;


country_ties = \
SELECT DISTINCT \
       c.article_id AS article_id, \
       IF( \
         STRCMP( \
           TRIM(TRAILING '.' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', -1)), \
           'USA' \
		 ), \
		 TRIM(TRAILING '.' FROM SUBSTRING_INDEX(a.name, ', ', -1)), \
         'USA' \
	   ) AS country_name \
FROM address a \
	 INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, country_name ASC;

country_ties_states = \
SELECT DISTINCT \
       c.article_id AS article_id, \
       IF( \
         STRCMP( \
           TRIM(TRAILING '.' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', -1)), \
           'USA' \
		 ), \
		 TRIM(TRAILING '.' FROM SUBSTRING_INDEX(a.name, ', ', -1)), \
         CONCAT( \
           SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', 1), \
           ' ', \
		   TRIM(TRAILING '.' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, ', ', -1), ' ', -1)) \
		 ) \
	   ) AS country_name \
FROM address a \
	 INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, country_name ASC;

country_ties_states_zips = \
SELECT DISTINCT \
       c.article_id AS article_id, \
	   TRIM(TRAILING '.' FROM SUBSTRING_INDEX(a.name, ', ', -1)) AS country_name \
FROM address a \
	 INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, country_name ASC;

institutional_ties = \
SELECT DISTINCT \
       c.article_id AS article_id, \
	   IF( \
         STRCMP(LEFT(a.name, 1), '['), \
		 SUBSTRING_INDEX(a.name, ', ', 1), \
         SUBSTRING_INDEX(SUBSTRING_INDEX(a.name, '] ', -1), ', ', 1) \
 	   ) AS institution_name \
FROM address a \
	 INNER JOIN article_address_relation b \
	 ON a.address_id = b.address_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, institution_name ASC;


keywords_author_relatedness = \
SELECT c.article_id AS article_id, \
	   a.name AS keyword_author_name \
FROM keyword_author a \
	 INNER JOIN article_keyword_author_relation b \
	 ON a.keyword_author_id = b.keyword_author_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, keyword_author_name ASC;

keywords_plus_relatedness = \
SELECT c.article_id AS article_id, \
	   a.name AS keyword_plus_name \
FROM keyword_plus a \
	 INNER JOIN article_keyword_plus_relation b \
	 ON a.keyword_plus_id = b.keyword_plus_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, keyword_plus_name ASC;


cocitations = \
SELECT c.article_id AS article_id, \
	   SUBSTRING_INDEX(a.name, ', ', 2) AS cited_reference_name \
FROM cited_reference a \
	 INNER JOIN article_cited_reference_relation b \
	 ON a.cited_reference_id = b.cited_reference_id \
	 INNER JOIN article c \
	 ON b.article_id = c.article_id \
ORDER BY article_id, cited_reference_name ASC;
