import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

class bibx_data_in {

	// used in property loading
	private static final String props_fileName = "properties/bibx_data_in_properties.txt";
	private static Properties props = new Properties();
	private static String database_url = "";
	private static String database_username = "";
	private static String database_password = "";
	// used in sql property loading
	private static final String props_sqlFileName = "properties/bibx_data_in_query_properties.sql";
	private static Properties props_sql = new Properties();

	// get the date of runtime
	private static java.util.Date date = new java.util.Date();
	private static SimpleDateFormat rundateformat = new SimpleDateFormat("HH':'mm':'ss");

	// sql
	private static PreparedStatement stm = null;
	private static ResultSet rsl = null;
	private static Connection conn = null;

	public static void main(String[] args) throws IOException, SQLException {
		
		if (args.length == 0 || args == null) {
			System.out.println("Please provide a file name as an argument.");
			System.exit(0);
		}
		if (!args[0].endsWith(".txt")) {
			args[0] = args[0]+".txt";
		}
		
		System.out.println("BibX started at: "+rundateformat.format(date)+".\n");
		long startTime = System.currentTimeMillis();

		// load all properties
		props.load(new FileInputStream(props_fileName));
		database_url = props.getProperty("database_url", "jdbc:mysql://localhost:3306/bibx?useSSL=false");
		database_username = props.getProperty("database_username", "");
		database_password = props.getProperty("database_password", "");

		// load the sql property file
		props_sql.load(new FileInputStream(props_sqlFileName));
/* old load for the sql property file
		InputStream is = null;
		is = wos_in.class.getResourceAsStream("/" + propsFileName);
		props.load(is);
		is.close();
*/			
		// sql connection
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Cannot find the database driver: com.mysql.jdbc.Driver.", e);
		}
		if (database_username.equals("") || database_password.equals("")) {
			System.out.println("Connecting to database: "+database_url);
			Console cons = System.console();
			while (database_username.equals("")) {
				database_username = cons.readLine("Database username: ");
			}
			while (database_password.equals("")) {
				char[] pwd = cons.readPassword("Database password: ");
				database_password = new String(pwd);
			}
		}
		conn = DriverManager.getConnection(database_url, database_username, database_password);
		System.out.println("Connected to database: "+database_url);
		conn.setAutoCommit(false);
		
		BufferedReader bufRead = new BufferedReader(new FileReader(args[0]));
		String myLine = null;
		bibx_article arti = null; // custom class
		int article_count = 0;
		int affected_row_count = 0;
		int iterator_counter = 0;

		// Order of WOS export field codes:
		// FN,VR,
		// PT,AU,AF,TI,SO,LA,DT,DE,ID,AB,C1,RP,EM,RI,OI,FU,FX,CR,NR,TC,Z9,U1,U2,PU,PI,PA,SN,J9,JI,PD,PY,VL,IS,BP,EP,DI,PG,WC,SC,GA,UT,ER,
		// EF
		System.out.println("Reading and storing articles:");
		myLine = bufRead.readLine();
		do {
			if (myLine.startsWith("PT ")) {
				arti = new bibx_article();
				article_count++;
//				System.out.println("\n***NEW ARTICLE*** Article_count: "+article_count);
				if (article_count % 100 == 0) {
					System.out.print("|");
				} else if (article_count % 10 == 0) {
					System.out.print("o");
				} else {
					System.out.print(".");
				}
				
				// start parsing stuff for one article
				myLine = bufRead.readLine();
				while (!(myLine.startsWith("PT ") || myLine.startsWith("ER"))) {
				
					if (myLine.startsWith("AU ")) {
						// do stuff with author(s)
						arti.authors.add(myLine.substring(3));
						// check for additional authors
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							arti.authors.add(myLine.substring(3));
						}
//						System.out.println("Authors: "+arti.authors);

					} else if (myLine.startsWith("TI ")) {
						// do stuff with title
						String titlestring = "";
						titlestring = myLine.substring(3);
						// check for additional title lines
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							titlestring += " "+myLine.substring(3);
						}
						arti.title = titlestring;
//						System.out.println("Title: "+arti.title);
						
					} else if (myLine.startsWith("SO ")) {
						// do stuff with source
						arti.source = myLine.substring(3);
//						System.out.println("Source: "+arti.source);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("DT ")) {
						// do stuff with type
						arti.type = myLine.substring(3);
//						System.out.println("Source: "+arti.type);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("DE ")) {
						// do stuff with author keywords
						String keyword_authorstring = "";
						keyword_authorstring = myLine.substring(3);
						// check for additional author keyword lines
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							keyword_authorstring += " "+myLine.substring(3);
						}
						arti.keywords_author = Arrays.asList(keyword_authorstring.split("; "));
//						System.out.println("Author keywords: "+arti.keywords_author);

					} else if (myLine.startsWith("ID ")) {
						// do stuff with keywords plus
						String keyword_plusstring = "";
						keyword_plusstring = myLine.substring(3);
						// check for additional keywords plus lines
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							keyword_plusstring += " "+myLine.substring(3);
						}
						arti.keywords_plus = Arrays.asList(keyword_plusstring.split("; "));
//						System.out.println("Keywords plus: "+arti.keywords_plus);

					} else if (myLine.startsWith("AB ")) {
						// do stuff with abstract
						arti.abs = myLine.substring(3);
						// check for additional abstract lines
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							arti.abs += "\n"+myLine.substring(3);
						}
