package org.codehaus.jackson.map.deser;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Unit tests to verify that deeply nested JSON content bound as "untyped"
 * values ({@link java.lang.Object}, {@link java.util.Map},
 * {@link java.util.List}) or as {@link JsonNode} trees is rejected with
 * a {@link JsonParseException}, instead of failing with a
 * {@link StackOverflowError}.
 */
public class TestDeepNestingDeser
    extends BaseMapTest
{
    private final static int NOT_TOO_DEEP = 1000;

    private final static int TOO_DEEP_NESTING = 1024;

    // Deep enough to overflow the stack if nesting was not limited
    private final static int EXPLOIT_NESTING = 100000;

    private final static String TOO_DEEP_MSG = "JSON is too deeply nested.";

    static class UntypedBean {
        public Object value;
    }

    static class NodeBean {
        public JsonNode node;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    /*
    /**********************************************************
    /* Untyped (Object, Map, List) deserialization
    /**********************************************************
     */

    public void testTooDeepUntypedWithArray() throws Exception
    {
        _verifyTooDeep(_nestedArrays(TOO_DEEP_NESTING), Object.class);
    }

    public void testTooDeepUntypedWithObject() throws Exception
    {
        _verifyTooDeep(_nestedObjects(TOO_DEEP_NESTING), Object.class);
    }

    public void testTooDeepUntypedWithJavaArrays() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationConfig.Feature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        _verifyTooDeep(m, _nestedArrays(TOO_DEEP_NESTING), Object.class);
    }

    public void testTooDeepUntypedMapAndList() throws Exception
    {
        _verifyTooDeep(_nestedObjects(TOO_DEEP_NESTING), Map.class);
        _verifyTooDeep(_nestedArrays(TOO_DEEP_NESTING), List.class);
        _verifyTooDeep(_nestedArrays(TOO_DEEP_NESTING), Object[].class);
    }

    public void testTooDeepUntypedInBean() throws Exception
    {
        _verifyTooDeep("{\"value\":" + _nestedArrays(TOO_DEEP_NESTING) + "}", UntypedBean.class);
        _verifyTooDeep("{\"value\":" + _nestedObjects(TOO_DEEP_NESTING) + "}", UntypedBean.class);
    }

    // Exploit: nesting deep enough to cause StackOverflowError without the limit
    public void testVeryDeepUntypedFailsWithoutStackOverflow() throws Exception
    {
        _verifyTooDeep(_nestedArrays(EXPLOIT_NESTING), Object.class);
        _verifyTooDeep(_nestedObjects(EXPLOIT_NESTING), Object.class);
        _verifyTooDeep(_nestedObjects(EXPLOIT_NESTING), Map.class);
        _verifyTooDeep(_nestedArrays(EXPLOIT_NESTING), List.class);
    }

    public void testNotTooDeepUntypedArray() throws Exception
    {
        Object ob = mapper.readValue(_nestedArrays(NOT_TOO_DEEP), Object.class);
        assertTrue(ob instanceof List<?>);
        assertEquals(NOT_TOO_DEEP, _listDepth(ob));
    }

    public void testNotTooDeepUntypedObject() throws Exception
    {
        Object ob = mapper.readValue(_nestedObjects(NOT_TOO_DEEP), Object.class);
        assertTrue(ob instanceof Map<?,?>);
        assertEquals(NOT_TOO_DEEP + 1, _mapDepth(ob));
    }

    public void testNotTooDeepUntypedJavaArray() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationConfig.Feature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        Object ob = m.readValue(_nestedArrays(NOT_TOO_DEEP), Object.class);
        assertTrue(ob instanceof Object[]);
    }

    /*
    /**********************************************************
    /* Tree model (JsonNode) deserialization
    /**********************************************************
     */

    public void testTooDeepTreeWithArray() throws Exception
    {
        _verifyTooDeep(_nestedArrays(TOO_DEEP_NESTING), JsonNode.class);
        _verifyTooDeep(_nestedArrays(TOO_DEEP_NESTING), ArrayNode.class);
        _verifyTooDeepTree(_nestedArrays(TOO_DEEP_NESTING));
    }

    public void testTooDeepTreeWithObject() throws Exception
    {
        _verifyTooDeep(_nestedObjects(TOO_DEEP_NESTING), JsonNode.class);
        _verifyTooDeep(_nestedObjects(TOO_DEEP_NESTING), ObjectNode.class);
        _verifyTooDeepTree(_nestedObjects(TOO_DEEP_NESTING));
    }

    public void testTooDeepTreeInBean() throws Exception
    {
        _verifyTooDeep("{\"node\":" + _nestedArrays(TOO_DEEP_NESTING) + "}", NodeBean.class);
        _verifyTooDeep("{\"node\":" + _nestedObjects(TOO_DEEP_NESTING) + "}", NodeBean.class);
    }

    // Exploit: nesting deep enough to cause StackOverflowError without the limit
    public void testVeryDeepTreeFailsWithoutStackOverflow() throws Exception
    {
        _verifyTooDeep(_nestedArrays(EXPLOIT_NESTING), JsonNode.class);
        _verifyTooDeep(_nestedObjects(EXPLOIT_NESTING), JsonNode.class);
        _verifyTooDeepTree(_nestedArrays(EXPLOIT_NESTING));
        _verifyTooDeepTree(_nestedObjects(EXPLOIT_NESTING));
    }

    public void testNotTooDeepTreeArray() throws Exception
    {
        JsonNode node = mapper.readTree(_nestedArrays(NOT_TOO_DEEP));
        assertTrue(node.isArray());
        int depth = 0;
        while (node != null && node.isArray()) {
            ++depth;
            node = node.get(0);
        }
        assertEquals(NOT_TOO_DEEP, depth);
    }

    public void testNotTooDeepTreeObject() throws Exception
    {
        JsonNode node = mapper.readValue(_nestedObjects(NOT_TOO_DEEP), JsonNode.class);
        assertTrue(node.isObject());
        int depth = 0;
        while (node != null && node.isObject()) {
            ++depth;
            node = node.get("x");
        }
        assertEquals(NOT_TOO_DEEP + 1, depth);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyTooDeep(String doc, Class<?> type) throws Exception
    {
        _verifyTooDeep(mapper, doc, type);
    }

    private void _verifyTooDeep(ObjectMapper m, String doc, Class<?> type) throws Exception
    {
        try {
            m.readValue(doc, type);
            fail("Should have thrown an exception for too deeply nested "+type.getName());
        } catch (JsonParseException jpe) {
            assertTrue("Unexpected message: "+jpe.getMessage(),
                    jpe.getMessage().startsWith(TOO_DEEP_MSG));
        }
    }

    private void _verifyTooDeepTree(String doc) throws Exception
    {
        try {
            mapper.readTree(doc);
            fail("Should have thrown an exception for too deeply nested tree");
        } catch (JsonParseException jpe) {
            assertTrue("Unexpected message: "+jpe.getMessage(),
                    jpe.getMessage().startsWith(TOO_DEEP_MSG));
        }
    }

    private int _listDepth(Object ob)
    {
        int depth = 0;
        while (ob instanceof List<?>) {
            ++depth;
            List<?> list = (List<?>) ob;
            ob = list.isEmpty() ? null : list.get(0);
        }
        return depth;
    }

    private int _mapDepth(Object ob)
    {
        int depth = 0;
        while (ob instanceof Map<?,?>) {
            ++depth;
            ob = ((Map<?,?>) ob).get("x");
        }
        return depth;
    }

    private String _nestedArrays(int nesting)
    {
        return _nestedDoc(nesting, "[ ", "] ");
    }

    private String _nestedObjects(int nesting)
    {
        return "{" + _nestedDoc(nesting, "\"x\":{", "} ") + "}";
    }

    private String _nestedDoc(int nesting, String open, String close)
    {
        StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
        for (int i = 0; i < nesting; ++i) {
            sb.append(open);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        for (int i = 0; i < nesting; ++i) {
            sb.append(close);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
