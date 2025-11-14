package io.quarkus.ts.infinispan.client;

import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.infinispan.client.hotrod.RemoteCache;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.ts.infinispan.client.serialized.Book;

@Path("/tx-books")
public class TransactionalBookResource {

    @Inject
    @Remote("bookstx")
    RemoteCache<String, Book> txBooksCache;

    @POST
    @Path("/commit")
    @Produces(MediaType.TEXT_PLAIN)
    public String addBooksWithCommit() {
        TransactionManager tm = txBooksCache.getTransactionManager();
        if (tm == null) {
            return "ERROR: TransactionManager is null";
        }

        try {
            tm.begin();
            txBooksCache.put("hp-1", new Book("Harry Potter 1"));
            txBooksCache.put("hp-2", new Book("Harry Potter 2"));
            txBooksCache.put("hp-3", new Book("Harry Potter 3"));
            tm.commit();
            return "Committed: 3 books added";
        } catch (Exception e) {
            try {
                tm.rollback();
            } catch (Exception ignored) {
            }
            return "ERROR: " + e.getMessage();
        }
    }

    @POST
    @Path("/rollback")
    @Produces(MediaType.TEXT_PLAIN)
    public String addBooksWithRollback() {
        TransactionManager tm = txBooksCache.getTransactionManager();
        if (tm == null) {
            return "ERROR: TransactionManager is null";
        }

        try {
            tm.begin();
            txBooksCache.put("got-1", new Book("Game of Thrones 1"));
            txBooksCache.put("got-2", new Book("Game of Thrones 2"));
            tm.rollback();
            return "Rolled back: books NOT added";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public int getCacheSize() {
        return txBooksCache.size();
    }

    @DELETE
    @Path("/clear")
    public void clearCache() {
        txBooksCache.clear();
    }
}