//						System.out.println("Abstract: "+arti.abs);

					} else if (myLine.startsWith("C1 ")) {
						// do stuff with address(es)
						arti.addresses.clear();
						arti.addresses.add(myLine.substring(3));
						// check for additional addresses
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							arti.addresses.add(myLine.substring(3));
						}
//						System.out.println("Addresses: "+arti.addresses);

					} else if (myLine.startsWith("CR ")) {
						// do stuff with cited references
						arti.cited_references.add(myLine.substring(3));
						// check for additional cited references
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							arti.cited_references.add(myLine.substring(3));
						}
//						System.out.println("Cited reference count: "+arti.cited_references.size());

					} else if (myLine.startsWith("NR ")) {
						// do stuff with number of cited references
						arti.number_of_references = Short.parseShort(myLine.substring(3));
//						System.out.println("Number of cited references: "+arti.number_of_references);
						myLine = bufRead.readLine();
						
					} else if (myLine.startsWith("TC ")) {
						// do stuff with times cited
						arti.times_cited = Short.parseShort(myLine.substring(3));
//						System.out.println("Times cited: "+arti.times_cited);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("PY ")) {
						// do stuff with publication year
						arti.year = Short.parseShort(myLine.substring(3));
//						System.out.println("Year: "+arti.year);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("VL ")) {
						// do stuff with volume
						arti.volume = myLine.substring(3);
//						System.out.println("Volume: "+arti.volume);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("IS ")) {
						// do stuff with issue
						arti.issue = myLine.substring(3);
//						System.out.println("Issue: "+arti.issue);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("BP ")) {
						// do stuff with beginning page number
						arti.page_begin = myLine.substring(3);
//						System.out.println("Page_begin: "+arti.page_begin);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("EP ")) {
						// do stuff with ending page number
						arti.page_end = myLine.substring(3);
//						System.out.println("Page_end: "+arti.page_end);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("PG ")) {
						// do stuff with number of pages
						arti.pages = Short.parseShort(myLine.substring(3));
//						System.out.println("Pages: "+arti.pages);
						myLine = bufRead.readLine();

					} else if (myLine.startsWith("WC ")) {
						// do stuff with Web of Science source categories
						String categorystring = "";
						categorystring = myLine.substring(3);
						// check for additional Web of Science source category lines
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							categorystring += " "+myLine.substring(3);
						}
						arti.source_categories = Arrays.asList(categorystring.split("; "));
//						System.out.println("Web of Science source categories: "+arti.source_categories);

					} else if (myLine.startsWith("SC ")) {
						// do stuff with source research areas
						String areastring = "";
						areastring = myLine.substring(3);
						// check for additional source research area lines
						while ((myLine = bufRead.readLine()).startsWith("   ")) {
							areastring += " "+myLine.substring(3);
						}
						arti.source_areas = Arrays.asList(areastring.split("; "));
//						System.out.println("Source research areas: "+arti.source_areas);

					} else {
						myLine = bufRead.readLine();
					}

				} // while inside a PT, ie. one article

				// done parsing one article, now should insert it into the db
				stm = conn.prepareStatement(props_sql.getProperty("insert_article"));
				stm.setString(1, arti.title);
				stm.setShort(2, arti.year);
				stm.setString(3, arti.volume);
				stm.setString(4, arti.issue);
				stm.setString(5, arti.page_begin);
				stm.setString(6, arti.page_end);
				stm.setShort(7, arti.pages);
				stm.setShort(8, arti.number_of_references);
				stm.setShort(9, arti.times_cited);
				stm.setString(10, arti.type);
				stm.executeUpdate();
