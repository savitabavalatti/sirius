/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 14.07.13
 * Time: 10:00
 * To change this template use File | Settings | File Templates.
 */
public interface Mapper<V, X> {
    X apply(V value) throws Exception;
}
