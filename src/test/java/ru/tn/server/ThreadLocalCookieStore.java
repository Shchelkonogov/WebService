package ru.tn.server;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

/**
 * @author Maksim Shchelkonogov
 */
public class ThreadLocalCookieStore implements CookieStore {

    private final static ThreadLocal<CookieStore> stores = new ThreadLocal<CookieStore>() {
        @Override
        protected synchronized CookieStore initialValue() {
            return (new CookieManager()).getCookieStore(); //InMemoryCookieStore
        }
    };

    @Override public void add(URI uri, HttpCookie cookie) { getStore().add(uri,cookie); }
    @Override public List<HttpCookie> get(URI uri) { return getStore().get(uri); }
    @Override public List<HttpCookie> getCookies() { return getStore().getCookies(); }
    @Override public List<URI> getURIs() { return getStore().getURIs(); }
    @Override public boolean remove(URI uri, HttpCookie cookie) { return getStore().remove(uri,cookie); }
    @Override public boolean removeAll() { return getStore().removeAll(); }
    @Override public int hashCode() { return getStore().hashCode(); }

    protected CookieStore getStore() { return stores.get(); }
    public void purgeStore() { stores.remove(); }
}