//				System.out.println("Rows affected (insert article): "+stm.getUpdateCount());
				
				// get article id
				stm = conn.prepareStatement(props_sql.getProperty("get_last_insert_id"));
				rsl = stm.executeQuery();
				rsl.first();
				arti.article_id = rsl.getInt(1);
//				System.out.println("Article id: "+arti.article_id);
				
				// insert article abstract if there was an abstract
				if (!arti.abs.equals("")) {
					stm = conn.prepareStatement(props_sql.getProperty("insert_abstract"));
					stm.setInt(1, arti.article_id);
					stm.setString(2, arti.abs);
					stm.executeUpdate();
				}
				
				// insert article authors
				stm = conn.prepareStatement(props_sql.getProperty("insert_author"));
				for (String author : arti.authors) {
					stm.setString(1, author);
					stm.setString(2, author);
					stm.executeUpdate();
					affected_row_count += stm.getUpdateCount();
				}
//				System.out.println("Inserted "+affected_row_count+" new authors.");
				affected_row_count = 0;
				
				// get ids for authors and insert them into the relation-table
				iterator_counter = 0;
				for (String author : arti.authors) {
					iterator_counter++;
					stm = conn.prepareStatement(props_sql.getProperty("get_author_id"));
					stm.setString(1, author);
					rsl = stm.executeQuery();
					rsl.first();
					stm = conn.prepareStatement(props_sql.getProperty("insert_article_author_relation"));
					stm.setInt(1, arti.article_id);
					stm.setInt(2, rsl.getInt(1));
					stm.setInt(3, iterator_counter);
					stm.executeUpdate();
					affected_row_count += stm.getUpdateCount();
				}
//				System.out.println("Inserted "+affected_row_count+" article-author-relations.");
				affected_row_count = 0;
				iterator_counter = 0;

				// insert article addresses
				stm = conn.prepareStatement(props_sql.getProperty("insert_address"));
				for (String address : arti.addresses) {
					stm.setString(1, address);
					stm.setString(2, address);
					stm.executeUpdate();
					affected_row_count += stm.getUpdateCount();
				}
//				System.out.println("Inserted "+affected_row_count+" new addresses.");
				affected_row_count = 0;
				
				// get ids for addresses and insert them into the relation-table
				for (String address : arti.addresses) {
					stm = conn.prepareStatement(props_sql.getProperty("get_address_id"));
					stm.setString(1, address);
					rsl = stm.executeQuery();
					rsl.first();
					stm = conn.prepareStatement(props_sql.getProperty("insert_article_address_relation"));
					stm.setInt(1, arti.article_id);
					stm.setInt(2, rsl.getInt(1));
					stm.executeUpdate();
					affected_row_count += stm.getUpdateCount();
				}
//				System.out.println("Inserted "+affected_row_count+" article-address-relations.");
				affected_row_count = 0;

				// insert source
				stm = conn.prepareStatement(props_sql.getProperty("insert_source"));
				stm.setString(1, arti.source);
				stm.setString(2, arti.source);
				stm.executeUpdate();
//				System.out.println("Inserted "+stm.getUpdateCount()+" new source(s).");
				boolean source_inserted = false;
				if (stm.getUpdateCount() == 1) {
					source_inserted = true;
				}

				// get source_id and update it to article
				stm = conn.prepareStatement(props_sql.getProperty("get_source_id"));
				stm.setString(1, arti.source);
				rsl = stm.executeQuery();
				rsl.first();
				arti.source_id = rsl.getInt(1);
				stm = conn.prepareStatement(props_sql.getProperty("update_article_source"));
				stm.setInt(1, arti.source_id);
				stm.setInt(2, arti.article_id);
				stm.executeUpdate();
