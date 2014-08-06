package eu.bigproject.dissemination;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.Query.Unit;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

//@WebServlet(name="followadder",value="/followadder")
public class FollowAdder extends HttpServlet
{
	static Properties properties = new Properties();
	static
	{
		try
		{
			InputStream in = FollowAdder.class.getResourceAsStream("/access.properties");
			properties.load(in);
			in.close();			
		}
		catch(Exception e) {throw new RuntimeException(e);}
	}

	final static String USER = "BIG_FP7";
	final static String API_KEY = properties.getProperty("twitter.key");
	final static String API_SECRET = properties.getProperty("twitter.secret");
	final static String TAG = "#bigdata";
	//	final static String TAG2 = "#bigdata #eu";
	private static final String	ACCESS_TOKEN	= properties.getProperty("twitter.accesstoken");
	private static final String	ACCESS_TOKEN_SECRET	= properties.getProperty("twitter.accesstoken.secret");
	final static short TWEET_COUNT = 100;
	final static short TWEET_COUNT_FROM_USER = 10;
	final static String SINCE = "2014-06-01";
	final static GeoLocation EUROPE_CENTER = new GeoLocation(54.9, 25.316667);
	private static final float TRESHOLD	=  0.1f;

	static void bla() throws TwitterException, IOException
	{

	}

	//	public static void main(String[] args) throws TwitterException, IOException
	//	{
	//		bla();
	//	}

	
	@Override public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException
	{	
		response.setContentType("text/html");
		PrintWriter pw = response.getWriter();
		pw.println("<html>");
		pw.println("<head><title>BIG FP7 Follow Adder</title></head>");
		pw.println("<body>");
		pw.println("<h1>BIG FP7 Follow Adder</h1>");
		
		try{
			Twitter twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(API_KEY, API_SECRET);
			twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN,ACCESS_TOKEN_SECRET));

			if(req.getParameter("follow")!=null)
			{
				twitter.createFriendship(req.getParameter("follow"));
				return;
			}
			
			Set<User> users = null;
			{
				Query userQuery = new Query(TAG);
				userQuery.setSince(SINCE);
				userQuery.setCount(TWEET_COUNT);
				userQuery.setGeoCode(EUROPE_CENTER, 1784,Unit.km);
				QueryResult result = twitter.search(userQuery);
				users = result.getTweets().stream().map(s -> s.getUser()).collect(Collectors.toSet());
			}
//			List<User> friends = twitter.getFriendsList("@"+USER,1000);
//			if(friends.size()<400) throw new RuntimeException("less than 400 ("+friends.size()+") following, something is wrong");
//			pw.println(users.size());
//			users.removeAll(friends);
			pw.println(users.size()+" users analyzed");
			//		{
			//			Query userQuery = new Query(TAG2);
			//			userQuery.setSince(SINCE);
			//			userQuery.setCount(TWEET_COUNT);			
			//			QueryResult result = twitter.search(userQuery);
			//			users.addAll(result.getTweets().stream().map(s -> s.getUser()).collect(Collectors.toSet()));
			//		}
			//		pw.println(users.size());

			Map<User,Float> userToScore = new HashMap<>();

			for(User user: users)
			{				
				Query q = new Query('@'+user.getScreenName());
				q.setSince(SINCE);

				q.setCount(TWEET_COUNT_FROM_USER);
				//			Set<Tweets>
				List<Status> tweets = twitter.search(q).getTweets();
				float score = (float)tweets.stream().filter(s->s.getText().contains(TAG)).count()/Math.max(3, tweets.size());
				userToScore.put(user, score);
				//			pw.println(user.getScreenName()+" "+score);
			}
			Comparator<User> c  = (u, v) -> userToScore.get(u).compareTo(userToScore.get(v));
			Comparator<User> c2 = (u, v) -> u.getScreenName().compareTo(v.getScreenName()); // so that two with identical scores dont get equalized
			SortedSet<User> sortedUsers = new TreeSet<>(c.thenComparing(c2).reversed());

			sortedUsers.addAll(users.stream().filter(u -> userToScore.get(u)>TRESHOLD).collect(Collectors.toSet()));
			for(User u: sortedUsers)
			{
				pw.println(u.getScreenName()+" "+userToScore.get(u)+" <a href=\"?follow="+u.getScreenName()+"\">Follow</a>");							
			}
			pw.println("</body></html>");
		}
		catch (TwitterException e)
		{throw new RuntimeException(e);}
	}

	//	static boolean poll(String message)
	//	{
	//		
	//	}

}