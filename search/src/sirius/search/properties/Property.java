/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.properties;

import org.elasticsearch.common.xcontent.XContentBuilder;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;
import sirius.search.Entity;
import sirius.search.Index;
import sirius.search.annotations.NotNull;
import sirius.web.security.UserContext;
import sirius.web.http.WebContext;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * A property describes how a field is persisted into the database and loaded back.
 * <p>
 * It also takes care of creating an appropriate mapping. Properties are generated by {@link PropertyFactory}
 * instances which generate a property for a given field of a class.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class Property {

    /**
     * Contains the underlying field for which the property was created
     */
    protected Field field;

    /**
     * Determines if <tt>null</tt> is accepted as a value for this property
     */
    protected final boolean nullAllowed;

    /**
     * Generates a new property for the given field
     *
     * @param field the underlying field from which the property was created
     */
    public Property(Field field) {
        this.field = field;
        this.field.setAccessible(true);
        this.nullAllowed = !field.getType().isPrimitive() && !field.isAnnotationPresent(NotNull.class);
    }

    /**
     * Initializes the property (field) of the given entity.
     *
     * @param entity the entity to initialize
     * @throws Exception in case of an error when initializing the entity
     */
    public void init(Entity entity) throws Exception {

    }

    /**
     * Some properties auto-create a value and therefore no setter for the given field should be defined.
     *
     * @return <tt>true</tt> if no setter for this property should be present, <tt>false</tt> otherwise
     */
    public boolean acceptsSetter() {
        return true;
    }

    /**
     * Returns the name of the property
     *
     * @return the name of the property, which is normally just the field name
     */
    public String getName() {
        return field.getName();
    }

    /**
     * Determines if <tt>null</tt> values are accepted by this property
     *
     * @return <tt>true</tt> if the property accepts <tt>null</tt> values, <tt>false</tt> otherwise
     */
    public boolean isNullAllowed() {
        return nullAllowed;
    }

    /**
     * Generates the representation of the entities field value to be stored in the database.
     *
     * @param entity the entity which field value is to be stored
     * @return the storable representation of the value
     */
    public Object writeToSource(Entity entity) {
        try {
            return transformToSource(field.get(entity));
        } catch (IllegalAccessException e) {
            Exceptions.handle(Index.LOG, e);
            return null;
        }
    }

    /**
     * Transforms the given field value to the representation which is stored in the database.
     *
     * @param o the value to transform
     * @return the storable representation of the value
     */
    protected Object transformToSource(Object o) {
        return o;
    }

    /**
     * Converts the given value back to its original form and stores it as the given entities field value.
     *
     * @param entity the entity to update
     * @param value  the stored value from the database
     */
    public void readFromSource(Entity entity, Object value) {
        try {
            Object val = transformFromSource(value);
            field.set(entity, val);
            entity.setSource(field.getName(), val);
        } catch (IllegalAccessException e) {
            Exceptions.handle(Index.LOG, e);
        }
    }

    /**
     * Transforms the given field value from the representation which is stored in the database.
     *
     * @param value the value to transform
     * @return the original representation of the value
     */
    protected Object transformFromSource(Object value) {
        return value;
    }

    /**
     * Generates the mapping used by this property
     *
     * @param builder the builder used to generate JSON
     * @throws IOException in case of an io error while generating the mapping
     */
    public void createMapping(XContentBuilder builder) throws IOException {
        builder.startObject(getName());
        builder.field("type", getMappingType());
        builder.field("store", isStored() ? "yes" : "no");
        builder.field("index", "not_analyzed");
        builder.endObject();
    }

    /**
     * Returns the data type used in the mapping
     *
     * @return the name of the data type used in the mapping
     */
    protected String getMappingType() {
        return "string";
    }

    /**
     * Determines if this property is stored as separate field in its original form.
     *
     * @return <tt>true</tt> if the raw values is stored as extra field in ElasticSearch, <tt>false</tt> otherwise
     */
    protected boolean isStored() {
        return false;
    }

    /**
     * Returns and converts the field value from the given request and writes it into the given entity.
     *
     * @param entity the entity to update
     * @param ctx    the request to read the data from
     */
    public void readFromRequest(Entity entity, WebContext ctx) {
        try {
            if (ctx.get(getName()).isNull()) {
                return;
            }
            field.set(entity, transformFromRequest(getName(), ctx));
        } catch (IllegalAccessException e) {
            Exceptions.handle(Index.LOG, e);
        }
    }

    /**
     * Extracts and converts the value from the given request.
     *
     * @param name name of the parameter to read
     * @param ctx  the request to read the data from
     * @return the converted value which can be assigned to the field
     */
    protected Object transformFromRequest(String name, WebContext ctx) {
        Value value = ctx.get(name);
        if (value.isEmptyString() && !field.getType().isPrimitive()) {
            return null;
        }
        Object result = value.coerce(field.getType(), null);
        if (result == null) {
            UserContext.setFieldError(name, value.get());
            throw Exceptions.createHandled()
                            .withNLSKey("Property.invalidInput")
                            .set("field", NLS.get(field.getDeclaringClass().getSimpleName() + "." + name))
                            .set("value", value.asString())
                            .handle();
        }

        return result;
    }

    /**
     * Provides access to the underlying java field
     *
     * @return the field upon which the property was created
     */
    public Field getField() {
        return field;
    }
}