//				System.out.println("Updated "+stm.getUpdateCount()+" article row(s) to add source_id "+arti.source_id+".");

				if (source_inserted) {
					// insert source categories
					stm = conn.prepareStatement(props_sql.getProperty("insert_source_category"));
					for (String source_category : arti.source_categories) {
						stm.setString(1, source_category);
						stm.setString(2, source_category);
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" new source categories.");
					affected_row_count = 0;
					
					// get ids for source categories and insert them into the relation-table
					for (String source_category : arti.source_categories) {
						stm = conn.prepareStatement(props_sql.getProperty("get_source_category_id"));
						stm.setString(1, source_category);
						rsl = stm.executeQuery();
						rsl.first();
						stm = conn.prepareStatement(props_sql.getProperty("insert_source_source_category_relation"));
						stm.setInt(1, arti.source_id);
						stm.setInt(2, rsl.getInt(1));
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" source-source_category-relations.");
					affected_row_count = 0;

					// insert source areas
					stm = conn.prepareStatement(props_sql.getProperty("insert_source_area"));
					for (String source_area : arti.source_areas) {
						stm.setString(1, source_area);
						stm.setString(2, source_area);
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" new source areas.");
					affected_row_count = 0;
					
					// get ids for source areas and insert them into the relation-table
					for (String source_area : arti.source_areas) {
						stm = conn.prepareStatement(props_sql.getProperty("get_source_area_id"));
						stm.setString(1, source_area);
						rsl = stm.executeQuery();
						rsl.first();
						stm = conn.prepareStatement(props_sql.getProperty("insert_source_source_area_relation"));
						stm.setInt(1, arti.source_id);
						stm.setInt(2, rsl.getInt(1));
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" source-source_area-relations.");
					affected_row_count = 0;
				}
				source_inserted = false;

				if (!arti.keywords_author.isEmpty()) {
					// insert article's author keywords
					stm = conn.prepareStatement(props_sql.getProperty("insert_keyword_author"));
					for (String keyword_author : arti.keywords_author) {
						stm.setString(1, keyword_author);
						stm.setString(2, keyword_author);
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" new author keywords.");
					affected_row_count = 0;
					
					// get ids for author keywords and insert them into the relation-table
					for (String keyword_author : arti.keywords_author) {
						stm = conn.prepareStatement(props_sql.getProperty("get_keyword_author_id"));
						stm.setString(1, keyword_author);
						rsl = stm.executeQuery();
						rsl.first();
						stm = conn.prepareStatement(props_sql.getProperty("insert_article_keyword_author_relation"));
						stm.setInt(1, arti.article_id);
						stm.setInt(2, rsl.getInt(1));
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" article-keyword_author-relations.");
					affected_row_count = 0;
				}

				if (!arti.keywords_plus.isEmpty()) {
					// insert article's keywords plus
					stm = conn.prepareStatement(props_sql.getProperty("insert_keyword_plus"));
					for (String keyword_plus : arti.keywords_plus) {
						stm.setString(1, keyword_plus);
						stm.setString(2, keyword_plus);
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" new keywords plus.");
					affected_row_count = 0;
					
					// get ids for keywords plus and insert them into the relation-table
					for (String keyword_plus : arti.keywords_plus) {
						stm = conn.prepareStatement(props_sql.getProperty("get_keyword_plus_id"));
						stm.setString(1, keyword_plus);
						rsl = stm.executeQuery();
						rsl.first();
						stm = conn.prepareStatement(props_sql.getProperty("insert_article_keyword_plus_relation"));
						stm.setInt(1, arti.article_id);
						stm.setInt(2, rsl.getInt(1));
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" article-keyword_plus-relations.");
					affected_row_count = 0;
				}

				if (!arti.cited_references.isEmpty()) {
					// insert article's cited references
					// the below is a trick to get unique values, as there are duplicate refs aka. data errors in WoS!
					HashSet<String> unique_cited_references = new HashSet<>(arti.cited_references);
					
					stm = conn.prepareStatement(props_sql.getProperty("insert_cited_reference"));
					for (String cited_reference : unique_cited_references) {
						stm.setString(1, cited_reference);
						stm.setString(2, cited_reference);
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" new cited references.");
					affected_row_count = 0;

					// get ids for cited references and insert them into the relation-table
					for (String cited_reference : unique_cited_references) {
						stm = conn.prepareStatement(props_sql.getProperty("get_cited_reference_id"));
						stm.setString(1, cited_reference);
						rsl = stm.executeQuery();
						rsl.first();
						stm = conn.prepareStatement(props_sql.getProperty("insert_article_cited_reference_relation"));
						stm.setInt(1, arti.article_id);
						stm.setInt(2, rsl.getInt(1));
						stm.executeUpdate();
						affected_row_count += stm.getUpdateCount();
					}
//					System.out.println("Inserted "+affected_row_count+" article-cited_reference-relations.");
					affected_row_count = 0;
				}

				// commit after every article?
//				conn.commit();
			} else { // myLine does NOT start with "PT "
				myLine = bufRead.readLine();
			}
		} while (myLine != null);

		conn.commit();
		stm.close();
		conn.close();
		System.out.println("\nStored "+article_count+" articles in "+(System.currentTimeMillis()-startTime)/1000F+" secs.");
		System.out.println("Database connection closed.\n");

		System.out.println("BibX finished at: "+rundateformat.format(new java.util.Date())+".");

	} // main
	
} // class
