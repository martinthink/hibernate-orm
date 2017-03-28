/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 * @author Luis Barreiro
 */
public class LazyCollectionDeletedTestTask extends AbstractEnhancerTestTask {
    private Long postId;

    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Post.class, Tag.class, AdditionalDetails.class};
    }

    public void prepare() {
        Configuration cfg = new Configuration();
        cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        super.prepare( cfg );

        try ( Session s = getFactory().openSession() ) {
            s.beginTransaction();

            Post post = new Post();

            Tag tag1 = new Tag();
            tag1.name = "tag1";
            Tag tag2 = new Tag();
            tag1.name = "tag2";

            Set<Tag> tagSet = new HashSet<>();
            tagSet.add( tag1 );
            tagSet.add( tag2 );
            post.tags = tagSet;

            AdditionalDetails details = new AdditionalDetails();
            details.post = post;
            details.details = "Some data";
            post.additionalDetails = details;

            postId = (Long) s.save( post );
            s.getTransaction().commit();
        }
    }

    public void execute() {
        try ( Session s = getFactory().openSession() ) {
            s.beginTransaction();

            Query query = s.createQuery( "from AdditionalDetails where id=" + postId );
            AdditionalDetails additionalDetails = (AdditionalDetails) query.uniqueResult();
            additionalDetails.details = "New data";
            s.persist( additionalDetails );

            // additionalDetais.post.tags get deleted on commit
            s.getTransaction().commit();
        }

        try ( Session s = getFactory().openSession() ) {
            s.beginTransaction();

            Query query = s.createQuery( "from Post where id=" + postId );
            Post retrievedPost = (Post) query.uniqueResult();

            assertFalse( "No tags found", retrievedPost.tags.isEmpty() );
            retrievedPost.tags.forEach( tag -> System.out.println( "Found tag: " + tag ) );

            s.getTransaction().commit();
        }
    }

    protected void cleanup() {
    }
}