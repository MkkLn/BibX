import java.util.*;

// used in bibx_data_in

public class bibx_article {
	
	public int article_id = 0;
	public String type = "";
	public String title = "";
	public String source = "";
	public int source_id = 0;
	public List<String> keywords_author = new ArrayList<String>();
	public List<String> keywords_plus = new ArrayList<String>();
	public List<String> cited_references = new ArrayList<String>();
	public List<String> addresses = new ArrayList<String>(Arrays.asList("NONE"));
	public String abs = "";
	public List<String> authors = new ArrayList<String>();
	public short number_of_references = 0;
	public short times_cited = 0;
	public short year = 0;
	public String volume = "";
	public String issue = "";
	public String page_begin = "";
	public String page_end = "";
	public short pages = 0;
	public List<String> source_categories = new ArrayList<String>();
	public List<String> source_areas = new ArrayList<String>();
/*
	public article() {
	}
*/
	@Override
	public String toString() {
		return "";
	}
}
