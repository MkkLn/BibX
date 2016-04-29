import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

// apache poi
import org.apache.poi.xwpf.usermodel.*;

class bibx_data_out {

	// used in property loading
	private static final String props_fileName = "properties/bibx_data_out_properties.txt";
	private static Properties props = new Properties();
	private static String database_url = "";
	private static String database_username = "";
	private static String database_password = "";
	private static String output_directory = "";
	private static boolean output_directory_append_date = false;
	private static String output_filename_prefix = "";
	private static boolean output_filename_append_date = true;
	private static String command_tex2pdf = "";
	private static String output_file_type = "";
	private static String highlight_pdf = "";
	// used in sql property loading
	private static final String props_sqlFileName = "properties/bibx_data_out_query_properties.sql";
	private static Properties props_sql = new Properties();

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

		System.out.println("BibX started at: "+rundateformat.format(date)+".\n");
		long startTime = System.currentTimeMillis();

		// collecting everything to this string for the output .txt and .tex files
		String filestring = "";

		// load all properties
		props.load(new FileInputStream(props_fileName));
		database_url = props.getProperty("database_url", "jdbc:mysql://localhost:3306/bibx?useSSL=false");
		database_username = props.getProperty("database_username", "");
		database_password = props.getProperty("database_password", "");
		output_directory = props.getProperty("output_directory", "bibx_out");
		output_directory_append_date = Boolean.parseBoolean(props.getProperty("output_directory_append_date", "false"));
		output_filename_prefix = props.getProperty("output_filename_prefix", "bibx_out");
		output_filename_append_date = Boolean.parseBoolean(props.getProperty("output_filename_append_date", "true"));
		command_tex2pdf = props.getProperty("command_tex2pdf", "pdflatex");
		output_file_type = props.getProperty("output_file_type", "all");
		highlight_pdf = props.getProperty("highlight_pdf", "");

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
		new File(output_directory).mkdirs();
		File f = new File(output_directory+"/"+output_filename_prefix+"_data"+rundate+".txt");

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
        int article_count = 0;
        int article_count_max = 0;

		// START ARTICLE EXTRACTION

		stm = conn.prepareStatement(props_sql.getProperty("articles"));
		rsl = stm.executeQuery();
		if (rsl.last()) {
			article_count_max = rsl.getRow();
			rsl.beforeFirst();
		}
		
		// WRITE TEXT FILE?

