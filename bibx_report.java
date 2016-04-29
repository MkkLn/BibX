import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

class bibx_report {

	// used in property loading
	private static final String props_fileName = "properties/bibx_report_properties.txt";
	private static Properties props = new Properties();
	private static String database_url = "";
	private static String database_username = "";
	private static String database_password = "";
	private static String output_directory = "";
	private static boolean output_directory_append_date = false;
	private static String report_files_subdirectory = "";
	private static String output_filename_prefix = "";
	private static boolean output_filename_append_date = true;
	private static String command_r = "";
	private static String command_tex2pdf = "";
	private static String appendix_visibility = "";
	private static int table_rows_max = 0;
	private static int network_edgesize_min_coauthorships = 1;
	private static int network_edgesize_min_country_ties = 1;
	private static int network_edgesize_min_country_ties_states = 1;
	private static int network_edgesize_min_country_ties_states_zips = 1;
	private static int network_edgesize_min_institutional_ties = 1;
	private static int network_edgesize_min_categories = 1;
	private static int network_edgesize_min_categories_weighted = 1;
	private static int network_edgesize_min_areas = 1;
	private static int network_edgesize_min_areas_weighted = 1;
	private static int network_edgesize_min_keywords_author_relatedness = 1;
	private static int network_edgesize_min_keywords_plus_relatedness = 1;
	private static int network_edgesize_min_cocitations = 1;
	// used in sql property loading
	private static final String props_sqlFileName = "properties/bibx_report_query_properties.sql";
	private static Properties props_sql = new Properties();

	// used for drawing networks, can be 'anything'
	private static final String tieSeparator = "---";

	// get the date of runtime
	private static java.util.Date date = new java.util.Date();
	private static SimpleDateFormat dateformat = new SimpleDateFormat("'_'yyMMdd'_'HHmmss");
	private static String rundate = dateformat.format(date);
	private static SimpleDateFormat rundateformat = new SimpleDateFormat("HH':'mm':'ss");

	// sql
	private static PreparedStatement stm = null;
	private static ResultSet rsl = null;
	private static Connection conn = null;

	// for command line executions
	private static Runtime rt = null;
	private static Process p = null;

