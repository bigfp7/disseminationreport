package bigfp7diss;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

public class FollowAdder
{
	static Properties properties = new Properties();
	static
	{
		try
		{
			InputStream in = new FileInputStream("twitter.properties");
			properties.load(in);
			in.close();			
		}
		catch(Exception e) {throw new RuntimeException(e);}
	}

	final static String user = "BIG_FP7";
	final static String API_KEY = properties.getProperty("api.key");
	final static String API_SECRET = properties.getProperty("api.secret");
	final static String TAG = "#bigdata";
	private static final String	ACCESS_TOKEN	= properties.getProperty("access.token");
	private static final String	ACCESS_TOKEN_SECRET	= properties.getProperty("access.token.secret");
	final static short USER_COUNT = 10;
	final static short TWEET_COUNT = 10;
	final static String SINCE = "2014-1-1";

	public static void main(String[] args) throws TwitterException
	{
		Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(API_KEY, API_SECRET);
		twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN,ACCESS_TOKEN_SECRET));
		//		storeAccessToken(twitter.verifyCredentials().getId() , ACCESS_TOKEN);

		//		IDs ids = twitter.getFollowersIDs(arg0);
		Query userQuery = new Query(TAG);
		userQuery.setSince(SINCE);
		userQuery.setCount(USER_COUNT);

		QueryResult result = twitter.search(userQuery);
		Set<User> users = result.getTweets().stream().map(s -> s.getUser()).collect(Collectors.toSet());
		System.out.println(users.size());

		Map<User,Float> userToScore = new HashMap<>();

		for(User user: users)
		{
			Query q = new Query('@'+user.getScreenName());
			q.setSince(SINCE);

			q.setCount(TWEET_COUNT);
			//			Set<Tweets>
			float score = (float)twitter.search(q).getTweets().stream().filter(s->s.getText().contains(TAG)).count()/TWEET_COUNT;
			userToScore.put(user, score);
			//			System.out.println(user.getScreenName()+" "+score);
		}
		Comparator<User> c  = (u, v) -> userToScore.get(u).compareTo(userToScore.get(v));
		Comparator<User> c2 = (u, v) -> u.getScreenName().compareTo(v.getScreenName()); // so that two with identical scores dont get equalized
		SortedSet<User> sortedUsers = new TreeSet<>(c.thenComparing(c2).reversed());
		sortedUsers.addAll(users);

		for(User user: sortedUsers)
		{
			System.out.println(user.getScreenName()+" "+userToScore.get(user));
		}

	}
}