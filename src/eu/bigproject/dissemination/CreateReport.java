package eu.bigproject.dissemination;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.benfante.jslideshare.SlideShareAPI;
import com.benfante.jslideshare.SlideShareAPIFactory;
import com.benfante.jslideshare.messages.Slideshow;
import com.benfante.jslideshare.messages.User;

//@WebServlet(name="createreport",value="/createreport")
public class CreateReport extends HttpServlet
{
	private static final int	MINIMAL_RECALCULATE_INTERVAL_SECONDS	= 60;
	private static final String BIBSONOMY_URL	= "http://www.bibsonomy.org/user/bigfp7";
	private static final String TWITTER_URL	= "https://twitter.com/BIG_FP7";
	private static final String	MAIN_MAILINGLIST_URL	= "http://lists.atosresearch.eu/mailman/private/bigdata/";
	private static final String	BLOG_URL	= "http://big-project.eu/blog";
	private static final String	INTERVIEW_URL	= "http://big-project.eu/text-interviews";

	private static final String MAIN_MAILINGLIST_EMAIL = FollowAdder.properties.getProperty("mailinglist.email");
	private static final String MAIN_MAILINGLIST_PASSWORD = FollowAdder.properties.getProperty("mailinglist.password");

	private static final String OTHER_MAILINGLISTS_PREFIX = "http://lists.atosresearch.eu/mailman/private/";
	private static final String OTHER_MAILINGLISTS_ROSTER_PREFIX = "http://lists.atosresearch.eu/mailman/admin/";
	private static final String OTHER_MAILINGLISTS_ROSTER_SUFFIX = "/members/list";

	private static final String[] OTHER_MAILINGLISTS = {"telcomedia-sf","health-sf","publicsector-sf","financialservices-sf","manuretailenertrans-sf","datacuration-wg","datastorage-wg","dataanalysis-wg","datausage-wg"};
	private static final String MAILINGLIST_ADMIN_USERNAME = FollowAdder.properties.getProperty("mailinglist.admin.username");
	private static final String MAILINGLIST_ADMIN_PASSWORD = FollowAdder.properties.getProperty("mailinglist.admin.password");

	private static final String SLIDESHARE_KEY = FollowAdder.properties.getProperty("slideshare.key");
	private static final String SLIDESHARE_SECRET = FollowAdder.properties.getProperty("slideshare.secret");

	static final String MESSAGE_COUNT = "messagecount";
	static final String BLOG_POSTS = "blogposts";

	static final String SLIDESHARES_EXTERNAL = "slidesharesexternal";
	static final String SLIDESHARES_INTERNAL = "internalslidesharesinternal";
	static final String SLIDESHARES_TOTAL = "slidesharestotal";
	static final String SLIDESHARES_PRESENTATION_VIEWS = "presentationviews";
	static final String SLIDESHARES_INTERVIEW_VIEWS = "interviewviews";

	static final String TWITTER_TWEETS = "tweets";
	static final String TWITTER_FOLLOWING = "following";
	static final String TWITTER_FOLLOWERS = "followers";

	static final String INTERVIEWS = "interviews";
	static final String PUBLICATIONS = "publications";

	static final StringBuffer htmlLastFullOutput = new StringBuffer();

	static Map<String,Integer> metrics = new HashMap<>();

	static final File metricsFile = new File(System.getProperty("java.io.tmpdir"),"metrics");

	static void logMetric(String m,int v)
	{
		println(m+'\t'+v);
		metrics.put(m, v);
	}

