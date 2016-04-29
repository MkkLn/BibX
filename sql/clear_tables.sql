USE bibxdb;

DELETE FROM article_address_relation WHERE article_id > 0;
DELETE FROM article_author_relation WHERE article_id > 0;
DELETE FROM article_keyword_author_relation WHERE article_id > 0;
DELETE FROM article_keyword_plus_relation WHERE article_id > 0;
DELETE FROM article_cited_reference_relation WHERE article_id > 0;
DELETE FROM address WHERE address_id > 0;
DELETE FROM author WHERE author_id > 0;
DELETE FROM article WHERE article_id > 0;
DELETE FROM source_source_category_relation WHERE source_id > 0;
DELETE FROM source_category WHERE source_category_id > 0;
DELETE FROM source_source_area_relation WHERE source_id > 0;
DELETE FROM source_area WHERE source_area_id > 0;
DELETE FROM source WHERE source_id > 0;
DELETE FROM keyword_author WHERE keyword_author_id > 0;
DELETE FROM keyword_plus WHERE keyword_plus_id > 0;
DELETE FROM cited_reference WHERE cited_reference_id > 0;