	public static void main(String[] args) throws IOException, SQLException {

//		System.out.println(new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator());
		System.out.println("BibX started at: "+rundateformat.format(date)+".\n");
		long startTime = System.currentTimeMillis();

		// collecting everything to this string for the main output .tex file
		String filestring = "";

		// load all properties
		props.load(new FileInputStream(props_fileName));
		database_url = props.getProperty("database_url", "jdbc:mysql://localhost:3306/bibx?useSSL=false");
		database_username = props.getProperty("database_username", "");
		database_password = props.getProperty("database_password", "");
		output_directory = props.getProperty("output_directory", "bibx_out");
		output_directory_append_date = Boolean.parseBoolean(props.getProperty("output_directory_append_date", "false"));
		report_files_subdirectory = props.getProperty("report_files_subdirectory", "report_files");
		output_filename_prefix = props.getProperty("output_filename_prefix", "bibx_out");
		output_filename_append_date = Boolean.parseBoolean(props.getProperty("output_filename_append_date", "true"));
		command_r = props.getProperty("command_r", "rscript");
		command_tex2pdf = props.getProperty("command_tex2pdf", "pdflatex");
		appendix_visibility = props.getProperty("appendix_visibility", "true");
		table_rows_max = Integer.parseInt(props.getProperty("table_rows_max", "40"));
		network_edgesize_min_coauthorships = Integer.parseInt(props.getProperty("network_edgesize_min_coauthorships", "1"));
		network_edgesize_min_country_ties = Integer.parseInt(props.getProperty("network_edgesize_min_country-ties", "1"));
		network_edgesize_min_country_ties_states = Integer.parseInt(props.getProperty("network_edgesize_min_country-ties-states", "1"));
		network_edgesize_min_country_ties_states_zips = Integer.parseInt(props.getProperty("network_edgesize_min_country-ties-states-zips", "1"));
		network_edgesize_min_institutional_ties = Integer.parseInt(props.getProperty("network_edgesize_min_institutional-ties", "1"));
		network_edgesize_min_categories = Integer.parseInt(props.getProperty("network_edgesize_min_categories", "1"));
		network_edgesize_min_categories_weighted = Integer.parseInt(props.getProperty("network_edgesize_min_categories-weighted", "1"));
		network_edgesize_min_areas = Integer.parseInt(props.getProperty("network_edgesize_min_areas", "1"));
		network_edgesize_min_areas_weighted = Integer.parseInt(props.getProperty("network_edgesize_min_areas-weighted", "1"));
		network_edgesize_min_keywords_author_relatedness = Integer.parseInt(props.getProperty("network_edgesize_min_keywords-author-relatedness", "1"));
		network_edgesize_min_keywords_plus_relatedness = Integer.parseInt(props.getProperty("network_edgesize_min_keywords-plus-relatedness", "1"));
		network_edgesize_min_cocitations = Integer.parseInt(props.getProperty("network_edgesize_min_cocitations", "1"));

		// do initial work with the loaded properties
		if (output_directory.endsWith("/")) {
			output_directory = output_directory.substring(0, output_directory.length()-1);
		}
		if (output_directory_append_date) {
			output_directory += rundate;
		}
		if (!output_filename_append_date) {
			rundate = "";
		}
		new File(output_directory+"/"+report_files_subdirectory).mkdirs();
		File f = new File(output_directory+"/"+output_filename_prefix+rundate+".tex");

		// load the sql property file
		props_sql.load(new FileInputStream(props_sqlFileName));

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

		// initialize some counters
		int affected_row_count = 0;
		int rowcount = 0;
		int resultset_counter = 0;

		// write latex header
		filestring += "\\documentclass[a4paper]{article}\n"
		+"\\usepackage{booktabs,dcolumn,multicol,parskip,graphicx,color,soul,datetime,tocloft,float}\n"
		+"\\usepackage[pdfstartview=FitPage, colorlinks=true, linkcolor=black, citecolor=blue, urlcolor=blue, linktoc=all]{hyperref}\n"
		+"\\newcolumntype{d}[1]{D{,}{.}{#1}}\n"
		+"\\parskip=\\baselineskip\n"
		+"\\parindent=0pt\n"
		+"\\setlength\\belowcaptionskip{.075cm}\n" // otherwise table captions are really close...
		+"\\begin{document}\n\n";

		// appendix visibility
		if (!appendix_visibility.equals("only")) {
		
		// title page
		filestring += "{\\centering\n"
		+"\\hbox{}\n"
		+"\\vspace{6cm}\n"
		+"{\\Large\\scshape Bibliometric Data Exploration\\par}\n"
		+"\\vspace{1cm}\n"
		+"{\\Huge\\bfseries Analysis Report\\par}\n"
		+"\\vspace{1.5cm}\n"
		+"\\setlength{\\parskip}{0pt}\n"
		+"{\\Large\\itshape Article database \\par "+database_url.substring(0, database_url.indexOf('?'))+" \\par}\n"
		+"\\vspace{6cm}\n"
		+"\\setlength{\\parskip}{4pt}\n"
		+"{\\Large{\\itshape Generated by} BibX\\footnote{\\href{http://github.com/mkkln/bibx}{http://github.com/mkkln/bibx}} {\\itshape on:}\\par}\n"
		+"% datetime settings:\n"
		+"\\renewcommand{\\dateseparator}{.}\n"
		+"\\settimeformat{hhmmsstime}\n"
		+"\\ddmmyyyydate\n"
		+"{\\large\\today{ at }\\currenttime\\par}\n"
		+"} % centering\n"
		+"\\thispagestyle{empty}\n"
		+"\\enlargethispage{2.5 \\baselineskip}\n"
		+"\\clearpage\n\n";

		// empty page
		filestring += "\\hbox{}\n"
		+"\\thispagestyle{empty}\n"
		+"\\clearpage\n\n";

		// table of contents
		filestring += "\\pagenumbering{roman}\n"
		+"\\setlength\\cftaftertoctitleskip{5ex}\n" // tocloft-package
		+"\\tableofcontents\n"
		+"\\clearpage\n\n";
		
		// START ANALYSES

		// Summary statistics
		System.out.println("Starting analyses...");
		filestring += "\\pagenumbering{arabic}\n"
		+"\\section{Summary statistics}\n\n"

		+"Database: "+database_url+"\n\n";

		stm = conn.prepareStatement(props_sql.getProperty("article_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int article_count = 0;
		article_count = rsl.getInt(1);
		filestring += "Article count: "+article_count+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("pages_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int pages_count = 0;
		pages_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Total number of article pages: "+pages_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of pages per article: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)pages_count/(double)article_count)+"\n\n";

		stm = conn.prepareStatement(props_sql.getProperty("author_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int author_count = 0;
		author_count = rsl.getInt(1);
		filestring += "Unique author count: "+author_count+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("article_author_relation_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int article_author_relation_count = 0;
		article_author_relation_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Article-author relation count: "+article_author_relation_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of authors per article: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)article_author_relation_count/(double)article_count)+"\\\\\n";
//		    +Math.round(((double)article_author_relation_count/(double)article_count)*100.0)/100.0+"\n\n";
		stm = conn.prepareStatement(props_sql.getProperty("address_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int address_count = 0;
		address_count = rsl.getInt(1);
		filestring += "Unique author address count: "+address_count+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("article_address_relation_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int article_address_relation_count = 0;
		article_address_relation_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Article-address relation count: "+article_address_relation_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of addresses per article: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)article_address_relation_count/(double)article_count)+"\n\n";

		stm = conn.prepareStatement(props_sql.getProperty("source_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int source_count = 0;
		source_count = rsl.getInt(1);
		filestring += "Unique source count: "+source_count+"\n\n";
		stm = conn.prepareStatement(props_sql.getProperty("source_category_count"));
		rsl = stm.executeQuery();
		rsl.first();
		filestring += "Unique source WoS category count: "+rsl.getInt(1)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("source_source_category_relation_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int source_source_category_relation_count = 0;
		source_source_category_relation_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Source-source\\_category relation count: "+source_source_category_relation_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of source WoS categories per source: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)source_source_category_relation_count/(double)source_count)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("source_source_category_relation_count_weighted"));
		rsl = stm.executeQuery();
		rsl.first();
		int source_source_category_relation_count_weighted = 0;
		source_source_category_relation_count_weighted = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Source-source\\_category relation count, weighted by source count: "
			+source_source_category_relation_count_weighted+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("source_area_count"));
		rsl = stm.executeQuery();
		rsl.first();
		filestring += "Unique source research area count: "+rsl.getInt(1)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("source_source_area_relation_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int source_source_area_relation_count = 0;
		source_source_area_relation_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Source-source\\_area relation count: "+source_source_area_relation_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of source research areas per source: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)source_source_area_relation_count/(double)source_count)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("source_source_area_relation_count_weighted"));
		rsl = stm.executeQuery();
		rsl.first();
		int source_source_area_relation_count_weighted = 0;
		source_source_area_relation_count_weighted = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Source-source\\_area relation count, weighted by source count: "
			+source_source_area_relation_count_weighted+"\n\n";

		stm = conn.prepareStatement(props_sql.getProperty("keyword_author_count"));
		rsl = stm.executeQuery();
		rsl.first();
		filestring += "Unique author keyword count: "+rsl.getInt(1)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("article_keyword_author_relation_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int article_keyword_author_relation_count = 0;
		article_keyword_author_relation_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Article-author\\_keyword relation count: "+article_keyword_author_relation_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of author keywords per article: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)article_keyword_author_relation_count/(double)article_count)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("keyword_plus_count"));
		rsl = stm.executeQuery();
		rsl.first();
		filestring += "Unique keyword plus count: "+rsl.getInt(1)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("article_keyword_plus_relation_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int article_keyword_plus_relation_count = 0;
		article_keyword_plus_relation_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Article-keyword\\_plus relation count: "+article_keyword_plus_relation_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of keyword plus per article: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)article_keyword_plus_relation_count/(double)article_count)+"\n\n";

		stm = conn.prepareStatement(props_sql.getProperty("cited_reference_count"));
		rsl = stm.executeQuery();
		rsl.first();
		filestring += "Unique cited reference count: "+rsl.getInt(1)+"\\\\\n";
		stm = conn.prepareStatement(props_sql.getProperty("article_cited_reference_relation_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int article_cited_reference_relation_count = 0;
		article_cited_reference_relation_count = rsl.getInt(1);
		filestring += "\\hspace*{0.5cm} Article-cited\\_reference relation count: "+article_cited_reference_relation_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of cited references per article: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)article_cited_reference_relation_count/(double)article_count)+"\n\n";

		stm = conn.prepareStatement(props_sql.getProperty("times_cited_count"));
		rsl = stm.executeQuery();
		rsl.first();
		int times_cited_count = 0;
		times_cited_count = rsl.getInt(1);
		filestring += "Citations to the articles: "+times_cited_count+"\\\\\n";
		filestring += "\\hspace*{0.5cm} Average number of citations per article: "
		    +String.format(Locale.ENGLISH, "%.2f", (double)times_cited_count/(double)article_count)+"\n\n";

		filestring += "\\clearpage\n\n";

		// ARTICLES
		filestring += "\\section{Articles}\n\n";

		// table for articles by year
		stm = conn.prepareStatement(props_sql.getProperty("articles_by_year"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[H]\n"
		+"\\centering\n";
		if (rowcount < table_rows_max) {
			filestring += "\\caption{Number of articles per year (all years included)}\n";
		} else {
			filestring += "\\caption{Number of articles per year, "+rowcount+" most recent years}\n";
		}
		filestring += "\\begin{tabular}{*{2}{r}d{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{r}{Year}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		rsl.afterLast();
		while (rsl.previous()) {
			filestring += rsl.getShort("year")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n"

		+"\\hbox{}\n\n";

		// year barplot in R
		String bar_filestring = "";
		// csv format saving:
		bar_filestring += "categ,count\n";
		rsl.beforeFirst();
		resultset_counter = 0;
		// include up to 20 latest years
		for (int i=0; i<(rowcount-20); i++){
			rsl.next();
		}
		while (rsl.next() && resultset_counter < 20) {
			resultset_counter++;
			bar_filestring += rsl.getInt("year")+","+rsl.getShort("count")+"\n";
		}
		resultset_counter = 0;
		Files.write(new File(output_directory+"/"+report_files_subdirectory+"/"+output_filename_prefix+"_bar-years"+rundate+".csv").toPath(),
			bar_filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE
		// run R
		rt = Runtime.getRuntime();
		System.out.println("Executing "+command_r+": barplot.r > "+output_filename_prefix+"_bar-years");
		p = rt.exec(command_r+" r/barplot.r"
					+" \""+output_directory+"/"+report_files_subdirectory+"\""
					+" \""+output_filename_prefix+"_bar-years"+rundate+".csv\""
					+" \""+output_filename_prefix+"_bar-years"+rundate+".pdf\""
					+" \"Year\"");
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		filestring += "\\begin{figure}[hb]\n"
		+"\\includegraphics[width=\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_bar-years"+rundate+".pdf}\n";
		if (rowcount <= 20) {
			filestring += "\\caption{Articles per year (all years included)}\n";
		} else {
			filestring += "\\caption{Articles per year (20 most recent years)}\n";
		}
		filestring +="\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// table for articles by type
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{Number of articles per type}\n"
		+"\\begin{tabular}{*{2}{r}d{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{r}{Type}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		stm = conn.prepareStatement(props_sql.getProperty("articles_by_type"));
		rsl = stm.executeQuery();
		while (rsl.next()) {
			filestring += rsl.getString("type")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_count)*100)+"\\\\\n";
		}
		filestring += "Total & "+article_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n"

		+"\\hbox{}\n\n";

		// type pie in R
		String pie_filestring = "";
		// CSV FORMAT SAVING:
		pie_filestring += "type,count\n";
		rsl.beforeFirst();
		while (rsl.next()) {
			pie_filestring += rsl.getString("type")+","+rsl.getShort("count")+"\n";
		}
		Files.write(new File(output_directory+"/"+report_files_subdirectory+"/"+output_filename_prefix+"_pie-type"+rundate+".csv").toPath(),
			pie_filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE
		// run R
		rt = Runtime.getRuntime();
		System.out.println("Executing "+command_r+": pie.r > "+output_filename_prefix+"_pie-type");
		p = rt.exec(command_r+" r/pie.r"
					+" \""+output_directory+"/"+report_files_subdirectory+"\""
					+" \""+output_filename_prefix+"_pie-type"+rundate+".csv\""
					+" \""+output_filename_prefix+"_pie-type"+rundate+".pdf\"");
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		filestring += "\\begin{figure}[hb]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=0.75\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_pie-type"+rundate+".pdf}\n"
		+"}\n" //end makebox special
		+"\\caption{Articles per type}\n"
		+"\\end{figure}\n\n";
		
		filestring += "\\clearpage\n\n";

		// table for most cited articles
		stm = conn.prepareStatement(props_sql.getProperty("most_cited_articles"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most cited articles}\n"
		+"\\footnotesize"
		+"\\rotatebox{90}{"
		+"\\begin{tabular}{p{4cm}rp{9cm}p{3cm}r}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Author(s)} & "
			+"\\multicolumn{1}{r}{Year} & "
			+"\\multicolumn{1}{l}{Title} & "
			+"\\multicolumn{1}{l}{Source} & "
			+"\\multicolumn{1}{r}{Cited} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += "\\rule{0pt}{3ex}"
					   +rsl.getString("authors").replace("[","{[}").replace("]","{]}")+" & "
					   +rsl.getInt("year")+" & "
					   +rsl.getString("title").replace("&","\\&")+" & "
					   +rsl.getString("source").substring(0, 1).toUpperCase()+rsl.getString("source").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("times_cited")+" \\\\\n";
		}
		if (rowcount == 10) {
			filestring += "... & & & & ... \\\\\n";
		}
		filestring += "Total & & & & "+times_cited_count+" \\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"} % rotatebox\n"
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// AUTHORS
		filestring += "\\section{Authors}\n\n";
		
		// table for number of articles per author count
		stm = conn.prepareStatement(props_sql.getProperty("articles_by_author_count"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{Number of articles per author count}\n"
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Authors}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getInt("authors")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & & & ... \\\\\n";
		}
		filestring += "Total & "+article_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n";

		// author count barplot in R
		bar_filestring = "";
		resultset_counter = 0;
		// CSV FORMAT SAVING:
		bar_filestring += "categ,count\n";
		rsl.beforeFirst();
		while (rsl.next() && resultset_counter < 15) {
			resultset_counter++;
			bar_filestring += rsl.getInt("authors")+","+rsl.getShort("count")+"\n";
		}
		resultset_counter = 0;
		Files.write(new File(output_directory+"/"+report_files_subdirectory+"/"+output_filename_prefix+"_bar-author-count"+rundate+".csv").toPath(),
			bar_filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE
		// run R
		rt = Runtime.getRuntime();
		System.out.println("Executing "+command_r+": barplot.r > "+output_filename_prefix+"_bar-author-count");
		p = rt.exec(command_r+" r/barplot.r"
					+" \""+output_directory+"/"+report_files_subdirectory+"\""
					+" \""+output_filename_prefix+"_bar-author-count"+rundate+".csv\""
					+" \""+output_filename_prefix+"_bar-author-count"+rundate+".pdf\""
					+" \"Author count\"");
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		filestring += "\\begin{figure}[H]\n"
		+"\\includegraphics[width=\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_bar-author-count"+rundate+".pdf}\n"
		+"\\caption{Articles per author count}\n"
		+"\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// table for number of authors with specific author order
		stm = conn.prepareStatement(props_sql.getProperty("authors_by_author_order"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{Number of authors with specific author order}\n"
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Order}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		int author_count_second = 1; // avoid division by zero later
		int author_count_third = 1; // avoid division by zero later
		resultset_counter = 0;
		while (rsl.next()) {
			resultset_counter++;
			filestring += rsl.getInt("author_order")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_author_relation_count)*100)+"\\\\\n";
			if (resultset_counter == 2) {
				author_count_second = rsl.getInt("count");
			} else if (resultset_counter == 3) {
				author_count_third = rsl.getInt("count");
			}
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_author_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n";
		resultset_counter = 0;

		filestring += "\\clearpage\n\n";

		// table for most productive authors
		stm = conn.prepareStatement(props_sql.getProperty("most_productive_authors"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" authors with most authorships}\n"
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").replace("[","{[}").replace("]","{]}")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_author_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_author_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple authors} \\\\\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most productive first authors
		stm = conn.prepareStatement(props_sql.getProperty("most_productive_authors_order"));
		stm.setInt(1, 1);
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" authors with most first authorships}\n"
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").replace("[","{[}").replace("]","{]}")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_count)*100)+"\\\\\n";
		}
		if (rowcount == 10) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n";

		// table for most productive second authors
		stm = conn.prepareStatement(props_sql.getProperty("most_productive_authors_order"));
		stm.setInt(1, 2);
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" authors with most second authorships}\n"
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").replace("[","{[}").replace("]","{]}")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)author_count_second)*100)+"\\\\\n";
		}
		if (rowcount == 10) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+author_count_second+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n";

		// table for most productive third authors
		stm = conn.prepareStatement(props_sql.getProperty("most_productive_authors_order"));
		stm.setInt(1, 3);
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[H]\n" // H requires float-package...
		+"\\centering\n"
		+"\\caption{"+rowcount+" authors with most third authorships}\n"
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").replace("[","{[}").replace("]","{]}")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)author_count_third)*100)+"\\\\\n";
		}
		if (rowcount == 10) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+author_count_third+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\end{tabular}\n"
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for coathorship network
		makeNetwork("coauthorships", "article_id", "author_name", output_filename_prefix+"_net-coauthorships", network_edgesize_min_coauthorships);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-coauthorships"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_coauthorships == 1) {
			filestring += "\\caption{Coauthorship networks}\n";
		} else {
			filestring += "\\caption{Coauthorship networks (only coauthorships recurring "+network_edgesize_min_coauthorships+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// ADDRESSES
		filestring += "\\section{Addresses}\n\n";
		
		// table for most mentioned addresses
		stm = conn.prepareStatement(props_sql.getProperty("most_mentioned_addresses"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[H]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most mentioned addresses}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, Math.min(rsl.getString("name").length(), 95)).replace("[","{[}").replace("]","{]}").replace("&","\\&")
			    +" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_address_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_address_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple addresses} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most mentioned countries
		stm = conn.prepareStatement(props_sql.getProperty("most_mentioned_countries"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most mentioned countries in the addresses}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("country_name")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_address_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_address_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple addresses} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most mentioned countries, U.S. states separated
		stm = conn.prepareStatement(props_sql.getProperty("most_mentioned_countries_states"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most mentioned countries in the addresses, U.S. states separated}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("country_name")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_address_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_address_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple addresses} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most mentioned countries, U.S. states and zip codes separated
		stm = conn.prepareStatement(props_sql.getProperty("most_mentioned_countries_states_zips"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most mentioned countries in the addresses, U.S. states and zip codes separated}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("country_name")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_address_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_address_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple addresses} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for country tie network
		makeNetwork("country_ties", "article_id", "country_name", output_filename_prefix+"_net-country-ties", network_edgesize_min_country_ties);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-country-ties"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_country_ties == 1) {
			filestring += "\\caption{Country ties}\n";
		} else {
			filestring += "\\caption{Country ties (only ties recurring "+network_edgesize_min_country_ties+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for country tie network, U.S. states separated
		makeNetwork("country_ties_states", "article_id", "country_name", output_filename_prefix+"_net-country-ties-states", network_edgesize_min_country_ties_states);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-country-ties-states"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_country_ties_states == 1) {
			filestring += "\\caption{Country ties, U.S. states separated}\n";
		} else {
			filestring += "\\caption{Country ties, U.S. states separated (only ties recurring "+network_edgesize_min_country_ties_states+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for country ties network, U.S. states and zip codes separated
		makeNetwork("country_ties_states_zips", "article_id", "country_name", output_filename_prefix+"_net-country-ties-states-zips", network_edgesize_min_country_ties_states_zips);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-country-ties-states-zips"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_country_ties_states_zips == 1) {
			filestring += "\\caption{Country ties, U.S. states and zip codes separated}\n";
		} else {
			filestring += "\\caption{Country ties, U.S. states and zip codes separated (only ties recurring "+network_edgesize_min_country_ties_states_zips+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most mentioned institutions
		stm = conn.prepareStatement(props_sql.getProperty("most_mentioned_institutions"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most mentioned institutions in the addresses}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("institution_name").replace("&","\\&")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_address_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_address_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple addresses} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for institutional tie network
		makeNetwork("institutional_ties", "article_id", "institution_name", output_filename_prefix+"_net-institutional-ties", network_edgesize_min_institutional_ties);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-institutional-ties"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_institutional_ties == 1) {
			filestring += "\\caption{Institutional ties}\n";
		} else {
			filestring += "\\caption{Institutional ties (only ties recurring "+network_edgesize_min_institutional_ties+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// SOURCES
		filestring += "\\section{Sources}\n\n";

		// table for most utilized sources
		stm = conn.prepareStatement(props_sql.getProperty("most_utilized_sources"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[H]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" sources with most articles}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("count")+" & "
					   +String.format("%1$.2f",(rsl.getDouble("count")/(double)article_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize } \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most utilized source WoS categories
		stm = conn.prepareStatement(props_sql.getProperty("most_utilized_source_categories"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" Web of Science categories with most occurrences in unique sources}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("count")+" & "
					   +String.format("%1$.2f",(rsl.getDouble("count")/(double)source_source_category_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+source_source_category_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Sources can list multiple Web of Science categories} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for most utilized source WoS categories
		makeNetwork("source_categories_by_source", "source_name", "source_category_name", output_filename_prefix+"_net-categories", network_edgesize_min_categories);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-categories"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_categories == 1) {
			filestring += "\\caption{Web of Science categories with most occurrences in unique sources}\n";
		} else {
			filestring += "\\caption{Web of Science categories with most occurrences in unique sources (only ties recurring "+network_edgesize_min_categories+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most utilized source WoS categories, weighted by source count
		stm = conn.prepareStatement(props_sql.getProperty("most_utilized_source_categories_weighted"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" Web of Science categories with most occurrences in the sources, weighted by source count}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("count")+" & "
					   +String.format("%1$.2f",(rsl.getDouble("count")/(double)source_source_category_relation_count_weighted)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+source_source_category_relation_count_weighted+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Sources can list multiple Web of Science categories} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for most utilized source WoS categories, weighted by source count
		makeNetwork("source_categories_by_source_weighted", "source_name", "source_category_name", output_filename_prefix+"_net-categories-weighted", network_edgesize_min_categories_weighted);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-categories-weighted"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_categories_weighted == 1) {
			filestring += "\\caption{Web of Science categories with most occurrences in the sources, weighted by source count}\n";
		} else {
			filestring += "\\caption{Web of Science categories with most occurrences in the sources, weighted by source count (only ties recurring "+network_edgesize_min_categories_weighted+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most utilized source research areas
		stm = conn.prepareStatement(props_sql.getProperty("most_utilized_source_areas"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" research areas with most occurrences in unique sources}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("count")+" & "
					   +String.format("%1$.2f",(rsl.getDouble("count")/(double)source_source_area_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+source_source_area_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Sources can list multiple research areas} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for most utilized source research areas
		makeNetwork("source_areas_by_source", "source_name", "source_area_name", output_filename_prefix+"_net-areas", network_edgesize_min_areas);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-areas"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_areas == 1) {
			filestring += "\\caption{Research areas with most occurrences in unique sources}\n";
		} else {
			filestring += "\\caption{Research areas with most occurrences in unique sources (only ties recurring "+network_edgesize_min_areas+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most utilized source research areas, weighted by source count
		stm = conn.prepareStatement(props_sql.getProperty("most_utilized_source_areas_weighted"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" research areas with most occurrences in the sources, weighted by source count}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("count")+" & "
					   +String.format("%1$.2f",(rsl.getDouble("count")/(double)source_source_area_relation_count_weighted)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+source_source_area_relation_count_weighted+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Sources can list multiple research areas} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for most utilized source research areas, weighted by source count
		makeNetwork("source_areas_by_source_weighted", "source_name", "source_area_name", output_filename_prefix+"_net-areas-weighted", network_edgesize_min_areas_weighted);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-areas-weighted"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_areas_weighted == 1) {
			filestring += "\\caption{Research areas with most occurrences in the sources, weighted by source count}\n";
		} else {
			filestring += "\\caption{Research areas with most occurrences in the sources, weighted by source count (only ties recurring "+network_edgesize_min_areas_weighted+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// KEYWORDS
		filestring += "\\section{Keywords}\n\n";

		// table for most mentioned author keywords
		stm = conn.prepareStatement(props_sql.getProperty("most_mentioned_keywords_author"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[H]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most mentioned author keywords}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("count")+" & "
					   +String.format("%1$.2f",(rsl.getDouble("count")/(double)article_keyword_author_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_keyword_author_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple author keywords} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for author keyword relatedness
		makeNetwork("keywords_author_relatedness", "article_id", "keyword_author_name", output_filename_prefix+"_net-keywords-author-relatedness", network_edgesize_min_keywords_author_relatedness);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-keywords-author-relatedness"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_keywords_author_relatedness == 1) {
			filestring += "\\caption{Author keywords' relatedness}\n";
		} else {
			filestring += "\\caption{Author keywords' relatedness (only keywords co-occurring "+network_edgesize_min_keywords_author_relatedness+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most mentioned keywords plus
		stm = conn.prepareStatement(props_sql.getProperty("most_mentioned_keywords_plus"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most mentioned keywords plus}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+" & "
					   +rsl.getInt("count")+" & "
					   +String.format("%1$.2f",(rsl.getDouble("count")/(double)article_keyword_plus_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_keyword_plus_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Articles can list multiple keywords plus} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// figure for keywords plus relatedness
		makeNetwork("keywords_plus_relatedness", "article_id", "keyword_plus_name", output_filename_prefix+"_net-keywords-plus-relatedness", network_edgesize_min_keywords_plus_relatedness);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-keywords-plus-relatedness"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_keywords_plus_relatedness == 1) {
			filestring += "\\caption{Keywords plus' relatedness}\n";
		} else {
			filestring += "\\caption{Keywords plus' relatedness (only keywords co-occurring "+network_edgesize_min_keywords_plus_relatedness+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";

		// CITED REFERENCES
		filestring += "\\section{Cited references}\n\n";

		// table for most cited references
		stm = conn.prepareStatement(props_sql.getProperty("most_cited_references"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[H]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most cited references}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			if (rsl.getString("name").startsWith(", DOI ", rsl.getString("name").lastIndexOf(','))) {
				filestring += rsl.getString("name").substring(0, rsl.getString("name").lastIndexOf(',')).replace("&","\\&");
			} else {
				filestring += rsl.getString("name").replace("&","\\&");
			}
			filestring +=
			    " & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_cited_reference_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_cited_reference_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize } \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most cited years
		stm = conn.prepareStatement(props_sql.getProperty("most_cited_years"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most recent years cited}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Year}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getShort("year")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_cited_reference_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_cited_reference_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{p{3.75cm}}{\\footnotesize Note: Some references do not\\newline contain year information} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

		// table for most cited authors
		stm = conn.prepareStatement(props_sql.getProperty("most_cited_authors"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			rowcount = rsl.getRow();
			rsl.beforeFirst();
		}
		filestring += "\\begin{table}[htbp]\n"
		+"\\centering\n"
		+"\\caption{"+rowcount+" most cited authors}\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide tables to break margins
		+"\\begin{tabular}{lrd{2}}\n"
		+"\\toprule\n"
		+"\\multicolumn{1}{l}{Name}&\\multicolumn{1}{r}{Count}&\\multicolumn{1}{r}{\\%} \\\\\n"
		+"\\midrule\n";
		while (rsl.next()) {
			filestring += rsl.getString("author_name").replace("[","{[}").replace("]","{]}")+" & "+rsl.getInt("count")+" & "
				+String.format("%1$.2f",(rsl.getDouble("count")/(double)article_cited_reference_relation_count)*100)+"\\\\\n";
		}
		if (rowcount == table_rows_max) {
			filestring += "... & & \\\\\n";
		}
		filestring += "Total & "+article_cited_reference_relation_count+" & 100,00\\\\\n"
		+"\\bottomrule\n"
		+"\\multicolumn{3}{l}{\\footnotesize Note: Some references do not contain author information} \\\\\n"
		+"\\end{tabular}\n"
		+"}\n" //end makebox special
		+"\\end{table}\n\n";

		filestring += "\\clearpage\n\n";

// TODO: Need to handle if cocitation network too large...?
		// figure for cocitation network
		makeNetwork("cocitations", "article_id", "cited_reference_name", output_filename_prefix+"_net-cocitations", network_edgesize_min_cocitations);
		filestring += "\\begin{figure}[p]\n"
		+"\\makebox[\\textwidth][c]{\n" //special for wide figures to break margins
		+"\\includegraphics[width=1.5\\textwidth]{"+report_files_subdirectory+"/"+output_filename_prefix+"_net-cocitations"+rundate+".pdf}\n"
		+"}\n"; //end makebox special
		if (network_edgesize_min_cocitations == 1) {
			filestring += "\\caption{Cocitation network}\n";
		} else {
			filestring += "\\caption{Cocitation network (only cocitations recurring "+network_edgesize_min_cocitations+" times or more shown)}\n";
		}
		filestring += "\\end{figure}\n\n";

		filestring += "\\clearpage\n\n";


		// appendix visibility
		}
		if (appendix_visibility.equals("only") || appendix_visibility.equals("true")) {

		// START APPENDICES
		String appendix_helpstring = "";
		
		// suppress section numbering for appendices
		filestring += "\\setcounter{secnumdepth}{0}\n";

		// add some space and heading to toc
        filestring += "\\addtocontents{toc}{\\vspace{\\normalbaselineskip} \\vspace{\\normalbaselineskip} \\bfseries Appendices\\par}\n\n";

		// add appendices title page
		filestring += "{\\centering\n"
		+"\\vspace*{\\fill}\n"
		+"{\\huge \\bfseries Appendices\\par}\n"
		+"\\rule{0pt}{10ex}\n"
		+"\\vspace*{\\fill}\n"
		+"} % centering\n"
		+"\\thispagestyle{empty}\n"
		+"\\clearpage\n\n";

		// Appendix: list all authors for error inspection
		filestring += "\\begin{multicols*}{3}\n"
		+"[\n"
		+"\\section{Appendix: All authors}\n"
		+"List all authors for WoS data inconsistency inspection, suspects highlighted.\n"
		+"]\n"
		+"\\begin{footnotesize}\n";
		stm = conn.prepareStatement(props_sql.getProperty("all_authors"));
		rsl = stm.executeQuery();
		appendix_helpstring = "";
		while (rsl.next()) {
			if (rsl.isFirst()) {
				appendix_helpstring = rsl.getString("name").replace("[","{[}").replace("]","{]}");
				filestring += appendix_helpstring;
			} else {
				if (rsl.getString("name").replace("[","{[}").replace("]","{]}").startsWith(appendix_helpstring)) {
					filestring += " \\\\ \\hl{"+rsl.getString("name").replace("[","{[}").replace("]","{]}")+"}";
				} else {
					appendix_helpstring = rsl.getString("name").replace("[","{[}").replace("]","{]}");
					filestring += " \\\\ "+appendix_helpstring;
				}
			}
		}
		appendix_helpstring = "";
		filestring += "\n\\end{footnotesize}\n"
		+"\\end{multicols*}\n\n";

		filestring += "\\clearpage\n\n";

		// Appendix: list all parsed institutions for error inspection
		filestring += "\\begin{multicols*}{2}\n"
		+"[\n"
		+"\\section{Appendix: All parsed institutions}\n"
		+"List all parsed institutions for WoS data inconsistency inspection, suspects highlighted.\n"
		+"]\n"
		+"\\begin{footnotesize}\n";
		stm = conn.prepareStatement(props_sql.getProperty("all_addresses"));
		rsl = stm.executeQuery();
		TreeSet<String> unique_parsed_institutions = new TreeSet<>();
		while (rsl.next()) {
//			System.out.println("\n"+rsl.getString("name"));
//			System.out.println(rsl.getString("name").indexOf("] ")+2);
//			System.out.println(rsl.getString("name").indexOf(','));
//			System.out.println(rsl.getString("name").substring(rsl.getString("name").indexOf("] ")+2).substring(0, rsl.getString("name").substring(rsl.getString("name").indexOf("] ")+2).indexOf(',')));
			if (rsl.getString("name").indexOf("] ") != -1) {
				unique_parsed_institutions.add(
					rsl.getString("name").substring(rsl.getString("name").indexOf("] ")+2).substring(0, rsl.getString("name").substring(rsl.getString("name").indexOf("] ")+2).indexOf(',')));
			} else {
				unique_parsed_institutions.add(
					rsl.getString("name").substring(0, rsl.getString("name").indexOf(',')));
			}
		}
		appendix_helpstring = "98p39p8q3vkj";
		for (String institution : unique_parsed_institutions) {
			if (institution.replace("&", "\\&").startsWith(appendix_helpstring)) {
				filestring += "\\hl{"+institution.replace("&", "\\&")+"} \\\\ ";
			} else {
				appendix_helpstring = institution.replace("&", "\\&");
				filestring += appendix_helpstring+" \\\\ ";
			}
		}
		filestring.substring(0, filestring.length()-4);
		appendix_helpstring = "";
		filestring += "\n\\end{footnotesize}\n\n"
		+"\\end{multicols*}\n\n";

		filestring += "\\clearpage\n\n";

		// Appendix: list all parsed countries for error inspection
		filestring += "\\begin{multicols*}{3}\n"
		+"[\n"
		+"\\section{Appendix: All parsed countries}\n"
		+"List all parsed countries for WoS data inconsistency inspection.\n"
		+"]\n"
		+"\\begin{footnotesize}\n";
		rsl.beforeFirst();
		TreeSet<String> unique_parsed_countries = new TreeSet<>();
		while (rsl.next()) {
			unique_parsed_countries.add(
				rsl.getString("name").substring(rsl.getString("name").lastIndexOf(',')+2, rsl.getString("name").length()-1));
		}
		for (String country : unique_parsed_countries) {
			filestring += country.replace("&", "\\&")+" \\\\ ";
		}
		filestring.substring(0, filestring.length()-4);
		filestring += "\n\\end{footnotesize}\n\n"
		+"\\end{multicols*}\n\n";

		filestring += "\\clearpage\n\n";

		// Appendix: list all sources
		filestring += "\\section{Appendix: All sources}\n"
		+"\\begin{footnotesize}\n";
		stm = conn.prepareStatement(props_sql.getProperty("all_sources"));
		rsl = stm.executeQuery();
		while (rsl.next()) {
			if (!rsl.isFirst()) {
				filestring += " \\\\ ";
			}
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&");
		}
		filestring += "\n\\end{footnotesize}\n\n";

		filestring += "\\clearpage\n\n";

		// Appendix: list all source categories
		filestring += "\\section{Appendix: All source WoS categories}\n"
		+"\\begin{footnotesize}\n";
		stm = conn.prepareStatement(props_sql.getProperty("all_source_categories"));
		rsl = stm.executeQuery();
		while (rsl.next()) {
			if (!rsl.isFirst()) {
				filestring += " \\\\ ";
			}
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&");
		}
		filestring += "\n\\end{footnotesize}\n\n";

		filestring += "\\clearpage\n\n";

		// Appendix: list all source research areas
		filestring += "\\section{Appendix: All source research areas}\n"
		+"\\begin{footnotesize}\n";
		stm = conn.prepareStatement(props_sql.getProperty("all_source_areas"));
		rsl = stm.executeQuery();
		while (rsl.next()) {
			if (!rsl.isFirst()) {
				filestring += " \\\\ ";
			}
			filestring += rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&");
		}
		filestring += "\n\\end{footnotesize}\n\n";

		filestring += "\\clearpage\n\n";

		// Appendix: list all author keywords for error inspection
		filestring += "\\begin{multicols*}{2}\n"
		+"[\n"
		+"\\section{Appendix: All author keywords}\n"
		+"List all author keywords for WoS data inconsistency inspection, suspects highlighted.\n"
		+"]\n"
		+"\\begin{footnotesize}\n";
		stm = conn.prepareStatement(props_sql.getProperty("all_keywords_author"));
		rsl = stm.executeQuery();
		appendix_helpstring = "";
		while (rsl.next()) {
			if (rsl.isFirst()) {
				appendix_helpstring = rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&");
				filestring += appendix_helpstring;
			} else {
				if ((rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")).startsWith(appendix_helpstring)) {
					filestring += " \\\\ \\hl{"+rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+"}";
				} else {
					appendix_helpstring = rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&");
					filestring += " \\\\ "+appendix_helpstring;
				}
			}
		}
		appendix_helpstring = "";
		filestring += "\n\\end{footnotesize}\n\n"
		+"\\end{multicols*}\n\n";

		filestring += "\\clearpage\n\n";

		// Appendix: list all keywords plus for error inspection
		filestring += "\\begin{multicols*}{2}\n"
		+"[\n"
		+"\\section{Appendix: All keywords plus}\n"
		+"List all keywords plus for WoS data inconsistency inspection, suspects highlighted.\n"
		+"]\n"
		+"\\begin{footnotesize}\n";
		stm = conn.prepareStatement(props_sql.getProperty("all_keywords_plus"));
		rsl = stm.executeQuery();
		appendix_helpstring = "";
		while (rsl.next()) {
			if (rsl.isFirst()) {
				appendix_helpstring = rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&");
				filestring += appendix_helpstring;
			} else {
				if ((rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")).startsWith(appendix_helpstring)) {
					filestring += " \\\\ \\hl{"+rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&")+"}";
				} else {
					appendix_helpstring = rsl.getString("name").substring(0, 1).toUpperCase()+rsl.getString("name").substring(1).toLowerCase().replace("&", "\\&");
					filestring += " \\\\ "+appendix_helpstring;
				}
			}
		}
		appendix_helpstring = "";
		filestring += "\n\\end{footnotesize}\n\n"
		+"\\end{multicols*}\n\n";
		// END APPENDICES

		} // appendix visibility

		// END ANALYSES
		System.out.println("...analyses done.");

		// write latex footer
		filestring += "\\end{document}\n";

		// write file
		Files.write(f.toPath(), filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE
		System.out.println("Wrote file "+f.toPath()+".");

		// typeset tex to pdf
		rt = Runtime.getRuntime();
//		System.out.println("pdflatex "+f.toPath()+" -aux-directory="+output_directory+" -output-directory="+output_directory);
		for (int i=1; i<=2; i++) {
			System.out.println("Executing "+command_tex2pdf+": Pass "+i+"/2.");
			p = rt.exec(command_tex2pdf+" "+f.toPath()
						+" -aux-directory="+output_directory
						+" -output-directory="+output_directory
						+" -enable-installer"
						+" -quiet");
			// below snippet from http://stackoverflow.com/questions/7960369/using-runtime-getruntime-exec-in-eclipse/7962434#7962434
			final InputStream pOut = p.getInputStream();
			Thread outputDrainer = new Thread() {
				public void run() {
					try {
						int c;
						do {
							c = pOut.read();
							if (c >= 0) {
								System.out.print((char)c);
							}
						} while (c >= 0);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			outputDrainer.start();
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} // for

//		conn.commit();
		if (stm != null) {
			stm.close();
		}
		conn.close();
		System.out.println("Analyzed the database and generated a pdf report in "+(System.currentTimeMillis()-startTime)/1000F+" secs.");
		System.out.println("Database connection closed.\n");

		System.out.println("BibX finished at: "+rundateformat.format(new java.util.Date())+".");
/*
		} catch (SQLException e) {
			throw new IllegalStateException("Cannot connect to the database!", e);
		} // 'finally' not needed here as the above try is Java 7 try-with-resources
*/
	} // main


	public static void makeNetwork(String sqlquery, String category, String property, String output_filename, int edgesize_min)
		throws IOException, FileNotFoundException, SQLException {

		Map<String, Integer> property_count = new TreeMap<String, Integer>();
		Map<String, Integer> property_tie_count = new TreeMap<String, Integer>();
		List<String> properties = new ArrayList<String>();
		String category_name = "";
		String category_temp = "";
		String property_name = "";
		String key = "";
		int value = 0;
		boolean waslastrow = false;
		
		stm = conn.prepareStatement(props_sql.getProperty(sqlquery));
		rsl = stm.executeQuery();
		if (rsl.first()) {
			category_temp = rsl.getString(category);
			property_name = rsl.getString(property);
			while (true) {
				if (!category_name.equals(category_temp) || properties.contains(property_name)) {
					// if there were two or more of the properties, there was a tie:
					if (properties.size() >= 2) {
						// make ties between all found properties:
						for (int i=0; i<properties.size(); i++) {
							for (int j=i+1; j<properties.size(); j++) {
								key = properties.get(i)+tieSeparator+properties.get(j);
								// add ties to tie-map:
								if (property_tie_count.containsKey(key)) {
									property_tie_count.put(key, property_tie_count.get(key)+1);
								} else {
									property_tie_count.put(key, 1);
								}
							}
						}
					}
					// add individual property occurrence count to count-map
					for (String prop : properties) {
						if (property_count.containsKey(prop)) {
							property_count.put(prop, property_count.get(prop)+1);
						} else {
							property_count.put(prop, 1);
						}
					}
					properties.clear();
				}

				if (!waslastrow) {
					properties.add(property_name);
					category_name = rsl.getString(category);
					if (!rsl.next()) {
						waslastrow = true;
						category_temp = "";
					} else {
						category_temp = rsl.getString(category);
						property_name = rsl.getString(property);
					}
				} else {
					break;
				}
			}; // while
		}
		// parsing sql done, now put everything to file
		String network_filestring = "";

		// CSV FORMAT SAVING:
		network_filestring += "id,label,size\n";
		for (Map.Entry<String, Integer> entry : property_count.entrySet()) {
			if (entry.getValue() >= edgesize_min) {
				network_filestring += entry.getKey().replace(",","")+","
					// capitalize first letter, lowercase rest?
//					+entry.getKey().substring(0, 1).toUpperCase()+entry.getKey().substring(1).toLowerCase().replace(",","")+","
					+entry.getKey().replace(",","")+","
					// scale the results in R, not in Java...
					+entry.getValue()+"\n";
			}
		}

		Files.write(new File(output_directory+"/"+report_files_subdirectory+"/"+output_filename+"-verts"+rundate+".csv").toPath(),
			network_filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE

		network_filestring = "from,to,width\n";
		for (Map.Entry<String, Integer> entry : property_tie_count.entrySet()) {
			if (entry.getValue() >= edgesize_min) {
				network_filestring += entry.getKey().substring(0, entry.getKey().indexOf(tieSeparator)).replace(",","")+","
					+entry.getKey().substring(entry.getKey().indexOf(tieSeparator)+tieSeparator.length(), entry.getKey().length()).replace(",","")+","
					// scale the results in R, not in Java...
					+entry.getValue()+"\n";
			}
		}

		Files.write(new File(output_directory+"/"+report_files_subdirectory+"/"+output_filename+"-edges"+rundate+".csv").toPath(),
			network_filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE

		// run R
		rt = Runtime.getRuntime();
		System.out.println("Executing "+command_r+": network.r (edgesize>="+edgesize_min+") > "+output_filename);
		p = rt.exec(command_r+" r/network.r "
					+"\""+output_directory+"/"+report_files_subdirectory+"\" "
					+"\""+output_filename+"-verts"+rundate+".csv\" "
					+"\""+output_filename+"-edges"+rundate+".csv\" "
					+"\""+output_filename+rundate+".pdf\" "
					+edgesize_min);
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	} // makeNetwork


} // class