	public static String loadPostContent(String url,String usernameParam, String username,String passwordParam, String password) throws MalformedURLException, IOException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write(usernameParam+"="+username+"&"+passwordParam+"="+password);
		writer.close();

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			try(Scanner in = new Scanner(connection.getInputStream(), "UTF-8"))
			{
				return in.useDelimiter("\\A").next();
			}
		}
		else {throw new RuntimeException(connection.getResponseMessage());}
	}

	public static String loadPostContentUser(String url,String username,String password) throws MalformedURLException, IOException
	{
		return loadPostContent(url, "username", username, "password", password);
	}

	public static String loadPostContentAdmin(String url,String username,String password) throws MalformedURLException, IOException
	{
		return loadPostContent(url, "notused", username, "adminpw", password);
	}

	static StringBuffer sb = new StringBuffer();
	static PrintWriter htmlOut = null;

	static void println(String s)
	{
		System.out.println(s);
		if(htmlOut!=null)
		{
			String endedLine = s+"</br>";
			htmlLastFullOutput.append(endedLine);
			htmlOut.println(endedLine);
			//		sb.append(s);
			//		sb.append("</br>\n");
		}
	}

	static void println() {sb.append("</br>\n");}

	public static void mailinglistRoster(String listName,String metric, String username, String password) throws MalformedURLException, IOException
	{
		String url = OTHER_MAILINGLISTS_ROSTER_PREFIX+listName+OTHER_MAILINGLISTS_ROSTER_SUFFIX;
		String content = loadPostContentAdmin(url,username,password);
//		System.out.print("Calculating mailing list member count for "+url);
		Matcher matcher = Pattern.compile("(\\d+) members total").matcher(content);
		matcher.find();
		int memberCount = Integer.valueOf(matcher.group(1));
		metric=metric+'-'+"roster";
		logMetric(metric,memberCount);
	}

	public static void mailinglist(String listName, String metric, String url, String username, String password) throws MalformedURLException, IOException
	{
		if(!url.endsWith("/")) url = url+"/";

		String content = loadPostContentUser(url,username,password);
		//				println(content);
		Matcher matcher = Pattern.compile("href=\"([^\"]+/date.html)\">\\[ Date \\]").matcher(content);
		int messageCount = 0;
		int mainMailinglistMessageCountUntil2013Inclusive = 952;
//		System.out.print("Calculating mailing list post count for "+url);
		while(matcher.find())
		{
//			System.out.print(".");
			String monthUrl = matcher.group(1);
			// faster processing of main mailinglist by manually adding precalculated 2013 values and skipping those in the counting
			if(url.equals(MAIN_MAILINGLIST_URL)&&!monthUrl.contains("2014")) continue;
			String newContent = loadPostContentUser(url+monthUrl,username,password);
			Matcher singleMatcher = Pattern.compile("<b>Messages:</b> (\\d+)<p>").matcher(newContent);
			singleMatcher.find();
			messageCount+=Integer.valueOf(singleMatcher.group(1));
		}
		println();
		if(url.equals(MAIN_MAILINGLIST_URL)) messageCount+=mainMailinglistMessageCountUntil2013Inclusive;
		logMetric(metric,messageCount);
		// roster needs admin credentials but we don't have those for main mailinglist
		if(!url.equals(MAIN_MAILINGLIST_URL)) mailinglistRoster(listName,metric, username, password);
	}

	public static void mailinglist() throws MalformedURLException, IOException
	{
		println("<h3>C2 Activities and interactions on blog and discussion lists</h3>");
		//		mailinglist(MESSAGE_COUNT,MAIN_MAILINGLIST_URL,MAIN_MAILINGLIST_EMAIL,MAIN_MAILINGLIST_PASSWORD);
		for(String listName: OTHER_MAILINGLISTS)
		{
			String url = OTHER_MAILINGLISTS_PREFIX+listName;
			mailinglist(listName,MESSAGE_COUNT+'-'+listName,url,MAILINGLIST_ADMIN_USERNAME, MAILINGLIST_ADMIN_PASSWORD);
		}
	}

	public static void bibsonomy() throws MalformedURLException, IOException
	{
		println("<h3>C6 Academic Publications</h3>");
		String content = loadContent(BIBSONOMY_URL);
		Matcher matcher = Pattern.compile("total:  (\\d+) publications").matcher(content);
		if(matcher.find()) logMetric(PUBLICATIONS,Integer.valueOf(matcher.group(1)));
	}

	public static void blogposts() throws MalformedURLException, IOException
	{
		println("<h3>C2 Activities and interactions on blog and discussion lists</h3>");
		int page;
		{
			String content = loadContent(BLOG_URL);
			Matcher matcher = Pattern.compile("<a title=\"Go to last page\" href=\"/blog\\?page=(\\d+)\">").matcher(content);
			matcher.find();
			page = Integer.valueOf(matcher.group(1));
		}
		try(Scanner lastPage = new Scanner(new URL(BLOG_URL+"?page="+page).openStream(), "UTF-8"))
		{
			String content = lastPage.useDelimiter("\\A").next();
			int postCount = page*10; // starts with page 0, so page 7 has page number 6 and thus 6 full pages of 10 posts
			Matcher matcher = Pattern.compile("<h2 class=\"title\" property=\"dc:title\"").matcher(content);

			while (matcher.find()) {postCount++;}

			logMetric(BLOG_POSTS,postCount);
		}
	}

	// copy of blogposts()
	public static void textInterviews() throws MalformedURLException, IOException
	{
		println("<h3>C10 Expert Interviews</h3>");
		int page;
		{
			String content = loadContent(INTERVIEW_URL);
			Matcher matcher = Pattern.compile("<a title=\"Go to last page\" href=\"/blog\\?page=(\\d+)\">").matcher(content);
			if(matcher.find())
			{page = Integer.valueOf(matcher.group(1));} else {page=0;}
		}
		{
			String content = loadContent(INTERVIEW_URL+(page>0?"?page="+page:""));
			int postCount = page*10; // starts with page 0, so page 7 has page number 6 and thus 6 full pages of 10 posts
			Matcher matcher = Pattern.compile(">View as PDF<").matcher(content);
			while (matcher.find()) {postCount++;}

			logMetric(INTERVIEWS,postCount);
		}
	}

	public static void slideShare()
	{
		println("<h3>C4 Number of figures on BIG Slideshares account</h3>");
		SlideShareAPI ssapi = SlideShareAPIFactory.getSlideShareAPI(SLIDESHARE_KEY,SLIDESHARE_SECRET);

		User bigProject = ssapi.getSlideshowByUser("BIG-Project");

		int interviewViews = 0;
		int presentationViews = 0;

		Collection<Slideshow> slideShows = bigProject.getSlideshows();

		int internal = 0;
		int external = 0;
		logMetric(SLIDESHARES_INTERNAL,(internal=slideShows.size()));
		logMetric(SLIDESHARES_EXTERNAL,(external=ssapi.getSlideshowByTag("big-fp7-project").getCount()));
		logMetric(SLIDESHARES_TOTAL,(internal+external));

		for(Slideshow slideShow: slideShows)
		{
			if(slideShow.getTitle().contains("Data Curation Interview")) interviewViews+=slideShow.getViews();
			else presentationViews+=slideShow.getViews();
		}

		logMetric(SLIDESHARES_PRESENTATION_VIEWS,presentationViews);
		logMetric(SLIDESHARES_INTERVIEW_VIEWS,interviewViews);
	}

	public static void twitter() throws MalformedURLException, IOException
	{
		println("<h3>C1 Activities and Interactions on Social Media</h3>");
		String content = loadContent(TWITTER_URL);
		HashMap<String,Matcher> matchers = new HashMap<>();
		matchers.put(TWITTER_TWEETS,		Pattern.compile("title=\"(\\d+)[^\"]+\" data-nav=\"tweets\"").matcher(content));
		matchers.put(TWITTER_FOLLOWING,	Pattern.compile("title=\"(\\d+)[^\"]+\" data-nav=\"following\"").matcher(content));
		matchers.put(TWITTER_FOLLOWERS,	Pattern.compile("title=\"(\\d+)[^\"]+\" data-nav=\"followers\"").matcher(content));
		matchers.forEach((s,m) -> {m.find();logMetric(s,Integer.valueOf(m.group(1)));});
	}

	public static String loadContent(String url) throws MalformedURLException, IOException
	{
		try(Scanner in = new Scanner(new URL(url).openStream(), "UTF-8"))
		{return in.useDelimiter("\\A").next();}
	}

	@Override public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException
	{
		response.setContentType("text/html");
		PrintWriter pw = response.getWriter();
		htmlOut = pw;
		pw.println("<html>");
		pw.println("<head><title>BIG FP7 Dissemination Report</title></head>");
		pw.println("<body>");
		pw.println("<h1>BIG FP7 Dissemination Report </h1>");
		createReport();
		pw.println("</body></html>");
	}

	static void save() throws IOException
	{
		Map<Long,Map<String,Integer>> metricss = load();
		metricss.put(Instant.now().getEpochSecond(),metrics);
		synchronized (metricss)
		{
			try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(metricsFile)))
			{out.writeObject(metricss);}
		}
	}

	/**@return a map from unix time (seconds) to the metrics of that time
	 * @throws IOException	 */
	@SuppressWarnings("unchecked") static Map<Long,Map<String,Integer>> load() throws IOException
	{
		Map<Long,Map<String,Integer>> metricss = new HashMap<>();
		if(metricsFile.exists())
		{
			try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(metricsFile)))
			{return (Map<Long,Map<String,Integer>>) in.readObject();}
			catch (ClassNotFoundException e) {throw new RuntimeException(e);}
		}
		return metricss;
	}

	static void createReport() throws MalformedURLException, IOException
	{
		Map<Long,Map<String,Integer>> oldMetricss = load();
		SortedSet<Long> times = new TreeSet<>(oldMetricss.keySet());
		long lastSecondsAgo = times.isEmpty()?(Long.MAX_VALUE):(Instant.now().getEpochSecond()-times.last());
		if(lastSecondsAgo<MINIMAL_RECALCULATE_INTERVAL_SECONDS)
		{
			if(htmlOut!=null)
			{
				htmlOut.print("Last call only "+lastSecondsAgo+" seconds ago, using cached output. If you want recalculated output, please wait until "+MINIMAL_RECALCULATE_INTERVAL_SECONDS+" seconds are elapsed.");
				htmlOut.print(htmlLastFullOutput);
			}
			System.out.println("Last call only "+lastSecondsAgo+" seconds ago, using cached output. If you want recalculated output, please wait until "+MINIMAL_RECALCULATE_INTERVAL_SECONDS+" seconds are elapsed.");
			System.out.println(htmlLastFullOutput);

			htmlLastFullOutput.setLength(0);
		} else
		{
			println();
			twitter();
			blogposts();
			bibsonomy();
			mailinglist();
			slideShare();
			textInterviews();

			if(oldMetricss.isEmpty()) {println("== no old metrics to compare ==");}
			else {println("<h2>Old Metrics and their differences</h2>");}

			for(long unixTime: times)
			{
				println("<h3>"+Instant.ofEpochSecond(unixTime)+"</h3>");
				Map<String,Integer> oldMetrics = oldMetricss.get(unixTime);
				for(String m:metrics.keySet())
				{
					if(!oldMetrics.containsKey(m)) {println("no value in old metrics for metric "+m);continue;}
					println(m+": "+oldMetrics.get(m)+" ("+(oldMetrics.get(m)-metrics.get(m))+")");
				}
			}
			save();
		}
	}


	public static void main(String[] args) throws MalformedURLException, IOException
	{
		createReport();
	}
}