		if (output_file_type.equals("text") || output_file_type.equals("all")){
			while (rsl.next()) {
				article_count++;
				filestring +=
					"NB\t"+article_count+"/"+article_count_max+"\n"+
					"AI\t"+rsl.getString("article_id")+"\n"+
					"AU\t"+rsl.getString("authors")+"\n"+
					"PY\t"+rsl.getString("year")+"\n"+
					"TI\t"+rsl.getString("title")+"\n"+
					"SO\t"+rsl.getString("source")+"\n"+
					"DT\t"+rsl.getString("type")+"\n"+
					"PG\t"+rsl.getString("pages")+"\n"+
					"NR\t"+rsl.getString("number_of_references")+"\n"+
					"TC\t"+rsl.getString("times_cited")+"\n"+
					"DE\t"+rsl.getString("keyword_author")+"\n"+
					"ID\t"+rsl.getString("keyword_plus")+"\n"+
					"AB\t"+rsl.getString("abstract")+"\n";
			}

			// write file
			Files.write(f.toPath(), filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE
			System.out.println("Wrote file "+f.toPath()+".");
		}
		
		// WRITE WORD FILE?

		if (output_file_type.equals("word") || output_file_type.equals("all")) {
			XWPFDocument xwpfdoc = new XWPFDocument();
			XWPFParagraph xwpfpara = null;
			XWPFRun xwpfrun = null;
			rsl.beforeFirst();
			article_count = 0;

			while (rsl.next()) {
				article_count++;
				xwpfpara = xwpfdoc.createParagraph();
//				xwpfpara.setIndentationLeft(500);
//				xwpfpara.setIndentationHanging(500);
				xwpfpara.setPageBreak(true);

				xwpfrun = xwpfpara.createRun();
				xwpfrun.setFontFamily("Times New Roman");
				xwpfrun.setFontSize(10);
				xwpfrun.setText("NB  ");
				xwpfrun.setText(article_count+"/"+article_count_max);
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("AI  ");
				xwpfrun.setText(rsl.getString("article_id"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("AU  ");
				xwpfrun.setText(rsl.getString("authors"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("PY  ");
				xwpfrun.setText(rsl.getString("year"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("TI  ");
				xwpfrun.setText(rsl.getString("title"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("SO  ");
				xwpfrun.setText(rsl.getString("source"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("DT  ");
				xwpfrun.setText(rsl.getString("type"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("PG  ");
				xwpfrun.setText(rsl.getString("pages"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("NR  ");
				xwpfrun.setText(rsl.getString("number_of_references"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("TC  ");
				xwpfrun.setText(rsl.getString("times_cited"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("DE  ");
				xwpfrun.setText(rsl.getString("keyword_author"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("ID  ");
				xwpfrun.setText(rsl.getString("keyword_plus"));
				xwpfrun.addCarriageReturn();
				xwpfrun.setText("AB  ");
				xwpfrun.setText(rsl.getString("abstract"));
				xwpfrun.addCarriageReturn();
			}

			f = new File(output_directory+"/"+output_filename_prefix+"_data"+rundate+".docx");
			FileOutputStream out = new FileOutputStream(f.toPath().toString());
			xwpfdoc.write(out);
			out.close();
			System.out.println("Wrote file "+f.toPath()+".");
		}

		// WRITE TEX FILE?

		if (output_file_type.equals("tex") || output_file_type.equals("all")){
			rsl.beforeFirst();
			article_count = 0;

			filestring = "\\documentclass[a4paper]{article}\n"
			+"\\usepackage{parskip,color,soul,tabto}\n"
			+"\\usepackage[pdfstartview=FitPage, colorlinks=true, linkcolor=black, citecolor=blue, urlcolor=blue, linktoc=all]{hyperref}\n"
			+"\\parskip=0pt\n"
			+"\\parindent -0.7cm\n"
			+"\\rightskip -0.7cm\n"
			+"\\begin{document}\n\n";
/*
			while (rsl.next()) {
				article_count++;
				filestring +=
					"\\vspace*{-2cm}\n"+
					"NB "+article_count+"/"+article_count_max+"\\par\n"+
					"AI "+rsl.getString("article_id")+"\\par\n"+
					"AU "+rsl.getString("authors")+"\\par\n"+
					"PY "+rsl.getString("year")+"\\par\n"+
					"TI "+rsl.getString("title")+"\\par\n"+
					"SO "+rsl.getString("source")+"\\par\n"+
					"DT "+rsl.getString("type")+"\\par\n"+
					"PG "+rsl.getString("pages")+"\\par\n"+
					"NR "+rsl.getString("number_of_references")+"\\par\n"+
					"TC "+rsl.getString("times_cited")+"\\par\n"+
					"DE "+rsl.getString("keyword_author")+"\\par\n"+
					"ID "+rsl.getString("keyword_plus")+"\\par\n"+
					"AB "+rsl.getString("abstract")+"\\par\n"+
					"\\clearpage\n\n";
			}
*/
			while (rsl.next()) {
				article_count++;
				filestring +=
					"\\vspace*{-2cm}\n"+
					"Nb \\tabto{0cm}"+article_count+"/"+article_count_max+" "+
					"(article\\_id: "+rsl.getString("article_id")+")\\par\n"+
					"TI \\tabto{0cm}"+rsl.getString("title")+"\\par\n"+
					"AU \\tabto{0cm}"+rsl.getString("authors")+"\\par\n"+
					"PY \\tabto{0cm}"+rsl.getString("year")+", "+
					"SO "+rsl.getString("source")+"\\par\n"+
					"DT \\tabto{0cm}"+rsl.getString("type")+"\\par\n"+
					"PG \\tabto{0cm}"+rsl.getString("pages")+", "+
					"NR "+rsl.getString("number_of_references")+", "+
					"TC "+rsl.getString("times_cited")+"\\par\n"+
					"DE \\tabto{0cm}"+rsl.getString("keyword_author")+"\\par\n"+
					"ID \\tabto{0cm}"+rsl.getString("keyword_plus")+"\\par\n"+
					"AB \\tabto{0cm}"+rsl.getString("abstract")+"\\par\n"+
					"\\clearpage\n\n";
			}

			// write latex footer
			filestring += "\\end{document}\n";

			// take care of special characters
			filestring = filestring.replace("&", "\\&").replace("%", "\\%").replace("$", "\\$");

			// highlight something in the pdf?
			if (!highlight_pdf.equals("")) {
				List<String> highlight_list = new ArrayList<String>();
				highlight_list = Arrays.asList(highlight_pdf.split(","));
				for (String highlight_pattern : highlight_list) {
					filestring = filestring.replaceAll("(?i)("+highlight_pattern+")", "\\\\hl\\{$1\\}");
				}
			}

			// write file
			f = new File(output_directory+"/"+output_filename_prefix+"_data"+rundate+".tex");
			Files.write(f.toPath(), filestring.getBytes()); // default StandardOpenOption sequence: CREATE,TRUNCATE_EXISTING,WRITE
			System.out.println("Wrote file "+f.toPath()+".");

			// typeset tex to pdf
			rt = Runtime.getRuntime();
			System.out.println("Executing "+command_tex2pdf+".");
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
		}

//		conn.commit();
		if (stm != null) {
			stm.close();
		}
		conn.close();
		System.out.println("Exported "+article_count+" articles from the database in "+(System.currentTimeMillis()-startTime)/1000F+" secs.");
		System.out.println("Database connection closed.\n");

		System.out.println("BibX finished at: "+rundateformat.format(new java.util.Date())+".");

	} // main

} // class
