articles = \
SELECT a.article_id, \
       GROUP_CONCAT(DISTINCT SUBSTRING_INDEX(c.name, ',', 1) ORDER BY b.author_order SEPARATOR ', ') AS authors, \
       a.year, \
       a.title, \
       d.name AS source, \
       a.type, \
       a.pages, \
       a.number_of_references, \
	   a.times_cited, \
       GROUP_CONCAT(DISTINCT SUBSTRING_INDEX(g.name, ',', 1) ORDER BY g.name SEPARATOR ', ') AS keyword_author, \
       GROUP_CONCAT(DISTINCT SUBSTRING_INDEX(i.name, ',', 1) ORDER BY i.name SEPARATOR ', ') AS keyword_plus, \
       e.txt as abstract \
FROM article a \
     INNER JOIN article_author_relation b \
     ON a.article_id = b.article_id \
     INNER JOIN author c \
     ON c.author_id = b.author_id \
     INNER JOIN source d \
     ON a.source_id = d.source_id \
     LEFT JOIN abstract e \
     ON a.article_id = e.article_id \
     LEFT JOIN article_keyword_author_relation f \
     ON a.article_id = f.article_id \
     LEFT JOIN keyword_author g \
     ON f.keyword_author_id = g.keyword_author_id \
     LEFT JOIN article_keyword_plus_relation h \
     ON a.article_id = h.article_id \
     LEFT JOIN keyword_plus i \
     ON h.keyword_plus_id = i.keyword_plus_id \
GROUP BY a.article_id \
ORDER BY a.article_id ASC;
