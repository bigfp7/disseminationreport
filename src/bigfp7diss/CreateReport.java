package bigfp7diss;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.benfante.jslideshare.SlideShareAPI;
import com.benfante.jslideshare.SlideShareAPIFactory;
import com.benfante.jslideshare.messages.Slideshow;
import com.benfante.jslideshare.messages.User;

public class CreateReport
{	
	private static final String BIBSONOMY_URL	= "http://www.bibsonomy.org/user/bigfp7";
	private static final String TWITTER_URL	= "https://twitter.com/BIG_FP7";
	private static final String	MAILINGLIST_URL	= "http://lists.atosresearch.eu/mailman/private/bigdata/";
	private static final String	BLOG_URL	= "http://big-project.eu/blog";

	static Map<String,Integer> metrics = new HashMap<>();

	static void logMetric(String s,int v)
	{
		System.out.println(s+'\t'+v);
		metrics.put(s, v);
	}

	public static String loadPostContent(String url, String username, String password) throws MalformedURLException, IOException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write("username="+username+"&password="+password);
		writer.close();

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			try(Scanner in = new Scanner(connection.getInputStream(), "UTF-8"))
			{
				return in.useDelimiter("\\A").next();
			}
		}
		else
		{
			throw new RuntimeException(connection.getResponseMessage());
		}
	}

	public static void mailinglist(String email, String password) throws MalformedURLException, IOException
	{
		System.out.println("=== C2 Activities and interactions on blog and discussion lists ===");

		String content = loadPostContent(MAILINGLIST_URL, email, password);
		//				System.out.println(content);
		Matcher matcher = Pattern.compile("href=\"([^\"]+/date.html)\">\\[ Date \\]").matcher(content);
		int messageCount2014 = 0;
		int messageCountUntil2013Inclusive = 952;
		System.out.print("Calculating mailing list post count");
		while(matcher.find())
		{
			System.out.print(".");
			String monthUrl = matcher.group(1);
			if(!monthUrl.contains("2014")) continue;
			String newContent = loadPostContent(MAILINGLIST_URL+monthUrl,email,password);
			Matcher singleMatcher = Pattern.compile("<b>Messages:</b> (\\d+)<p>").matcher(newContent);
			singleMatcher.find();
			messageCount2014+=Integer.valueOf(singleMatcher.group(1));
			//			if(!matcher.group(1).contains("2014")) messageCountUntil2013Inclusive+=Integer.valueOf(singleMatcher.group(1));
		}
		System.out.println();
		logMetric("Message Count",messageCount2014+messageCountUntil2013Inclusive);
		//		System.out.println("Message Count until 2013 inclusive\t"+messageCountUntil2013Inclusive);
	}

	public static void bibsonomy() throws MalformedURLException, IOException
	{
		System.out.println("=== C6 Academic Publications ===");
		String content = loadContent(BIBSONOMY_URL);
		Matcher matcher = Pattern.compile("total:  (\\d+) publications").matcher(content);
		if(matcher.find()) logMetric("Number of publications",Integer.valueOf(matcher.group(1)));
	}


	public static void blogposts() throws MalformedURLException, IOException
	{
		System.out.println("=== C2 Activities and interactions on blog and discussion lists ===");	
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

			logMetric("Number of blog posts",postCount);
		}		
	}

	public static void slideShare(String apiKey, String sharedSecret)
	{
		System.out.println("=== C4 Number of figures on BIG Slideshares account ===");
		SlideShareAPI ssapi = SlideShareAPIFactory.getSlideShareAPI(apiKey,sharedSecret);

		User bigProject = ssapi.getSlideshowByUser("BIG-Project");

		int interviewViews = 0;
		int presentationViews = 0;

		Collection<Slideshow> slideShows = bigProject.getSlideshows();

		int internal = 0;
		int external = 0;
		logMetric("Number of internal slideshares",(internal=slideShows.size()));
		logMetric("Number of external slideshares",(external=ssapi.getSlideshowByTag("big-fp7-project").getCount()));
		logMetric("Number Slideshares total\t",(internal+external));

		for(Slideshow slideShow: slideShows)
		{
			if(slideShow.getTitle().contains("Data Curation Interview")) interviewViews+=slideShow.getViews();
			else presentationViews+=slideShow.getViews();
		}

		logMetric("presentation views",presentationViews);
		logMetric("interview views",interviewViews);		
	}

	public static void twitter() throws MalformedURLException, IOException
	{
		System.out.println("=== C1 Activities and Interactions on Social Media ===");
		String content = loadContent(TWITTER_URL);
		HashMap<String,Matcher> matchers = new HashMap<>();
		matchers.put("Tweets",		Pattern.compile("title=\"(\\d+)[^\"]+\" data-nav=\"tweets\"").matcher(content));		
		matchers.put("Following",	Pattern.compile("title=\"(\\d+)[^\"]+\" data-nav=\"following\"").matcher(content));
		matchers.put("Followers",	Pattern.compile("title=\"(\\d+)[^\"]+\" data-nav=\"followers\"").matcher(content));
		matchers.forEach((s,m) -> {m.find();logMetric(s,Integer.valueOf(m.group(1)));});
	}

	public static String loadContent(String url) throws MalformedURLException, IOException
	{
		try(Scanner in = new Scanner(new URL(url).openStream(), "UTF-8"))
		{return in.useDelimiter("\\A").next();}
	}

	public static void main(String[] args) throws MalformedURLException, IOException
	{
		System.out.println("================== Statistics that don't need credentials =============");
		twitter();
		blogposts();
		bibsonomy();
		System.out.println("================== Statistics that need credentials =============");
		if(args.length<4)
		{
			System.err.println("Please call CreateReport with 4 parameters: <api-key> <shared secret> <your mailing list email adress> <your mailing list password>");
			System.exit(1);
		}
		slideShare(args[0],args[1]);
		mailinglist(args[2],args[3]);
	}		
}