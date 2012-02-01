import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsAnything.anything;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


public class BasicTest extends UnitTest {
    @Before
    public void setup() {
        Fixtures.deleteDatabase();
    }

    @Test
    public void createAndRetrieveUser() {
        new User("bob@gmail.com", "secret", "Bob").save();

        User bob = User.find("byEmail", "bob@gmail.com").first();
        assertThat(bob, notNullValue());
        assertThat(bob.fullname, is("Bob"));
    }

    @Test
    public void tryConnectAsUser() {
        new User("bob@gmail.com", "secret", "Bob").save();

        assertThat(User.connect("bob@gmail.com", "secret"), notNullValue());
        assertThat(User.connect("bob@gmail.com", "badpassword"), nullValue());
        assertThat(User.connect("tom@gmail.com", "secret"), nullValue());
    }

    @Test
    public void createPost() {
        User bob = new User("bob@gmail.com", "secret", "Bob").save();
        Post post = new Post(bob, "My first post", "Hello World").save();

        assertThat(Post.count(), is(1L));

        List<Post> posts = Post.find("byAuthor", bob).fetch();
        assertThat(posts.size(), is(1));

        Post firstPost = posts.get(0);
        assertThat(firstPost, notNullValue());
        assertThat(firstPost.author, is(bob));
        assertThat(firstPost.title, is("My first post"));
        assertThat(firstPost.content, is("Hello World"));
        assertThat(firstPost.postedAt, notNullValue());
    }

    @Test
    public void postComments() {
        User bob = new User("bob@gmail.com", "secret", "Bob").save();
        Post post = new Post(bob, "My first post", "Hello World").save();

        new Comment(post, "Jeff", "Nice post").save();
        new Comment(post, "Tom", "I knew that!").save();

        List<Comment> comments = Comment.find("byPost", post).fetch();
        assertThat(comments.size(), is(2));

        Comment first = comments.get(0);
        Comment second = comments.get(1);

        assertThat(first, notNullValue());
        assertThat(first.author, is("Jeff"));
        assertThat(first.content, is("Nice post"));
        assertThat(first.postedAt, notNullValue());

        assertThat(second, notNullValue());
        assertThat(second.author, is("Tom"));
        assertThat(second.content, is("I knew that!"));
        assertThat(second.postedAt, notNullValue());
    }

    @Test
    public void useTheCommentsRelation() {
        User bob = new User("bob@gmail.com", "secret", "Bob").save();
        Post post = new Post(bob, "My first post", "Hello World").save();

        post.addComment("Jeff", "Nice post");
        post.addComment("Tom", "I knew that!");

        assertThat(User.count(), is(1L));
        assertThat(Post.count(), is(1L));
        assertThat(Comment.count(), is(2L));

        Post p = Post.find("byAuthor", bob).first();
        assertThat(p, notNullValue());

        assertThat(p.comments.size(), is(2));
        assertThat(p.comments.get(0).author, is("Jeff"));

        p.delete();

        assertThat(User.count(), is(1L));
        assertThat(Post.count(), is(0L));
        assertThat(Comment.count(), is(0L));

    }

    @Test
    public void fullTest() {
        Fixtures.loadModels("data.yml");

        assertThat(User.count(), is(2L));
        assertThat(Post.count(), is(3L));
        assertThat(Comment.count(), is(3L));

        assertThat(User.connect("bob@gmail.com", "secret"), notNullValue());
        assertThat(User.connect("jeff@gmail.com", "secret"), notNullValue());
        assertThat(User.connect("jeff@gmail.com", "badpass"), nullValue());
        assertThat(User.connect("tom@gmail.com", "secret"), nullValue());

        List<Post> bobsPosts = Post.find("author.email", "bob@gmail.com").fetch();
        assertThat(bobsPosts.size(), is(2));

        List<Comment> commentsToBob = Comment.find("post.author.email", "bob@gmail.com").fetch();
        assertThat(commentsToBob.size(), is(3));

        Post frontPost = Post.find("order by postedAt desc").first();
        assertThat(frontPost, notNullValue());
        assertThat(frontPost.title, is("About the model layer"));

        assertThat(frontPost.comments.size(), is(2));

        frontPost.addComment("Jim", "Hello guys");
        assertThat(frontPost.comments.size(), is(3));
        assertThat(Comment.count(), is(4L));
    }
}
