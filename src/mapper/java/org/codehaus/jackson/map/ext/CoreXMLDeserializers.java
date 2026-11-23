package org.codehaus.jackson.map.ext;

import java.io.IOException;
import java.util.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.map.deser.std.FromStringDeserializer;
import org.codehaus.jackson.map.deser.std.StdScalarDeserializer;
import org.codehaus.jackson.map.util.Provider;

/**
 * Container deserializers that handle "core" XML types: ones included in standard
 * JDK 1.5. Types are directly needed by JAXB, and are thus supported within core
 * mapper package, not in "xc" package.
 *
 * @since 1.3
 */
public class CoreXMLDeserializers
    implements Provider<StdDeserializer<?>>
{
    /**
     * Data type factories are thread-safe after instantiation (and
     * configuration, if any); and since instantion (esp. implementation
     * introspection) can be expensive we better reuse the instance.
     */
    final static DatatypeFactory _dataTypeFactory;
    static {
        try {
            _dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Maximum length of a textual {@link Duration} or {@link XMLGregorianCalendar}
     * value accepted for deserialization. {@link DatatypeFactory} parses numeric
     * components of these values with the O(n^2) {@code BigInteger(String)} and
     * {@code BigDecimal(String)} constructors, so overly long values must be
     * rejected before they can reach it.
     *<p>
     * [databind#6127]: there are no stream read constraints to bound this with,
     * so a fixed limit is used instead.
     */
    protected final static int MAX_VALUE_LENGTH = 2000;

    /**
     * Helper method for constructing the description of a textual value that
     * exceeds {@link #MAX_VALUE_LENGTH}.
     */
    protected static String _valueTooLongMessage(String value)
    {
        return String.format("value length (%d) exceeds the maximum allowed (%d)",
                value.length(), MAX_VALUE_LENGTH);
    }

    /*
    /**********************************************************
    /* Provider implementation
    /**********************************************************
     */

    /**
     * Method called by {@link org.codehaus.jackson.map.deser.BasicDeserializerFactory}
     * to register deserializers this class provides.
     */
    @Override
    public Collection<StdDeserializer<?>> provide()
    {
        return Arrays.asList(new StdDeserializer<?>[] {
            new DurationDeserializer()
            ,new GregorianCalendarDeserializer()
            ,new QNameDeserializer()
        });
    }
    
    /*
    /**********************************************************
    /* Concrete deserializers
    /**********************************************************
     */

    public static class DurationDeserializer
        extends FromStringDeserializer<Duration>
    {
        public DurationDeserializer() { super(Duration.class); }
    
        @Override
        protected Duration _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            // [databind#6127] Length of the whole value is checked as a conservative
            // upper bound: it is never shorter than any single numeric component.
            if (value.length() > MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException(_valueTooLongMessage(value));
            }
            return _dataTypeFactory.newDuration(value);
        }
    }

    public static class GregorianCalendarDeserializer
        extends StdScalarDeserializer<XMLGregorianCalendar>
    {
        public GregorianCalendarDeserializer() { super(XMLGregorianCalendar.class); }
        
        @Override
        public XMLGregorianCalendar deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // [databind#6127] Similar to DurationDeserializer: length of a textual
            // value is checked before it is parsed any further
            if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
                String text = jp.getText().trim();
                if (text.length() > MAX_VALUE_LENGTH) {
                    throw ctxt.weirdStringException(_valueClass, _valueTooLongMessage(text));
                }
            }
            Date d = _parseDate(jp, ctxt);
            if (d == null) {
                return null;
            }
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(d);
            return _dataTypeFactory.newXMLGregorianCalendar(calendar);
        }
    }

    public static class QNameDeserializer
        extends FromStringDeserializer<QName>
    {
        public QNameDeserializer() { super(QName.class); }
        
        @Override
        protected QName _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return QName.valueOf(value);
        }
    }
}
