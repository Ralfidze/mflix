package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.swing.event.DocumentEvent;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

  private final MongoCollection<User> usersCollection;
  //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
  //returns a Session object
  private final MongoCollection<Session> sessionsCollection;

  private final Logger log;

  @Autowired
  public UserDao(
      MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
    super(mongoClient, databaseName);
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
    log = LoggerFactory.getLogger(this.getClass());
    //TODO> Ticket: User Management - implement the necessary changes so that the sessions
    // collection returns a Session objects instead of Document objects.
    sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
  }

  /**
   * Inserts the `user` object in the `users` collection.
   *
   * @param user - User object to be added
   * @return True if successful, throw IncorrectDaoOperation otherwise
   */
  public boolean addUser(User user) {
    //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
    try {
      usersCollection.insertOne(user);
      return true;
    }
    catch (Exception ex){
      throw new IncorrectDaoOperation(String.format("User %s wasn't added", user.getName()), ex);
    }
    //TODO > Ticket: Handling Errors - make sure to only add new users
    // and not users that already exist.

  }

  /**
   * Creates session using userId and jwt token.
   *
   * @param userId - user string identifier
   * @param jwt - jwt string token
   * @return true if successful
   */
  public boolean createUserSession(String userId, String jwt) {
    //TODO> Ticket: User Management - implement the method that allows session information to be
    // stored in it's designated collection.
    Bson filter = new Document("user_id", userId);
    Bson update = Updates.set("jwt", jwt);

    UpdateOptions options = new UpdateOptions().upsert(true);
    try{
      UpdateResult updateResult = sessionsCollection.updateOne(filter,update, options);
      return updateResult.wasAcknowledged();
    }
    catch (MongoWriteException ex){
      return false;
    }

    //TODO > Ticket: Handling Errors - implement a safeguard against
    // creating a session with the same jwt token.
  }

  /**
   * Returns the User object matching the an email string value.
   *
   * @param email - email string to be matched.
   * @return User object or null.
   */
  public User getUser(String email) {
    //TODO> Ticket: User Management - implement the query that returns the first User object.

    return usersCollection.find(new Document("email", email)).first();
  }

  /**
   * Given the userId, returns a Session object.
   *
   * @param userId - user string identifier.
   * @return Session object or null.
   */
  public Session getUserSession(String userId) {
    //TODO> Ticket: User Management - implement the method that returns Sessions for a given
    // userId
    return sessionsCollection.find(new Document("user_id", userId)).first();
  }

  public boolean deleteUserSessions(String userId) {
    //TODO> Ticket: User Management - implement the delete user sessions method
    Bson filter = new Document("user_id", userId);
    DeleteResult deleteResult = sessionsCollection.deleteOne(filter);
    return deleteResult.wasAcknowledged();
  }

  /**
   * Removes the user document that match the provided email.
   *
   * @param email - of the user to be deleted.
   * @return true if user successfully removed
   */
  public boolean deleteUser(String email) {
    // remove user sessions
    //TODO> Ticket: User Management - implement the delete user method
    //TODO > Ticket: Handling Errors - make this method more robust by
    // handling potential exceptions.
    if(deleteUserSessions(email)) {
      Bson deleteFilter = new Document("email", email);
      DeleteResult deleteResult = usersCollection.deleteOne(deleteFilter);
      return deleteResult.wasAcknowledged();
    }
    else
      return false;
  }

  /**
   * Updates the preferences of an user identified by `email` parameter.
   *
   * @param email - user to be updated email
   * @param userPreferences - set of preferences that should be stored and replace the existing
   *     ones. Cannot be set to null value
   * @return User object that just been updated.
   */
  public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
    //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
    // be updated.
    //TODO > Ticket: Handling Errors - make this method more robust by
    // handling potential exceptions when updating an entry.

    if(userPreferences == null)
      throw new IncorrectDaoOperation("no user preferences");

    Bson filter = new Document("email", email);
    User userToupdatePrefs = usersCollection.find(filter).first();
    if(userToupdatePrefs == null)
      throw new IncorrectDaoOperation("no user to update");
    Map<String, String> oldPrefs = userToupdatePrefs.getPreferences();
    if(oldPrefs == null)
      oldPrefs = new HashMap<>();
    for(Map.Entry<String, ?> entry : userPreferences.entrySet()){
        oldPrefs.put(entry.getKey(),entry.getValue().toString());
    }

    UpdateResult result = usersCollection.updateOne(filter,Updates.set("preferences", oldPrefs));
    return result.wasAcknowledged();

  }
}
